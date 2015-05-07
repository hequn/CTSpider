package com.ctbri.spider.cache;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.ctbri.spider.model.Entity;

/**
 * 
 *  Copyright 2014 SOGOU
 *  All right reserved.
 *	<p>
 *	The very important heaps of the system, they are used to keep the 
 *	information needed. 
 *	</p>
 * @author Qun He
 * @Creat Time : 2014-9-15 下午3:22:45
 * @CacheHandler
 */
public class CacheHandler {
	//The product holder(loader)
	public static final BlockingQueue<Entity> fileLines = new LinkedBlockingQueue<>();
	
	public static final String readWriteLock = new String("readWriteLock");
}
