package com.ctbri.spider.processor;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.entry.SpiderConfig;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.model.EntityContainer;
import com.ctbri.spider.puter.BadPageReloader;
import com.ctbri.spider.puter.PageBatchWriter;
import com.ctbri.spider.utils.FileLoadTools;

public class LinkedHomePageProcessor implements PageProcessor,SpiderConfig,PageBatchWriter,BadPageReloader {
	
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
    		.setTimeOut(15000)
    		.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.101 Safari/537.36")
    		.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    		.addHeader("Accept-Encoding", "gzip, deflate, sdch")
    		.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    		.addHeader("Cache-Control", "no-cache")
    		.addHeader("Connection", "keep-alive");       
    
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
        //page.putField("author", page.getUrl().regex("https://github\\.com/(\\w+)/.*").toString());
		try {
			page.putField("url", page.getUrl().get());
			
			String combinedHeader = "";
			List<Selectable> headerInfo = page.getHtml().xpath("//div[@class='fl l-txt']/a/text()").nodes();
			for (Iterator<Selectable> iterator = headerInfo.iterator(); iterator.hasNext();) {
				Selectable selectable = (Selectable) iterator.next();
				combinedHeader+=","+selectable.get();
			}
			combinedHeader = combinedHeader.substring(1);
			
			String nameInfo = null;
			nameInfo = page.getHtml().$("h1.title-box.left").xpath("//*/text()").get();
			
			String exInfo = "";
			List<Selectable> exInfos = page.getHtml().xpath("//div[@mod-id='lj-er-detail-title-line']/div[@class='view-label']/span/span/text()").nodes();
			for (Iterator<Selectable> iterator = exInfos.iterator(); iterator.hasNext();) {
				Selectable selectable = (Selectable) iterator.next();
				exInfo+=","+selectable.get();
			}
			exInfo = exInfo.substring(1);
			
			Selectable contentW = page.getHtml().xpath("//section[@mod-id='lj-er-detail-houseinfo']");
			String imgInfo = null;
			imgInfo = contentW.xpath("//div[@id='album-box']/div[@class='album-panel']/div[@class='album-view-wrap']/ul/li/img/@data-url").get();
			
			Selectable priceSpan = contentW.xpath("//div[@class='info-box left']/div/dl[1]/dd/span");
			String price = priceSpan.xpath("//strong/text()").get();
			String area = priceSpan.xpath("//i/text()").get().substring(1);
			
			String perPrice = contentW.xpath("//div[@class='info-box left']/div/dl[2]/dd/text()").get();
			String firstPay = contentW.xpath("//div[@class='info-box left']/div/dl[3]/dd/text()").get();
			String monthPay = contentW.xpath("//div[@class='info-box left']/div/dl[4]/dd/text()").get();
			String houseFormat = contentW.xpath("//div[@class='info-box left']/div/dl[5]/dd/text()").get();
			String direction = contentW.xpath("//div[@class='info-box left']/div/dl[6]/dd/text()").get();
			String totalLayout = contentW.xpath("//div[@class='info-box left']/div/dl[7]/dd/text()").get();
			String department = contentW.xpath("//div[@class='info-box left']/div/dl[8]/dd/a/text()").get();
			String year = contentW.xpath("//div[@class='info-box left']/div/dl[8]/dd/text()").get();
			
			page.putField("headerInfo", combinedHeader);
			page.putField("shortCutInfo", nameInfo);
			page.putField("exInfo", exInfo);
			page.putField("imgInfo", imgInfo);
			page.putField("priceTotaL", price);
			page.putField("areaSize", area);
			page.putField("perPrice", perPrice);
			page.putField("firstPay", firstPay);
			page.putField("monthPay", monthPay);
			page.putField("houseFormat", houseFormat);
			page.putField("direction", direction);
			page.putField("totalLayout", totalLayout);
			page.putField("department", department);
			page.putField("year", year);
			
		} catch (Exception e) {
			//page.setSkip(true);
			logger.info("Exception Happened!"+e.getMessage());
		}
    }
	
	@Override
	public void initBatchWritePos(String saveLocation) throws Exception{
		File file = new File(saveLocation);
		if(!file.exists()) file.mkdirs();
		File fileFull = new File(saveLocation+"/linkedHome");
		if(!fileFull.exists()) fileFull.mkdir();
	}
	
	@Override
	public EntityContainer pageBatchWrite(int maxPageCount,String saveLocation) throws Exception{
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String dateTime = sdf.format(new Date());
		String keyID = UUID.randomUUID().toString();
		String tmpFull = saveLocation+"/linkedHome/"+keyID;		
		PrintWriter printWriter1 = new PrintWriter(new File(tmpFull),"utf-8");
		EntityContainer ec = new EntityContainer();
		ec.setDomainKey(site.getDomain());		
		ec.getParamsOrder().add("url");
		ec.getParamsOrder().add("headerInfo");
		ec.getParamsOrder().add("shortCutInfo");
		ec.getParamsOrder().add("exInfo");
		ec.getParamsOrder().add("imgInfo");
		ec.getParamsOrder().add("priceTotaL");
		ec.getParamsOrder().add("areaSize");
		ec.getParamsOrder().add("perPrice");
		ec.getParamsOrder().add("firstPay");
		ec.getParamsOrder().add("monthPay");
		ec.getParamsOrder().add("houseFormat");
		ec.getParamsOrder().add("direction");
		ec.getParamsOrder().add("totalLayout");
		ec.getParamsOrder().add("department");
		ec.getParamsOrder().add("year");
		ec.getParamsOrder().add("dateTime");
		for(int i = 0; i< maxPageCount ; i++){
			Entity e = CacheHandler.resultLines.poll();
			if(e == null) break;
			
			String url = (String) e.getItemParams().get("url");
			String headerInfo = (String) e.getItemParams().get("headerInfo");
			String shortCutInfo = (String) e.getItemParams().get("shortCutInfo");
			String exInfo = (String) e.getItemParams().get("exInfo");
			String imgInfo = (String) e.getItemParams().get("imgInfo");
			String priceTotaL = (String) e.getItemParams().get("priceTotaL");
			String areaSize = (String) e.getItemParams().get("areaSize");
			String perPrice = (String) e.getItemParams().get("perPrice");
			String firstPay = (String) e.getItemParams().get("firstPay");
			String monthPay = (String) e.getItemParams().get("monthPay");
			String houseFormat = (String) e.getItemParams().get("houseFormat");
			String direction = (String) e.getItemParams().get("direction");
			String totalLayout = (String) e.getItemParams().get("totalLayout");
			String department = (String) e.getItemParams().get("department");
			String year = (String) e.getItemParams().get("year");
			
			printWriter1.println(url+"\t"+headerInfo+"\t"+shortCutInfo+"\t"+exInfo+"\t"
					+imgInfo+"\t"+priceTotaL+"\t"+areaSize+"\t"+perPrice+"\t"+firstPay+"\t"
					+monthPay+"\t"+houseFormat+"\t"+direction+"\t"+totalLayout+"\t"
					+department+"\t"+year+"\t"+dateTime);
			
			e.getItemParams().put("dateTime", dateTime);
			ec.getEntities().add(e);
		}
		printWriter1.close();
		
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
		return null;
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