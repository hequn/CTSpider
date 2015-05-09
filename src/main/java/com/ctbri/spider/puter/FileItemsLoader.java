package com.ctbri.spider.puter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Spider.Status;

import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.utils.FileLoadTools;
import com.ctbri.spider.utils.ThreadTool;

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

	private int period = 1000*20;//20 seconds
	private int maxQueueSize = 5000;
	private Spider spider = null;
	
	private static Logger logger = Logger.getLogger(FileItemsLoader.class);
	
	public FileItemsLoader(int period, int maxQueueSize,Spider spider) {
		super();
		this.period = period;
		this.maxQueueSize = maxQueueSize;
		this.spider = spider;
	}

	@Override
	public void run() {
    	while (true) {
			try {
				File[] files = FileLoadTools
						.getFilesByDirectory(SystemConstants.properties
								.getProperty(SystemConstants.SEED_LOCATION));
				if (files.length == 0) {
					if (spider.getStatus() == Status.Init) {
						ThreadTool.startNewThread(spider, Thread.NORM_PRIORITY);
					}else if(spider.getStatus() == Status.Stopped) break;
				}
				for (File file : files) {
					int countLine = 0;
					String line = null;
					BufferedReader fileReader = new BufferedReader(
							new FileReader(file));
					while ((line = fileReader.readLine()) != null) {
						spider.addUrl(line);
						logger.info("Adding Url to Scheduler");
						if (spider.getStatus() == Status.Init) {
							ThreadTool.startNewThread(spider,
									Thread.NORM_PRIORITY);
						}
						countLine++;
						if (countLine > maxQueueSize) {
							Thread.sleep(period);
							countLine = 0;
						}
					}
					fileReader.close();
					file.delete();
				}
			} catch (Exception e) {
				logger.error("Error happens", e);
			}
			try {
				Thread.sleep(period*10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
