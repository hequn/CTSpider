package com.ctbri.spider.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Spider.Status;

import com.ctbri.spider.cache.SystemConstants;

/**
 * 
 *  Copyright 2014 SOGOU
 *  All right reserved.
 *	<p>
 *	The file tracker to track the file, if the menu of one product modified,
 *  the product will load again to update the information.
 *	</p>
 * @author Qun He
 * @Creat Time : 2014-9-2 下午2:51:12
 * @VersionFileTracker
 */
public class FileItemsLoader implements Runnable{

	private static int period = 1000*20;//20 seconds
	private static int maxQueueSize = 5000;
	private static Logger logger = Logger.getLogger(FileItemsLoader.class);
	private static Spider spider = null;

	@Override
	public void run() {
    	try {
			File[] files = FileLoadTools.getFilesByDirectory(SystemConstants.properties.getProperty(SystemConstants.SEED_LOCATION));
			for (File file : files) {
				int countLine = 0;
				String line = null;
				BufferedReader fileReader = new BufferedReader(new FileReader(file));
				while ((line=fileReader.readLine())!=null) {
					spider.addUrl(line);
					logger.info("Adding Url to Scheduler");
					if(spider.getStatus()==Status.Init) {
						Thread newThread = new Thread(spider);
						newThread.setDaemon(false);
						newThread.start();
					}
					countLine++;
					if(countLine > maxQueueSize){
						Thread.sleep(period);
						countLine = 0;
					}
				}
				fileReader.close();
			}
		} catch (Exception e) {
			logger.error("Error happens",e);
		}
	}
	
	public static Spider getSpider() {
		return spider;
	}
	
	public static void setSpider(Spider spider) {
		FileItemsLoader.spider = spider;
	}
}
