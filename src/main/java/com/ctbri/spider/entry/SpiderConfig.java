package com.ctbri.spider.entry;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.Downloader;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The interface that allows the Processor to get {@link Downloader} and set the {@link Spider} to itself.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午1:54:19
 */
public interface SpiderConfig {
		
	/**
	 * 
	 * <p>
	 * 	Get the downloader. For the reason of get it is that in the previous
	 *  framework the downloader can be just get by spider, but now solving
	 *  the ajax problems needs to download some other urls in the processor 
	 *  process method.
	 * </p>
	 * @return the downloader spider needs
	 */
	public Downloader getDownloader();
	
	/**
	 * 
	 * <p>
	 * 	Set the spider back to the process, especially for the ajax downloading, too.
	 * </p>
	 * @param spider
	 */
	public void setSpider(Spider spider);
	
}
