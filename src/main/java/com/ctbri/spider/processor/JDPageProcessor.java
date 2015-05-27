package com.ctbri.spider.processor;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.processor.PageProcessor;

import com.alibaba.fastjson.JSONArray;
import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.entry.SpiderConfig;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.model.EntityContainer;
import com.ctbri.spider.puter.BadPageReloader;
import com.ctbri.spider.puter.PageBatchWriter;
import com.ctbri.spider.utils.FileLoadTools;

public class JDPageProcessor implements PageProcessor,SpiderConfig,PageBatchWriter,BadPageReloader {
	
	//定义logger，用于记录日志
	private Logger logger = LoggerFactory.getLogger(getClass());
	//定义spider，null即可
	private Spider spider = null;
	//定义downloader，包括spider拿来定义在此的原因是暴露给process方法使用PageProcessor接口就是为此而设计
	private Downloader downloader = new OriginalDownloader();

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等，header很重要，不同的业务逻辑需要的也不同，差异性极大
    private Site site = Site.me()
    		.setRetryTimes(6)
    		.setSleepTime(200)
    		.setCycleRetryTimes(6)
    		.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.101 Safari/537.36")
    		.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    		.addHeader("Accept-Encoding", "gzip, deflate, sdch")
    		.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    		.addHeader("Cache-Control", "no-cache")
    		.addHeader("Connection", "keep-alive");
    //JD相关耦合性配置
    private String jdPrice = "http://p.3.cn/prices/get?skuid=J_";
       
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑，JD的专属process逻辑
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
		try {
			String pUrl = page.getUrl().get();
			int ind = pUrl.indexOf(".html");
			String productId = pUrl.substring(19,ind);
			page.putField("productKey", productId);
			String productName = page.getHtml().xpath("//div[@class='breadcrumb']").$("a[clstag$=mbNav-4]").xpath("/a/text()").get();
			page.putField("productName", productName);
//			最早使用PhantomJS，无界面浏览器加载，最终获得价格，但是效率很低
//			String productPrice = page.getHtml().xpath("//strong[@id='jd-price']/text()").get().substring(1);
//			page.putField("productPrice", productPrice);
			Page pricePage = downloader.download(new Request(jdPrice+productId), spider);
			JSONArray ja = JSONArray.parseArray(pricePage.getRawText());
			String productPrice = (String) ja.getJSONObject(0).get("p");
			double pTemp = Double.valueOf(productPrice);
			if(pTemp > 0) page.putField("productPrice", productPrice);
			else {
				page.putField("productPrice", null);
				logger.debug("No price info : "+pUrl);
			}
			if(productName == null || "".equals(productName)) logger.info("No brand info : "+pUrl);
		} catch (Exception e) {
			logger.error("Part or Null info page : "+page.getUrl().get()+" Info: "+e.getMessage());
		}
    }
	
	@Override
	public void initBatchWritePos() throws Exception{
		File file = new File(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION));
		if(!file.exists()) file.mkdir();
		File fileFull = new File(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/FullParams");
		if(!fileFull.exists()) fileFull.mkdir();
		File fileSOB = new File(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfBrand");
		if(!fileSOB.exists()) fileSOB.mkdir();
		File fileSOP = new File(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice");
		if(!fileSOP.exists()) fileSOP.mkdir();
		File fileSOA = new File(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll");
		if(!fileSOA.exists()) fileSOA.mkdir();	
	}
	
	@Override
	public EntityContainer pageBatchWrite(int maxPageCount) throws Exception{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String dateTime = sdf.format(new Date());
		String keyID = UUID.randomUUID().toString();
		String tmpFull = SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/FullParams/"+keyID;
		String tmpShortOfBrand = SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfBrand/"+keyID;
		String tmpShortOfPrice = SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice/"+keyID;
		String tmpShortOfAll = SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll/"+keyID;
		PrintWriter printWriter1 = new PrintWriter(new File(tmpFull),"gbk");
		PrintWriter printWriter2 = new PrintWriter(new File(tmpShortOfBrand),"gbk");
		PrintWriter printWriter3 = new PrintWriter(new File(tmpShortOfPrice),"gbk");
		PrintWriter printWriter4 = new PrintWriter(new File(tmpShortOfAll),"gbk");
		EntityContainer ec = new EntityContainer();
		ec.setDomainKey(site.getDomain());
		ec.getParamsOrder().add("productString");
		ec.getParamsOrder().add("productKey");
		ec.getParamsOrder().add("productName");
		ec.getParamsOrder().add("productPrice");
		ec.getParamsOrder().add("dateTime");
		for(int i = 0; i< maxPageCount ; i++){
			Entity e = CacheHandler.resultLines.poll();
			if(e == null) break;
			String key = (String) e.getItemParams().get("productKey");
			String pName = (String) e.getItemParams().get("productName");
			String pPrice = (String) e.getItemParams().get("productPrice");
			if(pName==null&&pPrice==null) printWriter4.println("1101"+key+"\t"+pName+"\t"+"-1"+"\t"+dateTime);
			else if(pName!=null&&pPrice==null) printWriter3.println("1101"+key+"\t"+pName+"\t"+"-1"+"\t"+dateTime);
			else if(pName==null&&pPrice!=null) printWriter2.println("1101"+key+"\t"+pName+"\t"+pPrice+"\t"+dateTime);
			else printWriter1.println("1101"+key+"\t"+pName+"\t"+pPrice+"\t"+dateTime);
			if(pPrice == null) e.getItemParams().put("productPrice","-1");
			e.getItemParams().put("productString", "1101");
			e.getItemParams().put("dateTime", dateTime);
			ec.getEntities().add(e);
		}
		printWriter1.close();
		printWriter2.close();
		printWriter3.close();
		printWriter4.close();
		
		return ec;
	}
	
	@Override
	public File initReloadBackPos() throws Exception {
		File fileR = new File(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/RetriedItems");
		if(!fileR.exists()) fileR.mkdir();
		return fileR;
	}
	
	@Override
	public File[] getReloadFiles() throws Exception{
		File[] filesSPrice = FileLoadTools.getFilesByDirectory(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice");
		File[] filesSAll = FileLoadTools.getFilesByDirectory(SystemConstants.propertiesControl.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll");
		File[] needRecrawl = ArrayUtils.addAll(filesSPrice, filesSAll);
		return needRecrawl;
	}

	@Override
	public String pageReloadUrl(String oneLine) throws Exception{
		String[] items = oneLine.split(" ");
		return "http://item.jd.com/"+items[0]+".html";		
	}
	//上面三个重写的方法逻辑和内容均不需要改变
	@Override
    public Site getSite() {
        return site;
    }

	@Override
	public Downloader getDownloader() {
		return downloader;
	}

	@Override
	public void setSpider(Spider spider) {
		this.spider = spider;
	}
}