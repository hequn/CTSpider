package com.ctbri.spider.processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;

import com.ctbri.spider.downloader.WebAjaxDownloader;
import com.ctbri.spider.utils.FileCarHomePipeline;

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
public class CarHomePageToFileProcessor implements PageProcessor {

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3).setSleepTime(200)
    		.setCycleRetryTimes(3);
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
		try {
			//String testA = page.getHtml().get();
			//String testB = testA.replaceAll("[\r\n]", "");
			//System.out.println(testB);
			String nowUrl = page.getUrl().get();
			if(nowUrl.matches("http://www.autohome.com.cn/grade/carhtml/[a-z].html")){
				addAndCrawlingUrlsTop(page);
			}else if(nowUrl.matches("http://www.autohome.com.cn/[0-9]+/#.*")){
				page.putField("content", page.getHtml().get());
			}else if(nowUrl.matches("http://www.autohome.com.cn/spec/[0-9]+/#.*")) {
				page.putField("content", page.getHtml().get());
			}else if(nowUrl.matches("http://www.autohome.com.cn/spec/[0-9]+/config[.]html#.*")) {
				page.putField("content", page.getHtml().get());
			}else if(nowUrl.matches("http://www.autohome.com.cn/[0-9]+/price.html#.*")){
				addAndCrawlingUrlsLeaf(page);
			} 
			
		} catch (Exception e) {
			page.setSkip(true);
			logger.info("Not a leaf or empyt page! "+e.getMessage());
			if(e.getMessage().contains("Index")) page.addTargetRequest(page.getUrl().get());
		}
    }
    
    private void addAndCrawlingUrlsTop(Page page) {
    	// 部分三：从页面发现后续的url地址来抓取
    	page.addTargetRequests(page.getHtml().links().regex("(http://www.autohome.com.cn/[0-9]+/.*)").all());
    	page.addTargetRequests(page.getHtml().links().regex("(http://www.autohome.com.cn/spec/[0-9]+/.*)").all());
	}
    
    private void addAndCrawlingUrlsLeaf(Page page) {
    	// 部分三：从页面发现后续的url地址来抓取
    	page.addTargetRequests(page.getHtml().links().regex("(http://www.autohome.com.cn/spec/[0-9]+/#pvareaid=[0-9]+)").all());
    	page.addTargetRequests(page.getHtml().links().regex("(http://www.autohome.com.cn/spec/[0-9]+/config.html#pvareaid=[0-9]+)").all());   	
    }

	@Override
    public Site getSite() {
        return site;
    }
    
    public static void main(String[] args) {
    	Spider a = Spider.create(new CarHomePageToFileProcessor())
        //从https://github.com/code4craft开始抓    
        //.addUrl("http://www.autohome.com.cn/spec/24829/config.html#pvareaid=2023154")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/a.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/b.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/c.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/d.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/e.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/f.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/g.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/h.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/i.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/j.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/k.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/l.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/m.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/n.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/o.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/p.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/q.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/r.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/s.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/t.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/u.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/v.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/w.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/x.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/y.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/z.html")
        //去重队列
        .scheduler(new QueueScheduler())
        //尝试使用Phantomjs
        .setDownloader(new WebAjaxDownloader())
        //设置Pipeline，将结果以json方式保存到文件
        .addPipeline(new FileCarHomePipeline("f:/carhome"))//   /ctbri/webspider_carhome/data
        //开启5个线程同时执行
        .thread(4);

        //启动爬虫
        a.run();
    }
}