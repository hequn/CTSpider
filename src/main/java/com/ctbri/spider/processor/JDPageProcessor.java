package com.ctbri.spider.processor;

import javax.management.JMException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;

import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.downloader.WebAjaxDownloader;
import com.ctbri.spider.pipeline.JDPipeline;
import com.ctbri.spider.scheduler.RedisDuplicateScheduler;
import com.ctbri.spider.utils.FileItemsLoader;
import com.ctbri.spider.utils.FileItemsSaver;

public class JDPageProcessor implements PageProcessor {

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3).setSleepTime(300).setCycleRetryTimes(3);
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
		try {
			if(page.getUrl().get().equals(SystemConstants.properties.getProperty(SystemConstants.D_SEED))){
				page.setSkip(true);
				return;
			}else{
				String pUrl = page.getUrl().get();
				int ind = pUrl.indexOf(".html");
				String productId = pUrl.substring(19,ind);
				page.putField("productId", productId);
				String productName = page.getHtml().xpath("//div[@class='breadcrumb']").$("a[clstag$=mbNav-4]").xpath("/a/text()").get();
				page.putField("productName", productName);
				String productPrice = page.getHtml().xpath("//strong[@id='jd-price']/text()").get().substring(1);
				page.putField("productPrice", productPrice);
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
    	Spider a = Spider.create(new JDPageProcessor())
        //用来获取site的domain，没有实际内容意义，但对于分布式来说很重要，唯一的标示
        .addUrl(SystemConstants.properties.getProperty(SystemConstants.D_SEED))
        //设置Scheduler，使用Redis来管理URL队列
        .setScheduler(
        	new RedisDuplicateScheduler(
        		new JedisPool(
        			new JedisPoolConfig(),
        			SystemConstants.properties.getProperty(SystemConstants.REDIS_IP), 
        			Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.REDIS_PORT))
        		)
        	)
        )
        //尝试使用SeleniumDownloader
        .setDownloader(new WebAjaxDownloader())
        //设置Pipeline，将结果以json方式保存到文件
        .addPipeline(new JDPipeline(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)))
        //开启5个线程同时执行
        .thread(Integer.valueOf(SystemConstants.properties.getProperty(SystemConstants.TH_COUNT)));
    	//设置读取不到URL时的等待时间
        a.setEmptySleepTime(1000*300);
        //设置无URL且无线程运行时，是否退出
        a.setExitWhenComplete(!Boolean.valueOf(SystemConstants.properties.getProperty(SystemConstants.NEVER_STOP)));
    	//启动监视器，cmd jconsole.exe查看状态 jmx
    	try {
			SpiderMonitor.instance().register(a);
		} catch (JMException e) {
			e.printStackTrace();
		}
    	//线程问题，稍后启动爬虫
    	//a.run();
        //种子用户，提供文件中的url到redis中
        if(Boolean.valueOf(SystemConstants.properties.getProperty(SystemConstants.SEED_CLIENT))){
        	FileItemsLoader fil = new FileItemsLoader();
        	FileItemsLoader.setSpider(a);
        	Thread tfil = new Thread(fil);
        	tfil.setDaemon(false);
        	tfil.setPriority(Thread.MAX_PRIORITY);
        	tfil.start();
        } else {
			Thread newThread = new Thread(a);
			newThread.setDaemon(false);
			newThread.start();
        };
        //结果存储到文件，多线程，pipline向queue中存入数据，batch的作用
        FileItemsSaver fis = new FileItemsSaver();
        FileItemsSaver.setSpider(a);
        Thread tfis = new Thread(fis);
        tfis.setDaemon(false);
        tfis.start();
    }
}