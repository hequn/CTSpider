package com.ctbri.spider.processor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.JMException;

import org.apache.commons.codec.binary.Base64;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.Selectable;

import com.alibaba.fastjson.JSONArray;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.downloader.WebAjaxDownloader;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.pipeline.GeneratePipeline;
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

	private Downloader downloader = new OriginalDownloader();
	
	private static Spider spider = null;
    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me()
    		.setRetryTimes(6)
    		.setSleepTime(200)
    		.setCycleRetryTimes(6)
    		.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2526.106 Safari/537.36")
    		.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    		.addHeader("Accept-Encoding", "gzip, deflate, sdch")
    		.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    		.addHeader("Cache-Control", "no-cache")
    		.addHeader("Connection", "keep-alive");
    		//.addHeader("Cookie", "SINAGLOBAL=8926377859897.912.1427704806534; UOR=www.testwo.com,widget.weibo.com,bbs.xmfish.com; YF-Page-G0=8ec35b246bb5b68c13549804abd380dc; login_sid_t=7c368b17e34f9284660a219f6abf2a8b; YF-Ugrow-G0=169004153682ef91866609488943c77f; _s_tentry=passport.weibo.com; Apache=6022827473934.74.1452739743348; ULV=1452739743355:43:2:1:6022827473934.74.1452739743348:1452046739597; SSOLoginState=1452741161; un=hequn0815@163.com; SUS=SID-1793002255-1452741162-JA-n7tj4-45e2d78a16dd45532fc8cef148234a9c; SUE=es%3Dcb666bdde6395e2243d55b96a34b835c%26ev%3Dv1%26es2%3Da7eef37d17e1bba4837dff81363e19ac%26rs0%3DooeK8QAALSAquE0L84apACXUr3l0jl16tB%252BHju9RyoqJ8XR%252Buo8DSEOnrtthOigHPwyLCK9rEQ%252FwjUhQeHr7%252Bf92WUu3XPwPSRiNXwgO%252FCRQpWjQtD0u3wHEh%252BTTN2jabzni%252F0yHXMm%252BneFNu0cvUiDZ348Xtv6nj82m4zLzrjU%253D%26rv%3D0; SUP=cv%3D1%26bt%3D1452741162%26et%3D1452827562%26d%3Dc909%26i%3D4a9c%26us%3D1%26vf%3D0%26vt%3D0%26ac%3D0%26st%3D0%26uid%3D1793002255%26name%3Dhequn0815%2540163.com%26nick%3Dhequn0815%26fmp%3D%26lcp%3D2015-11-30%252013%253A13%253A09; SUB=_2A257k2J6DeRxGedJ4lER8CzOzjmIHXVY6dSyrDV8PUNbu9AMLWTEkW9LHesg7_RhnmC4MuFOZWoaPaK6TIZrqQ..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9W55w.g0Kq4ddGPAsF6Z7l2V5JpX5Kzt; SUHB=0xnzEX7UtmV7Zg; ALF=1484277162; YF-V5-G0=5f9bd778c31f9e6f413e97a1d464047a")
    		//.addHeader("Host","weibo.com")
    		//.addHeader("Pragma","no-cache");
    
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private String jdPrice = "http://p.3.cn/prices/get?skuid=J_";
    
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
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
					JSONArray ja = JSONArray.parseArray("{}");//pricePage.getRawText()
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
    public Site getSite() {
        return site;
    }
    
    public static void main(String[] args) {
    	Spider a = Spider.create(new TestProcessor())
        //从https://github.com/code4craft开始抓    
        .addUrl("http://item.jd.com/1397842808.html")
//        .addUrl("http://item.jd.com/2174965.html")
//        .addUrl("http://item.jd.com/10150740786.html")
//        .addUrl("http://item.jd.com/1792768.html")
//        .addUrl("http://item.jd.com/1079625869.html")
//        .addUrl("http://item.jd.com/10042298299.html")
//        .addUrl("http://item.jd.com/10150393085.html")
//        .addUrl("http://item.jd.com/10126954571.html")
//        .addUrl("http://item.jd.com/10122792507.html")
//        .addUrl("http://item.jd.com/1603352909.html")
        //.addUrl("http://weibo.com/1401789167/follow?relate=fans")
        //设置Scheduler，使用Redis来管理URL队列
        //.setScheduler(new RedisScheduler(new JedisPool(new JedisPoolConfig(),"192.168.56.128", 6379)))
        //尝试使用SeleniumDownloader
        .setDownloader(new OriginalDownloader())//new OriginalDownloader()
        //设置Pipeline，将结果以json方式保存到文件
        .addPipeline(new FilePipeline("D:/testdata"))//new GeneratePipeline()
        .setExitWhenComplete(true)
        //开启5个线程同时执行
        .thread(2);
    	a.setEmptySleepTime(1000);
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
    	TestProcessor.spider = a;
        //启动爬虫
        a.run();
    }
}