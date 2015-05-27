package com.ctbri.spider.puter;

import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Spider.Status;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.model.EntityContainer;
import com.ctbri.spider.utils.CommonTools;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The controller of the {@link PageBatchWriter}. It will check the {@link CacheHandler} resultLines 
 *	every period milliseconds and will stop only when the registered spider stopped. Not every check will 
 *	lead to batch writing, maxQueueSize will determine it.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:49:51
 */
public class PageBatchWriterController implements Runnable{

	private int period = 0;
	private int maxQueueSize = 0;
	private Spider spider = null;
	private PageBatchWriter pageBatchWriter = null;
	
	private static Logger logger = Logger.getLogger(PageBatchWriterController.class);
	
	public PageBatchWriterController(int period, int maxQueueSize , Spider spider , PageBatchWriter pageBatchWriter) {
		super();
		this.period = period;
		this.maxQueueSize = maxQueueSize;
		this.spider = spider;
		this.pageBatchWriter = pageBatchWriter;
	}

	@Override
	public void run() {
		
		logger.info("The PageBatchWriterController starts");
		try {
			this.pageBatchWriter.initBatchWritePos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		int sleepCount = 0;
		int oldQueueSize = 0;
		int newQueueSize = 0;
		
		while(true){
			try {
				sleepCount++;
				oldQueueSize = CacheHandler.resultLines.size();
				
				Thread.sleep(period);				
				
				if(spider.getStatus()==Status.Stopped){
					logger.info("Spider stopped, stop the specific pageBatchWriter,too");
					break;
				}
				
				newQueueSize = CacheHandler.resultLines.size();
				//if maxSize reached, writeNewItems
				if(newQueueSize > maxQueueSize){
					writeNewItems();
					sleepCount = 0;
				}else if(newQueueSize!=0) {
					//if waiting for a long time and the queue size has no increasing
					//means probably there is no more urls, writeNewItems at once
					if(sleepCount > 10 && newQueueSize - oldQueueSize == 0){
						logger.debug("Results queue size keep same for too long time, so there is no more Urls "
								+ "to crawl or the network worked not well, so wirte result out although the "
								+ "maxQueueSize is not reached");
						writeNewItems();
						sleepCount = 0;
					}
				}else sleepCount = 0;
			} catch (Exception e) {
				logger.error("Error happens when try to batch write the results out",e);
			}
		}
	}
	/**
	 * 
	 * <p>
	 * 	Write the items to files and get the returned {@link EntityContainer}, then 
	 * 	send the instance to CTSpider-Server in json formatted.
	 * </p>
	 * @throws Exception
	 */
	private void writeNewItems() throws Exception{
		synchronized (CacheHandler.readWriteLock) {
			EntityContainer ec = pageBatchWriter.pageBatchWrite(maxQueueSize);
			boolean sendF = CommonTools.sendCrawlResults(
				SystemConstants.propertiesConnection.getProperty(SystemConstants.CTSERVER_IP),
				SystemConstants.propertiesConnection.getProperty(SystemConstants.CTSERVER_PORT),
				SystemConstants.propertiesConnection.getProperty(SystemConstants.CTSERVER_NAME),
				ec
			);
			if(sendF == false) logger.error("Connection error, please check the CTSpider Server");
		}
	}
}
