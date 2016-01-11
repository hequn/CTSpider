package com.ctbri.spider.tracker;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.entry.MainEntry;
import com.ctbri.spider.utils.CommonTools;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 * A tracker that update the information from the redis, the information is set by CTSpider-Server.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 上午11:52:45
 */
public class UpdateInfoTracker {

	//the rate of executing
	private static int period = 1000*10;//ten seconds

	private static Logger logger = Logger.getLogger(UpdateInfoTracker.class);
	
	/**
	 * Start the tracker method.
	 * @param targetPageP the processor object
	 */
	public static void startTracker(final Object targetPageP){
		CacheHandler.timerPoolUI.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				String updateConf = null;
				String hungUp = null;
				String finishPush = null;
				String lastTotal = null;
				
	    		Jedis jedis = CommonTools.getJedisResource();
	    		try {
	    			
	    			//get the keys
	    			String domainKey = SystemConstants.propertiesConnection.getProperty(SystemConstants.DOMAIN_KEY);
	    			String paramKey = SystemConstants.PARAM_PREFIX + domainKey;
	    			//send the heart beat and then get the running command from the server, 
	    			//keep running or hungup , this is especially for this process not for
	    			//other distributed jars
	    			CacheHandler.workerSignal = CommonTools.sendHeartBeatInfo(
	    					SystemConstants.propertiesConnection.getProperty(SystemConstants.CTSERVER_IP),
	    					SystemConstants.propertiesConnection.getProperty(SystemConstants.CTSERVER_PORT),
	    					SystemConstants.propertiesConnection.getProperty(SystemConstants.CTSERVER_NAME),
	    					domainKey,
	    					CacheHandler.cachedNoExtraUrls.size()+"",
	    					CacheHandler.processUUID,
	    					false
	    			);
	    			//if it does not need to work then return at this time, in order to avoid the situation that network has 
	    			//already been off
	    			if(CacheHandler.workerSignal.equals(SystemConstants.HUNGUP)) return;
	    			//start to get the configuration
	    			updateConf = jedis.hget(paramKey,SystemConstants.PARAM_UPDATE_KEY);
	    			if(updateConf!=null && !updateConf.equals(CacheHandler.confVersion)){
	    				//if updateConf no longer the same with the cached one, means configuration changed
	    				//reload all, but not trackers
	    				MainEntry.setSystemConfigurationAndStart(targetPageP);
	    			}
	    			hungUp = jedis.hget(paramKey, SystemConstants.HUNGUP_CLIENT_KEY);
	    			if(hungUp!=null){
	    				//if the server tell all the clients to hung up
	    				CacheHandler.hungUp = Boolean.valueOf(hungUp);
	    			}
	    			finishPush = jedis.hget(paramKey, SystemConstants.FINISH_PUSHED_KEY);
	    			if(finishPush!=null){
	    				//if the push finished
	    				CacheHandler.finishPush = Boolean.valueOf(finishPush);
	    			}
	    			lastTotal = jedis.hget(paramKey, SystemConstants.LAST_TOTAL_KEY);
	    			if(lastTotal!=null){
	    				//the item size pushed last time
	    				CacheHandler.lastTotal = Integer.valueOf(lastTotal);
	    			}
	    			logger.debug("Try UpdateInfoTracker once");
	    		} catch(Exception e){
	    			logger.error("Error happens",e);
	    			CacheHandler.workerSignal = SystemConstants.HUNGUP;
	    			if(jedis!=null) jedis.close();
	    		}finally {
	    		  if(jedis!=null&&jedis.isConnected())
	    			  CacheHandler.jedisPool.returnResourceObject(jedis);
	    		}
			}
		}, 0 , period, TimeUnit.MILLISECONDS);
	}

}
