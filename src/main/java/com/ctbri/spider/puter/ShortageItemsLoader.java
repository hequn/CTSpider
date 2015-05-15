package com.ctbri.spider.puter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;

import com.ctbri.spider.cache.CacheHandler;

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

	private int period = 1000*3600*6;//1000 seconds
	private Spider spider = null;
	private FailurePageReloader pageReloader = null;
	
	private static Logger logger = Logger.getLogger(ShortageItemsLoader.class);
		
	public ShortageItemsLoader(int period, Spider spider, FailurePageReloader pageReloader) {
		super();
		this.period = period;
		this.spider = spider;
		this.pageReloader = pageReloader;
	}

	@Override
	public void run() {
		
		File fileR = null;
		try {
			fileR = this.pageReloader.initReloadBackPos();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		while(true){
			try {

				File[] needRecrawl = this.pageReloader.getReloadFiles();
				
				synchronized (CacheHandler.readWriteLock) {
					for (File file : needRecrawl) {
						String line = null;
						BufferedReader fileReader = new BufferedReader(new FileReader(file));
						while ((line = fileReader.readLine()) != null) {
							String dealedUrl = pageReloader.pageReloadUrl(line);
							spider.addUrl(dealedUrl);
							logger.info("Adding Failure Url to Scheduler :"+ dealedUrl);
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
}
