package com.ctbri.spider.processor;

import javax.management.JMException;

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
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.pipeline.JDPipeline;
import com.ctbri.spider.puter.FileItemsLoader;
import com.ctbri.spider.puter.FileItemsSaver;
import com.ctbri.spider.puter.ShortageItemsLoader;
import com.ctbri.spider.scheduler.RedisDuplicateScheduler;
import com.ctbri.spider.utils.ThreadTool;

public class JDPageProcessor implements PageProcessor {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	//定义spider
	private static Spider spider = null;
	//定义downloader，包括spider拿来定义在此的原因是暴露给process方法使用，注意确保使用的唯一性
	private static Downloader downloader = new OriginalDownloader();

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3)
    		.setSleepTime(200).setCycleRetryTimes(3)
    		.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.101 Safari/537.36");
    
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
				page.putField("productId", productId);
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
    public Site getSite() {
        return site;
    }
    
    public static void main(String[] args) {
    	//Jedis的配置信息
    	JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
    	jedisPoolConfig.setMaxActive(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_MAX_ACTIVE)));
    	jedisPoolConfig.setMaxIdle(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_MAX_IDLE)));
    	jedisPoolConfig.setMaxWait(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_MAX_WAIT)));
    		
    	spider = Spider.create(new JDPageProcessor())
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
        .addPipeline(new JDPipeline(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)))
        //开启5个线程同时执行
        .thread(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.TH_COUNT)));
    	//设置读取不到URL时的线程睡眠时间
    	spider.setEmptySleepTime(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.WAIT_EMPTY)));
        //设置无URL且无线程运行时，是否退出
    	spider.setExitWhenComplete(!Boolean.valueOf(SystemConstants.properties.getProperty(SystemConstants.NEVER_STOP)));
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
        	//重爬不成功items
        	ShortageItemsLoader sil = new ShortageItemsLoader(
            		1000*3600*Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.RELOAD_WAIT)),
            		spider
        	);
        	ThreadTool.startNewThread(sil, Thread.NORM_PRIORITY);
        } else {
        	ThreadTool.startNewThread(spider, Thread.NORM_PRIORITY);
        };
        //结果存储到文件，多线程，pipline向queue中存入数据，batch的作用
        FileItemsSaver fis = new FileItemsSaver(
        		Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.RESULT_WAIT_CHECK)),
        		Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.RESULT_MAX_QUEUE)),
        		spider        		
        );
        ThreadTool.startNewThread(fis, Thread.NORM_PRIORITY);
    }
}