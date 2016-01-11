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
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.processor.PageProcessor;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.downloader.WebAjaxDownloader;
import com.ctbri.spider.entry.SpiderConfig;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.model.EntityContainer;
import com.ctbri.spider.puter.BadPageReloader;
import com.ctbri.spider.puter.PageBatchWriter;
import com.ctbri.spider.utils.FileLoadTools;

public class TMPageProcessor implements PageProcessor,SpiderConfig,PageBatchWriter,BadPageReloader {
	
	//定义logger，用于记录日志
	private Logger logger = LoggerFactory.getLogger(getClass());
	//定义spider，null即可
	private Spider spider = null;
	//定义downloader，包括spider拿来定义在此的原因是暴露给process方法使用PageProcessor接口就是为此而设计
	private Downloader downloader = new WebAjaxDownloader();

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等，header很重要，不同的业务逻辑需要的也不同，差异性极大
    private Site site = Site.me()
    		.setRetryTimes(6)
    		.setSleepTime(200)
    		.setCycleRetryTimes(6);    
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑，JD的专属process逻辑
    /**
     * 需要强调的是当爬虫被封锁时，有几种方式。
     * <p>1、异常情况。异常情况的url在此架构下会回到redis中，放置于末尾处理。</p>
     * <p>2、页面需要输入验证码或者其他返回页面。需要特异性识别页面并使各个线程休眠适当时间
     * ,即在判断页面是封锁页面时使用
     * 			spider.addUrl(page.getUrl().get());//返回当前问题url到redis供其他客户端爬取
     * 			Thread.sleep(时间);//同时睡眠线程
     * 处理此客户端。</p>
     * 
     * process the page, extract urls to fetch, extract the data and store
     *
     * @param page
     */
    @Override
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
		try {
			String pUrl = page.getUrl().get();
			String productId = pUrl.substring(36);
			page.putField("productKey", productId);
			String productName = page.getHtml().xpath("//li[@id='J_attrBrandName']/text()").get();
			if(productName == null){
				productName = page.getHtml().xpath("//ul[@id='J_AttrUL']/li/text()").get();
			}
			page.putField("productName", productName);
//			最早使用PhantomJS，无界面浏览器加载，最终获得价格，但是效率很低
			String productPrice = page.getHtml().xpath("//dl[@id='J_PromoPrice']//span[@class='tm-price']/text()").get();
			page.putField("productPrice", productPrice);
			if(productPrice == null)logger.debug("No price info : "+pUrl);
			if(productName == null || "".equals(productName)) logger.info("No brand info : "+pUrl);
		} catch (Exception e) {
			logger.error("Part or Null info page : "+page.getUrl().get()+" Info: "+e.getMessage());
		}
    }
	
	@Override
	public void initBatchWritePos(String saveLocation) throws Exception{
		File file = new File(saveLocation);
		if(!file.exists()) file.mkdir();
		File fileFull = new File(saveLocation+"/FullParams");
		if(!fileFull.exists()) fileFull.mkdir();
		File fileSOB = new File(saveLocation+"/ShortOfBrand");
		if(!fileSOB.exists()) fileSOB.mkdir();
		File fileSOP = new File(saveLocation+"/ShortOfPrice");
		if(!fileSOP.exists()) fileSOP.mkdir();
		File fileSOA = new File(saveLocation+"/ShortOfAll");
		if(!fileSOA.exists()) fileSOA.mkdir();	
	}
	
	@Override
	public EntityContainer pageBatchWrite(int maxPageCount,String saveLocation) throws Exception{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String dateTime = sdf.format(new Date());
		String keyID = UUID.randomUUID().toString();
		String tmpFull = saveLocation+"/FullParams/"+keyID;
		String tmpShortOfBrand = saveLocation+"/ShortOfBrand/"+keyID;
		String tmpShortOfPrice = saveLocation+"/ShortOfPrice/"+keyID;
		String tmpShortOfAll = saveLocation+"/ShortOfAll/"+keyID;
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
			if(pName==null&&pPrice==null) printWriter4.println("1701"+key+"\t"+pName+"\t"+"-1"+"\t"+dateTime);
			else if(pName!=null&&pPrice==null) printWriter3.println("1701"+key+"\t"+pName+"\t"+"-1"+"\t"+dateTime);
			else if(pName==null&&pPrice!=null) printWriter2.println("1701"+key+"\t"+pName+"\t"+pPrice+"\t"+dateTime);
			else printWriter1.println("1701"+key+"\t"+pName+"\t"+pPrice+"\t"+dateTime);
			if(pPrice == null) e.getItemParams().put("productPrice","-1");
			e.getItemParams().put("productString", "1701");
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
	public File initReloadBackPos(String saveLocation) throws Exception {
		File fileR = new File(saveLocation+"/RetriedItems");
		if(!fileR.exists()) fileR.mkdir();
		return fileR;
	}
	
	@Override
	public File[] getReloadFiles(String saveLocation) throws Exception{
		File[] filesSPrice = FileLoadTools.getFilesByDirectory(saveLocation+"/ShortOfPrice");
		File[] filesSAll = FileLoadTools.getFilesByDirectory(saveLocation+"/ShortOfAll");
		File[] needRecrawl = ArrayUtils.addAll(filesSPrice, filesSAll);
		return needRecrawl;
	}

	@Override
	public String pageReloadUrl(String oneLine) throws Exception{
		String[] items = oneLine.split(" ");
		return "http://detail.tmall.com/item.htm?id="+items[0];
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