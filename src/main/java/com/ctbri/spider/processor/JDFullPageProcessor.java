package com.ctbri.spider.processor;

import java.io.File;
import java.io.PrintWriter;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.jsoup.nodes.Element;
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
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.entry.SpiderConfig;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.model.EntityContainer;
import com.ctbri.spider.puter.BadPageReloader;
import com.ctbri.spider.puter.PageBatchWriter;
import com.ctbri.spider.utils.FileLoadTools;

public class JDFullPageProcessor implements PageProcessor,SpiderConfig,PageBatchWriter,BadPageReloader {
	
	//定义logger，用于记录日志
	private Logger logger = LoggerFactory.getLogger(getClass());
	//定义spider，null即可
	private Spider spider = null;
	//定义downloader，包括spider拿来定义在此的原因是暴露给process方法使用PageProcessor接口就是为此而设计
	private Downloader downloader = new OriginalDownloader();

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等，header很重要，不同的业务逻辑需要的也不同，差异性极大
    private Site site = Site.me()
    		.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.101 Safari/537.36")
    		.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    		.addHeader("Accept-Encoding", "gzip, deflate, sdch")
    		.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    		.addHeader("Cache-Control", "no-cache")
    		.addHeader("Connection", "keep-alive");
    //JD相关耦合性配置
    private String jdPrice = "http://p.3.cn/prices/get?skuid=J_";
       
    
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
			
			page.putField("url", Base64.encodeBase64String(page.getUrl().get().getBytes()));
			
			String pUrl = page.getUrl().get();
			int ind = pUrl.indexOf(".html");
			String productId = pUrl.substring(19,ind);
			Page pricePage = downloader.download(new Request(jdPrice+productId), spider);
			//获得价格的url对于json，对于此页是事务性的事件，联动性
			if(pricePage.isNeedCycleRetry()){
				int retried;
				for (retried = 0; retried < 5; retried++) {
					pricePage = downloader.download(new Request(jdPrice+productId), spider);
					if(!pricePage.isNeedCycleRetry()) break;
					Thread.sleep(500);//暂停0.5秒
				}
				if(retried==5) pricePage = null;
			}
			if (pricePage!=null) {
				String productPrice = "-1";
				try {
					JSONArray ja = JSONArray.parseArray(pricePage.getRawText());
					productPrice = (String) ja.getJSONObject(0).get("p");
				} catch (Exception e) {
					logger.error("Not a right formatted json "+e.getMessage());
				}	
				Element jdPrice = page.getHtml().getDocument().getElementById("jd-price");
				if(jdPrice!=null) page.getHtml().getDocument().getElementById("jd-price").text(productPrice);
				else{
					jdPrice = page.getHtml().getDocument().getElementById("mini-jd-price");
					if(jdPrice!=null) page.getHtml().getDocument().getElementById("mini-jd-price").text(productPrice);
				}
				
			}else{
				Element jdPrice = page.getHtml().getDocument().getElementById("jd-price");
				if(jdPrice!=null) page.getHtml().getDocument().getElementById("jd-price").text("-1");
				else{
					jdPrice = page.getHtml().getDocument().getElementById("mini-jd-price");
					if(jdPrice!=null) page.getHtml().getDocument().getElementById("mini-jd-price").text("-1");
				}
				logger.error("No price info for jd price url, tried 10 times but not get price : " + pUrl);
			}
			
			String result = page.getHtml().get().replaceAll("[\r\n]", "");
			page.putField("content",result);

		} catch (Exception e) {
			if(page==null) logger.error("Retry too many times for a url, the info is recored in the log.");
			else logger.error("Part or Null info page : "+page.getUrl().get()+" Info: "+e.getMessage());
		}
    }
	
	@Override
	public void initBatchWritePos(String saveLocation) throws Exception{
		File file = new File(saveLocation);
		if(!file.exists()) file.mkdirs();
		File fileFull = new File(saveLocation+"/jdItems");
		if(!fileFull.exists()) fileFull.mkdir();
	}
	
	@Override
	public EntityContainer pageBatchWrite(int maxPageCount,String saveLocation) throws Exception{
		
		for(int i = 0; i< maxPageCount ; i++){
			Entity e = CacheHandler.resultLines.poll();
			if(e == null) break;
			
			String url = (String) e.getItemParams().get("url");
			String content = (String) e.getItemParams().get("content");
			
			PrintWriter printWriter = new PrintWriter(new File(saveLocation+"/jdItems/"+url),"utf-8");
			printWriter.print(content);
			printWriter.flush();
			printWriter.close();
		}
		
		return null;
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