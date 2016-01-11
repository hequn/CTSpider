package com.ctbri.spider.processor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.JMException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.downloader.WebAjaxDownloader;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.pipeline.JsonPipeline;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	一款垂直设计的爬虫，预先不知道有多少URL，所用通过获取一个页面中需要新加入的任务的方式持续抓取。
 *	这种方式是一般爬虫的工作方式，汽车之家汽车参数和价格的页面爬虫就是本例。（在当前的架构模式下不可用，参考可用的{@link JDPageProcessor}）。
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:12:43
 */
public class TestProcessor implements PageProcessor {

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3).setSleepTime(200)
    		.setCycleRetryTimes(3);
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
        //page.putField("author", page.getUrl().regex("https://github\\.com/(\\w+)/.*").toString());
		try {
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
			
			page.putField("url", page.getUrl().get());
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
    public Site getSite() {
        return site;
    }
    
    public static void main(String[] args) {
    	Spider a = Spider.create(new TestProcessor())
        //从https://github.com/code4craft开始抓    
        .addUrl("http://sh.lianjia.com/ershoufang/SH0001021023.html")
        //设置Scheduler，使用Redis来管理URL队列
        //.setScheduler(new RedisScheduler(new JedisPool(new JedisPoolConfig(),"192.168.56.128", 6379)))
        //尝试使用SeleniumDownloader
        .setDownloader(new OriginalDownloader())//new OriginalDownloader()
        //设置Pipeline，将结果以json方式保存到文件
        .addPipeline(new JsonPipeline("/ctbri/webspider_carhome/data"))//new GeneratePipeline()
        //开启5个线程同时执行
        .thread(4);
    	//启动监视器，cmd jconsole.exe查看状态 jmx
//    	if(Boolean.valueOf(SystemConstants.propertiesConnection.getProperty(SystemConstants.PROXY_CRAWL))){
//    		a.getSite().setProxyReuseInterval(1000*5);
//    		String[] proxies = SystemConstants.propertiesConnection.getProperty(SystemConstants.PROXY_QUEUE).split(";");
//    		List<String[]> pList = new ArrayList<>();
//    		for (String proxy : proxies) {
//    			if(proxy!=null && !proxy.trim().equals(""))
//    				pList.add(proxy.split(":"));
//			}
//    		a.getSite().setHttpProxyPool(pList);
//    	}
    	try {
			SpiderMonitor.instance().register(a);
		} catch (JMException e) {
			e.printStackTrace();
		}
        //启动爬虫
        a.run();
    }
}