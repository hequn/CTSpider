package com.ctbri.spider.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import redis.clients.jedis.Jedis;

import com.alibaba.fastjson.JSON;
import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.model.EntityContainer;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	Some methods tools used by system.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午12:02:30
 */
public class CommonTools {

	private static Logger logger = Logger.getLogger(CommonTools.class);

	/**
	 * 
	 * <p>
	 * 	Convert the time in different String value formated to seconds.
	 * </p>
	 * @param strTime  the time string
	 * @return the seconds represented by strTime
	 */
	public static Long getLongValueOfTimeInSeconds(StringBuffer strTime) {
		int year = 0, month = 0, date = 0, hour = 0, minutes = 0, seconds = 0;
		try {
			if (strTime.length() == 14) {
				year = Integer.parseInt(strTime.substring(0, 4));
				month = Integer.parseInt(strTime.substring(4, 6)) - 1;
				date = Integer.parseInt(strTime.substring(6, 8));
				hour = Integer.parseInt(strTime.substring(8, 10));
				minutes = Integer.parseInt(strTime.substring(10, 12));
				seconds = Integer.parseInt(strTime.substring(12, 14));
			} else if (strTime.length() == 12) {
				year = Integer.parseInt(strTime.substring(0, 4));
				month = Integer.parseInt(strTime.substring(4, 6)) - 1;
				date = Integer.parseInt(strTime.substring(6, 8));
				hour = Integer.parseInt(strTime.substring(8, 10));
				minutes = Integer.parseInt(strTime.substring(10, 12));
			} else if (strTime.length() == 8) {
				year = Integer.parseInt(strTime.substring(0, 4));
				month = Integer.parseInt(strTime.substring(4, 6)) - 1;
				date = Integer.parseInt(strTime.substring(6, 8));
			}
		} catch (Exception e) {
			return null;
		}

		Calendar clstart = Calendar.getInstance();
		clstart.set(year, month, date);
		clstart.set(Calendar.HOUR_OF_DAY, hour);
		clstart.set(Calendar.MINUTE, minutes);
		clstart.set(Calendar.SECOND, seconds);
		clstart.set(Calendar.MILLISECOND, 0);
		return clstart.getTimeInMillis() / 1000;
	}

	/**
	 * 
	 * <p>
	 * Start a new user thread.
	 * </p>
	 * @param run a thread instance 
	 * @param priority the priority of this thread
	 */
	public static void startNewThread(Runnable run, int priority) {
		Thread tTmp = new Thread(run);
		tTmp.setDaemon(false);
		tTmp.setPriority(priority);
		tTmp.start();
	}

	/**
	 * 
	 * <p>
	 * 	Get a jedis instance from the CacheHandler.jedisPool.
	 * </p>
	 * @return the jedis client object
	 */
	public static Jedis getJedisResource() {
		Jedis jedis = null;
		while (true) {
			try {
				jedis = CacheHandler.jedisPool.getResource();
				break;
			} catch (Exception e) {
				logger.error("Redis connection errors, try it again :", e);
				if(jedis!=null) jedis.close();
				try {
					Thread.sleep(1000 * 5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			}
		}
		return jedis;
	}

	/**
	 * 
	 * <p>
	 * Return the jedis object to the pool.
	 * </p>
	 * @param jedis the used jedis instance previous
	 */
	public static void returnJedisResource(Jedis jedis) {
		CacheHandler.jedisPool.returnResourceObject(jedis);
	}

	/**
	 * 
	 * <p>
	 * 	Once batch pages writer worked, it will send the results to server together.
	 * </p>
	 * @param ip the server ip
	 * @param port the server port
	 * @param server the server name
	 * @param entityContainer the results container
	 * @return true if no errors
	 */
	public static boolean sendCrawlResults(String ip, String port,String server,
			EntityContainer entityContainer) {
		boolean result = true;
		try {
			String jsonEC = JSON.toJSONString(entityContainer);
			CloseableHttpClient httpclient = HttpClients.createDefault();
			try {
				HttpPost httpost = new HttpPost("http://" + ip + ":" + port+"/"+ server+ "/SaverServlet");
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				// set params to post
				nvps.add(new BasicNameValuePair(
						SystemConstants.ENTITY_CONTAINER, jsonEC));
				// make it to utf8 encoding
				httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
				RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(7000).setConnectTimeout(3000).build();//设置请求和传输超时时间
				httpost.setConfig(requestConfig);//If the server is down the thread will wait for more than 24 hours
				HttpResponse response = httpclient.execute(httpost);
				HttpEntity entity = response.getEntity();
				EntityUtils.consume(entity);
			} finally {
				httpclient.close();
			}
			logger.debug("Send the results to server successfully");
		} catch (Exception e) {
			logger.error("Send the results to server failed, please check the server",e);
			result = false;
		}
		return result;
	}
	
	/**
	 * 
	 * <p>
	 * For heart beating monitoring. Send the info to CTSpider-Server and get the response.
	 * </p>
	 * @param ip the server ip
	 * @param port the server port
	 * @param server the server name
	 * @param siteName the siteName (item.jd.com e.g.)
	 * @param cachedQueueSize the left cached size
	 * @param keyOnly the uuid of the process
	 * @param initial the first time sending the message or not
	 * @return the sent back command that is RUNNING or HUNGUP, no other string can be
	 */
	public static String sendHeartBeatInfo(String ip, String port,String server,
			String siteName,String cachedQueueSize,String keyOnly,boolean initial) {
		String result = SystemConstants.RUNNING;
		try {
			CloseableHttpClient httpclient = HttpClients.createDefault();
			try {
				HttpPost httpost = new HttpPost("http://" + ip + ":" + port+"/"+ server+ "/RegistServlet");
				List<NameValuePair> nvps = new ArrayList<NameValuePair>();
				// set params to post
				nvps.add(new BasicNameValuePair(SystemConstants.HEART_INIT, initial+""));
				nvps.add(new BasicNameValuePair(SystemConstants.KEY_ONLY, keyOnly));
				nvps.add(new BasicNameValuePair(SystemConstants.REMOTE_SITE_NAME, siteName));
				nvps.add(new BasicNameValuePair(SystemConstants.CACHED_QUEUE_SIZE, cachedQueueSize));
				// set it to utf8 encoding
				httpost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
				RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(7000).setConnectTimeout(3000).build();//设置请求和传输超时时间
				httpost.setConfig(requestConfig);//If the server is down the thread will wait for more than 24 hours
				HttpResponse response = httpclient.execute(httpost);
				HttpEntity entity = response.getEntity();
				result = IOUtils.toString(entity.getContent());
				EntityUtils.consume(entity);
			} finally {
				httpclient.close();
			}
			logger.debug("Send the heartinfo to server successfully");
		} catch (Exception e) {
			logger.error("Send the heart beat info to server failed, please check the network",e);
			//continue to crawl or not
			result = SystemConstants.HUNGUP;
		}
		return result;
	}
}
