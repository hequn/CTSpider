package com.ctbri.spider.processor;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.pipeline.FilePipeline;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.QueueScheduler;

import com.ctbri.spider.downloader.OriginalDownloader;
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
public class MixedPageToFileTester implements PageProcessor {

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3).setSleepTime(200)
    		.setCycleRetryTimes(3).setDomain("mixed.com");
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
		try {
			page.putField("url", Base64.encodeBase64String(page.getUrl().get().getBytes()));
			String result = page.getHtml().get().replaceAll("[\r\n]", "");
			page.putField("content",result);
			System.out.println(result.substring(0, 400));
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
    	Spider a = Spider.create(new MixedPageToFileTester())
        //从https://github.com/code4craft开始抓    
        //.addUrl("http://www.autohome.com.cn/spec/24829/config.html#pvareaid=2023154")
    	.addUrl("http://item.jd.com/1558562.html")
        .addUrl("http://cnews.chinadaily.com.cn/2016-01/26/content_23260994.htm")
        //去重队列
        .scheduler(new QueueScheduler())
        //尝试使用Phantomjs
        .setDownloader(new OriginalDownloader())
        //设置Pipeline，将结果以json方式保存到文件
        .addPipeline(new FilePipeline("d:/testdata"))//   /ctbri/webspider_carhome/data
        //开启5个线程同时执行
        .thread(4);

        //启动爬虫
        a.run();
    }
}