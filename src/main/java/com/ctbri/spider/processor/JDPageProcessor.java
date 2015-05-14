package com.ctbri.spider.processor;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.management.JMException;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;

import com.alibaba.fastjson.JSONArray;
import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.pipeline.GeneratePipeline;
import com.ctbri.spider.puter.PageBatchWriter;
import com.ctbri.spider.puter.FileItemsLoader;
import com.ctbri.spider.puter.FileItemsSaver;
import com.ctbri.spider.puter.FailurePageReload;
import com.ctbri.spider.puter.ShortageItemsLoader;
import com.ctbri.spider.scheduler.RedisDuplicateScheduler;
import com.ctbri.spider.utils.FileLoadTools;
import com.ctbri.spider.utils.ThreadTool;

public class JDPageProcessor implements PageProcessor,PageBatchWriter,FailurePageReload {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	//定义spider
	private static Spider spider = null;
	//定义downloader，包括spider拿来定义在此的原因是暴露给process方法使用，注意确保使用的唯一性
	private static Downloader downloader = new OriginalDownloader();

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private static Site site = Site.me().setRetryTimes(6)
    		.setSleepTime(200).setCycleRetryTimes(6)
    		.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.101 Safari/537.36")
    		.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    		.addHeader("Accept-Encoding", "gzip, deflate, sdch")
    		.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    		.addHeader("Cache-Control", "no-cache")
    		.addHeader("Connection", "keep-alive")
    		.addHeader("Referer", "http://www.jd.com");
    //JD相关耦合性配置
    private static String jdPrice = "http://p.3.cn/prices/get?skuid=J_";
    
