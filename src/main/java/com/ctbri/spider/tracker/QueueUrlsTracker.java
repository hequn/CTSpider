package com.ctbri.spider.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.utils.CommonTools;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The cached urls holder. Get the urls batched from the redis.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午3:05:57
 */
public class QueueUrlsTracker {


	private static int period = 1000*10;//ten seconds
	private static int maxQueueSize = 200;
	private static int batchGetSize = 100;
	private static Logger logger = Logger.getLogger(QueueUrlsTracker.class);
	
	/**
	 * Start the tracker method.
	 */
	public static void startTracker(){

		CacheHandler.timerPoolQU.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {		
				if(CacheHandler.hungUp == true || 
						CacheHandler.workerSignal.equals(SystemConstants.HUNGUP)) return;
				int size = CacheHandler.cachedNoExtraUrls.size();
				if(size > maxQueueSize) return;
	    		Jedis jedis = CommonTools.getJedisResource();
	    		String queueKey = SystemConstants.QUEUE_PREFIX+
	    				SystemConstants.propertiesConnection.getProperty(SystemConstants.DOMAIN_KEY);
	    		try {
	    			if(jedis.llen(queueKey).intValue() == 0) {
	    				return;
	    			}
	    			Pipeline pipeline = jedis.pipelined();
	    			List<Response<String>> resps = new ArrayList<>();
	    			for (int i = 0; i < batchGetSize; i++) {
	    				resps.add(pipeline.lpop(queueKey));
					}
	    			pipeline.sync();
	    			for (Response<String> response : resps) {
	    				if(response!=null&&response.get()!=null)
	    						CacheHandler.cachedNoExtraUrls.offer(response.get());
	    				else {
	    					break;
	    				}
					}
	    			logger.debug("Try QueueUrlsTracker once");
	    		} catch(Exception e){
	    			logger.error("Error happens when tracking the queue "+queueKey,e);
	    			if(jedis!=null) jedis.close();
	    		}finally {
	    			if(jedis!=null&&jedis.isConnected())
	    				CacheHandler.jedisPool.returnResourceObject(jedis);
	    		}
			}
		}, 0 , period, TimeUnit.MILLISECONDS);
	}
}
