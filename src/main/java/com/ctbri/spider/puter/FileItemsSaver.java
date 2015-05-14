package com.ctbri.spider.puter;

import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Spider.Status;

import com.ctbri.spider.cache.CacheHandler;

public class FileItemsSaver implements Runnable{

	private int period = 1000*20;//three seconds
	private int maxQueueSize = 500;
	private Spider spider = null;
	private PageBatchWriter pageBatchWriter = null;
	
	private static Logger logger = Logger.getLogger(FileItemsSaver.class);
	
	public FileItemsSaver(int period, int maxQueueSize , Spider spider , PageBatchWriter pageBatchWriter) {
		super();
		this.period = period;
		this.maxQueueSize = maxQueueSize;
		this.spider = spider;
		this.pageBatchWriter = pageBatchWriter;
	}

	@Override
	public void run() {

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
					writeNewItems();
					break;
				}
				
				newQueueSize = CacheHandler.resultLines.size();
				if(newQueueSize > maxQueueSize){
					writeNewItems();
					sleepCount = 0;
				}else if(newQueueSize!=0) {
					if(sleepCount > 10 && newQueueSize - oldQueueSize == 0){
						writeNewItems();
						sleepCount = 0;
					}
				}else sleepCount = 0;
			} catch (Exception e) {
				logger.error("Error happens",e);
			}
		}
	}
	
	private void writeNewItems() throws Exception{
		synchronized (CacheHandler.readWriteLock) {
			pageBatchWriter.pageBatchWrite(maxQueueSize);
		}
	}
}