    public static String domainSeed = "http://item.jd.com/00000000.html";
    
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑，JD的专属process逻辑
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
		try {
			if(page.getUrl().get().equals(domainSeed)){
				page.setSkip(true);
				return;
			}else{
				String pUrl = page.getUrl().get();
				int ind = pUrl.indexOf(".html");
				String productId = pUrl.substring(19,ind);
				page.putField("productKey", productId);
				String productName = page.getHtml().xpath("//div[@class='breadcrumb']").$("a[clstag$=mbNav-4]").xpath("/a/text()").get();
				page.putField("productName", productName);
//				String productPrice = page.getHtml().xpath("//strong[@id='jd-price']/text()").get().substring(1);
//				page.putField("productPrice", productPrice);
				Page pricePage = downloader.download(new Request(jdPrice+productId), spider);
				JSONArray ja = JSONArray.parseArray(pricePage.getRawText());
				String productPrice = (String) ja.getJSONObject(0).get("p");
				double pTemp = Double.valueOf(productPrice);
				if(pTemp > 0) page.putField("productPrice", productPrice);
				else {
					page.putField("productPrice", null);
					logger.info("No price info : "+pUrl);
				}
				if(productName == null || "".equals(productName)) logger.info("No brand info : "+pUrl);
			}
		} catch (Exception e) {
			logger.info("Part or Null info page : "+page.getUrl().get()+" Info: "+e.getMessage());
		}
    }
	
	@Override
	public void initBatchWritePos() throws Exception{
		File file = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION));
		if(!file.exists()) file.mkdir();
		File fileFull = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/FullParams");
		if(!fileFull.exists()) fileFull.mkdir();
		File fileSOB = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfBrand");
		if(!fileSOB.exists()) fileSOB.mkdir();
		File fileSOP = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice");
		if(!fileSOP.exists()) fileSOP.mkdir();
		File fileSOA = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll");
		if(!fileSOA.exists()) fileSOA.mkdir();	
	}
	
	@Override
	public void pageBatchWrite(int maxPageCount) throws Exception{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String dateTime = sdf.format(new Date());
		String keyID = UUID.randomUUID().toString();
		String tmpFull = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/FullParams/"+keyID;
		String tmpShortOfBrand = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfBrand/"+keyID;
		String tmpShortOfPrice = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice/"+keyID;
		String tmpShortOfAll = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll/"+keyID;
		PrintWriter printWriter1 = new PrintWriter(new FileWriter(tmpFull));
		PrintWriter printWriter2 = new PrintWriter(new FileWriter(tmpShortOfBrand));
		PrintWriter printWriter3 = new PrintWriter(new FileWriter(tmpShortOfPrice));
		PrintWriter printWriter4 = new PrintWriter(new FileWriter(tmpShortOfAll));
		for(int i = 0; i< maxPageCount ; i++){
			Entity e = CacheHandler.resultLines.poll();
			if(e == null) break;
			String key = (String) e.getItemParams().get("productKey");
			String pName = (String) e.getItemParams().get("productName");
			String pPrice = (String) e.getItemParams().get("productPrice");
			if(pName==null&&pPrice==null) printWriter4.println(key+" "+pName+" "+pPrice+" "+dateTime);
			else if(pName!=null&&pPrice==null) printWriter3.println(key+" "+pName+" "+pPrice+" "+dateTime);
			else if(pName==null&&pPrice!=null) printWriter2.println(key+" "+pName+" "+pPrice+" "+dateTime);
			else printWriter1.println(key+" "+pName+" "+pPrice+" "+dateTime);
		}
		printWriter1.close();
		printWriter2.close();
		printWriter3.close();
		printWriter4.close();
	}
	
	@Override
	public File initReloadBackPos() throws Exception {
		File fileR = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/RetriedItems");
		if(!fileR.exists()) fileR.mkdir();
		return fileR;
	}
	
	@Override
	public File[] getReloadFiles() throws Exception{
		File[] filesSPrice = FileLoadTools.getFilesByDirectory(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice");
		File[] filesSAll = FileLoadTools.getFilesByDirectory(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll");
		File[] needRecrawl = ArrayUtils.addAll(filesSPrice, filesSAll);
		return needRecrawl;
	}

	@Override
	public String pageReloadUrl(String oneLine) throws Exception{
		String[] items = oneLine.split(" ");
		return "http://item.jd.com/"+items[0]+".html";		
	}
	
	@Override
    public Site getSite() {
        return site;
    }
	
    public static void main(String[] args) {
    	//Jedis的配置信息
    	JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    	jedisPoolConfig.setMaxActive(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_MAX_ACTIVE)));
    	jedisPoolConfig.setMaxIdle(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_MAX_IDLE)));
    	jedisPoolConfig.setMaxWait(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_MAX_WAIT)));
    		
    	JDPageProcessor jdPageProcessor = new JDPageProcessor();
    	spider = Spider.create(jdPageProcessor)
        //用来获取site的domain，没有实际内容意义，但对于分布式来说很重要，唯一的标示
        .addUrl(domainSeed)//"http://item.jd.com/1319192.html"
        //设置Scheduler，使用Redis来管理URL队列
        .setScheduler(
        	new RedisDuplicateScheduler(
        		new JedisPool(
        			jedisPoolConfig,
        			SystemConstants.properties.getProperty(SystemConstants.REDIS_IP), 
        			Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_PORT)),
        			Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_TIME_OUT))
        		)
        	)
        )
        //尝试使用一些Downloader
        .setDownloader(downloader)
        //设置Pipeline，将结果以json方式保存到文件
        .addPipeline(new GeneratePipeline())
        //开启5个线程同时执行
        .thread(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.TH_COUNT)));
    	//设置读取不到URL时的线程睡眠时间
    	spider.setEmptySleepTime(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.WAIT_EMPTY)));
        //设置无URL且无线程运行时，是否退出
    	spider.setExitWhenComplete(!Boolean.valueOf(SystemConstants.properties.getProperty(SystemConstants.NEVER_STOP)));
    	//使用代理的话，按照设置进行
    	if(Boolean.valueOf(SystemConstants.properties.getProperty(SystemConstants.PROXY_CRAWL))){
    		site.setProxyReuseInterval(1000*5);
    		String[] proxies = SystemConstants.properties.getProperty(SystemConstants.PROXY_QUEUE).split(";");
    		List<String[]> pList = new ArrayList<>();
    		for (String proxy : proxies) {
    			if(proxy!=null && !proxy.trim().equals(""))
    				pList.add(proxy.split(":"));
			}
    		site.setHttpProxyPool(pList);
    	}
    	//启动监视器，cmd jconsole.exe查看状态 jmx
    	try {
			SpiderMonitor.instance().register(spider);
		} catch (JMException e) {
			e.printStackTrace();
		}
    	//线程问题，稍后启动爬虫
    	//spider.run();
        //种子用户，提供文件中的url到redis中
        if(Boolean.valueOf(SystemConstants.properties.getProperty(SystemConstants.SEED_CLIENT))){
        	FileItemsLoader fil = new FileItemsLoader(
        		Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.SEED_WAIT_CHECK)),
        		Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.SEED_MAX_QUEUE)),
        		spider
        	);
        	ThreadTool.startNewThread(fil, Thread.MAX_PRIORITY);
        	if (Boolean.valueOf(SystemConstants.properties.getProperty(SystemConstants.RELOAD_SHORTAGE_PAGE))) {
				//重爬不成功items
				ShortageItemsLoader sil = new ShortageItemsLoader(
					1000 * 3600 * Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.RELOAD_WAIT)),
					spider,
					jdPageProcessor
				);
				ThreadTool.startNewThread(sil, Thread.NORM_PRIORITY);
			}
        } else {
        	ThreadTool.startNewThread(spider, Thread.NORM_PRIORITY);
        };
        //结果存储到文件，多线程，pipline向queue中存入数据，batch的作用
        FileItemsSaver fis = new FileItemsSaver(
        		Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.RESULT_WAIT_CHECK)),
        		Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.RESULT_MAX_QUEUE)),
        		spider,
        		jdPageProcessor
        );
        ThreadTool.startNewThread(fis, Thread.NORM_PRIORITY);
    }
}