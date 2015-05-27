package com.ctbri.spider.puter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Spider.Status;

import com.ctbri.spider.cache.CacheHandler;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The controller of the {@link BadPageReloader}. It will reload the 
 *	files every period milliseconds and will stop only when the 
 *	registered spider stopped.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:46:19
 */
public class BadPageReloaderController implements Runnable{

	private int period = 0;
	private Spider spider = null;
	private BadPageReloader pageReloader = null;
	
	private static Logger logger = Logger.getLogger(BadPageReloaderController.class);
		
	public BadPageReloaderController(int period, Spider spider, BadPageReloader pageReloader) {
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
					logger.debug("Adding Failure Url to Scheduler");
					for (File file : needRecrawl) {
						String line = null;
						BufferedReader fileReader = new BufferedReader(new FileReader(file));
						while ((line = fileReader.readLine()) != null) {
							String dealedUrl = pageReloader.pageReloadUrl(line);
							spider.addUrl(dealedUrl);
						}
						fileReader.close();
						FileUtils.copyFileToDirectory(file, fileR);
						file.delete();
					}
				}
				Thread.sleep(period);
				if(spider.getStatus()==Status.Stopped){
					logger.info("The holded spider stopped by configuration updated, "
							+ "stop the BadPageReloaderController too");
					break;
				}
			} catch (Exception e) {
				logger.error("Error happens while adding the failure urls",e);
			}
		}
	}
}
