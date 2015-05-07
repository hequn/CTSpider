package com.ctbri.spider.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;

import com.ctbri.spider.cache.CacheHandler;
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
public class ShortageItemsLoader implements Runnable{

	private static int period = 1000*1000;//1000 seconds
	private static Logger logger = Logger.getLogger(ShortageItemsLoader.class);
	private static Spider spider = null;

	@Override
	public void run() {
		File fileR = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/RetriedItems");
		if(!fileR.exists()) fileR.mkdir();
		
		while(true){
			try {
				File[] filesSPrice = FileLoadTools.getFilesByDirectory(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice");
				File[] filesSAll = FileLoadTools.getFilesByDirectory(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll");
				
				File[] needRecrawl = ArrayUtils.addAll(filesSPrice, filesSAll);
				
				synchronized (CacheHandler.readWriteLock) {
					for (File file : needRecrawl) {
						String line = null;
						BufferedReader fileReader = new BufferedReader(new FileReader(file));
						while ((line = fileReader.readLine()) != null) {
							spider.addUrl(line);
							logger.info("Adding Failure Url to Scheduler");
						}
						fileReader.close();
						FileUtils.copyFileToDirectory(file, fileR);
						file.delete();
					}
				}
				Thread.sleep(period);
			} catch (Exception e) {
				logger.error("Error happens",e);
			}
		}
	}
	
	public static Spider getSpider() {
		return spider;
	}
	
	public static void setSpider(Spider spider) {
		ShortageItemsLoader.spider = spider;
	}
}
