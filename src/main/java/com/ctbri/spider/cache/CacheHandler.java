package com.ctbri.spider.cache;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

import redis.clients.jedis.JedisPool;
import us.codecraft.webmagic.Spider;

import com.ctbri.spider.model.Entity;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p> 
 *  The important core caches of the spider, they are used to keep the information needed. 
 *</p>
 * 
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 上午11:04:32
 */
public class CacheHandler {
	//the resultLines is the queue holding the results from the page, added by GeneratePipeline
	//and using by PageBatchWriter#pageBatchWrite,PageBatchWriterController
	public static final BlockingQueue<Entity> resultLines = new LinkedBlockingQueue<>();
	//because of using too many redis connections is a waste of time, using cached queue to batch get urls 
	public static final BlockingQueue<String> cachedNoExtraUrls = new LinkedBlockingQueue<>();
	//the timerPool for execute QueueUrlsTracker and UpdateInfoTracker
	public static final ScheduledExecutorService timerPool = Executors.newScheduledThreadPool(2); 
	//the synchronized key for BadPageReloaderController and PageBatchWriterController
	public static final String readWriteLock = new String("readWriteLock");
	//the jedisPool to get redis resources
	public static JedisPool jedisPool = null;
	//cache the spider for global controlling
	public static volatile Spider spider = null;
	//the hungUp signal get from the redis which is set by CTSpider-Server
	public static volatile boolean hungUp = true;
	//check if the server push seeds finished
	public static volatile boolean finishPush = false;
	//check the server last pushed item size
	public static volatile Integer lastTotal = 0;
	//cached configuration version
	public static volatile String confVersion = null;
	//the key only for this jar process
	public static final String processUUID = UUID.randomUUID().toString();
	//the unique signal for this jar process, RUNNING or HUNGUP
	public static volatile String workerSignal = "RUNNING";
}
