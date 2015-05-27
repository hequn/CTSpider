package com.ctbri.spider.entry;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Spider.Status;
import us.codecraft.webmagic.processor.PageProcessor;

import com.alibaba.fastjson.JSON;
import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.pipeline.GeneratePipeline;
import com.ctbri.spider.puter.BadPageReloader;
import com.ctbri.spider.puter.BadPageReloaderController;
import com.ctbri.spider.puter.PageBatchWriter;
import com.ctbri.spider.puter.PageBatchWriterController;
import com.ctbri.spider.scheduler.RedisCachedScheduler;
import com.ctbri.spider.tracker.QueueUrlsTracker;
import com.ctbri.spider.tracker.UpdateInfoTracker;
import com.ctbri.spider.utils.CommonTools;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>The main entry of the crawler. {@link SystemConstants} must be settled before the running,
 * the redis connection, CTSpider-Server connection e.g. Especially the processor which will be used,
 * please ensure the properties file is configured exactly right.
 * </p>
 * 
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 上午10:05:52
 */
public class MainEntry {
	
	private static Logger logger = Logger.getLogger(MainEntry.class);

    public static void main(String[] args) {
    	//Jedis configurations, must be set before new the JedisPool
    	JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(Integer.valueOf(SystemConstants.propertiesConnection.getProperty(SystemConstants.REDIS_MAX_ACTIVE)));
		jedisPoolConfig.setMaxIdle(Integer.valueOf(SystemConstants.propertiesConnection.getProperty(SystemConstants.REDIS_MAX_IDLE)));
		jedisPoolConfig.setMaxWaitMillis(Integer.valueOf(SystemConstants.propertiesConnection.getProperty(SystemConstants.REDIS_MAX_WAIT)));
    	//new the jedis pool using jedisPoolConfig and cache it
		CacheHandler.jedisPool = new JedisPool(
    			jedisPoolConfig,
    			SystemConstants.propertiesConnection.getProperty(SystemConstants.REDIS_IP), 
    			Integer.valueOf(SystemConstants.propertiesConnection.getProperty(SystemConstants.REDIS_PORT)),
    			Integer.valueOf(SystemConstants.propertiesConnection.getProperty(SystemConstants.REDIS_TIME_OUT))
    	);
    	    	
		//get the processor
    	String processorName  = SystemConstants.propertiesConnection.getProperty(SystemConstants.PAGE_PROCESSOR);
    	
    	Object targetPageP = null;
		try {
			targetPageP = Class.forName(processorName).newInstance();
		} catch (Exception e) {
			logger.error("Reflection parsing error, not the right Object",e);
			return;
		}

		setSystemConfigurationAndStart(targetPageP);
		
		UpdateInfoTracker.startTracker(targetPageP);
		
		QueueUrlsTracker.startTracker();
    }
    /**
     * <p>
     * The logical codes help configure and start all of the spider related threads and parameters.
     * The method will be executed in two condition:
     * <ol>
     * 	<li>Execute the jar file and get into the main methods</li>
     * 	<li>The configuration properties file is updated by the server, client checks it and kill the
     * previous spider related threads, then run the method to rerun the spider e.g.
     * </li>
     * </ol>
     * </p>
     * @param targetPageP the processor used
     */
    public static void setSystemConfigurationAndStart(Object targetPageP){
		Jedis jedis = CommonTools.getJedisResource();
		try {
			String proJsonValue = null;
			//check the server first and get the properties of the specific domain for targetPageP
			while(proJsonValue==null || CacheHandler.confVersion == null){
				//get the pro if it is null sleep and retry
				proJsonValue= jedis.hget(
						SystemConstants.PARAM_PREFIX + 
						SystemConstants.propertiesConnection.getProperty(SystemConstants.DOMAIN_KEY),
						SystemConstants.CONFIGURATION_KEY
				);
				//update the version of this configuration
				CacheHandler.confVersion = jedis.hget(
						SystemConstants.PARAM_PREFIX + 
						SystemConstants.propertiesConnection.getProperty(SystemConstants.DOMAIN_KEY),
						SystemConstants.PARAM_UPDATE_KEY
				);
				Thread.sleep(1000*5);
			}
			//parse the json string to object again
			SystemConstants.propertiesControl = JSON.parseObject(proJsonValue, Properties.class);
		} catch(Exception e){
			logger.error("Error happens when connecting to the redis to get the configuration info",e);
		}finally {
			//must be returned to the pool or it will be stuck up once the pool is exhausted
		  CacheHandler.jedisPool.returnResourceObject(jedis);
		}
		logger.debug("Get the configuration info from the redis successfully. Start the spider next");
		//if a spider already exists, which means the update configuration event happened, so kill it 
		//together the related BadPageReloaderController and PageBatchWriterController will stop,too
		if(CacheHandler.spider!=null&&CacheHandler.spider.getStatus()==Status.Running) {
			logger.debug("A previous spider is running with the previous configuration, so stop it");
			CacheHandler.spider.stop();
		}
		
		//parse targetPageP to the right object
    	PageProcessor pageProcessor = (PageProcessor)targetPageP;
    	SpiderConfig spiderConfig = (SpiderConfig) targetPageP;
		
    	//create a new spider using the targetPageP
    	Spider spider = Spider.create(pageProcessor)
        //set the Scheduler to spider
        .setScheduler(
        	//the RedisCachedScheduler is used
        	new RedisCachedScheduler(
        			CacheHandler.jedisPool
        	)
        )
        //the downloader to be set
        .setDownloader(spiderConfig.getDownloader())
        //set Pipeline, should be GeneratePipeline, just a map translator
        .addPipeline(new GeneratePipeline())
        //set the threads number by the propertiesControl configuration
        .thread(Integer.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.TH_COUNT)));
    	//set the sleeping time while the spider get a null url from the scheduler
    	spider.setEmptySleepTime(Integer.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.WAIT_EMPTY)));
        //set exit when get no urls from the redis
    	spider.setExitWhenComplete(false);
    	//the spider itself should be used by the processor too
    	spiderConfig.setSpider(spider);
    	//cache the spider first or again(new one)
    	CacheHandler.spider = spider;
    	//the crawling rate between two urls , this parameter is very important for effecting the spider efficient
    	pageProcessor.getSite().setSleepTime(Integer.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.CRAWL_RATE)));
    	//set the right domain key to the processor, all of the controlling use the domain string
    	pageProcessor.getSite().setDomain(SystemConstants.propertiesConnection.getProperty(SystemConstants.DOMAIN_KEY));
    	//using proxy or not
    	if(Boolean.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.PROXY_CRAWL))){
    		pageProcessor.getSite().setProxyReuseInterval(1000*5);
    		String[] proxies = SystemConstants.propertiesControl.getProperty(SystemConstants.PROXY_QUEUE).split(";");
    		List<String[]> pList = new ArrayList<>();
    		for (String proxy : proxies) {
    			if(proxy!=null && !proxy.trim().equals(""))
    				pList.add(proxy.split(":"));
			}
    		pageProcessor.getSite().setHttpProxyPool(pList);
    	}
    	//reload the bad page or not 
        if (Boolean.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.RELOAD_SHORTAGE_PAGE))) {
			BadPageReloaderController sil = new BadPageReloaderController(
				1000 * 3600 * Integer.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.RELOAD_WAIT)),
				spider,
				(BadPageReloader)targetPageP
			);
			CommonTools.startNewThread(sil, Thread.NORM_PRIORITY);
		}
        //new the batch writer controller
        PageBatchWriterController fis = new PageBatchWriterController(
        		Integer.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.RESULT_WAIT_CHECK)),
        		Integer.valueOf(SystemConstants.propertiesControl.getProperty(SystemConstants.RESULT_MAX_QUEUE)),
        		spider,
        		(PageBatchWriter)targetPageP
        );
        //start the threads that necessary
        CommonTools.startNewThread(fis, Thread.NORM_PRIORITY);
        CommonTools.startNewThread(spider, Thread.NORM_PRIORITY);
    }
}
