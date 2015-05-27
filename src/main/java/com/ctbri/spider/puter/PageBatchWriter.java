package com.ctbri.spider.puter;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.model.EntityContainer;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The interface indicates that the implementation should initial the position to save the result, 
 *	also batch write the result from the {@link CacheHandler} resultLines queue.<br/> 
 *	<strong>The interface helps writing the items to the local machine disk where runs the jar process</strong>.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:16:17
 */
public interface PageBatchWriter {
	
	/**
	 * 
	 * <p>
	 * 	Initial the position folders for saving the items.
	 * </p>
	 * @throws Exception
	 */
	public void initBatchWritePos() throws Exception;
	
	/**
	 * 
	 * <p>
	 * 	Batch write the items to files.
	 * </p>
	 * @param maxPageCount the max size to write out at one time
	 * @return the results container which will be sent to the server next
	 * @throws Exception
	 */
	public EntityContainer pageBatchWrite(int maxPageCount) throws Exception;

}
