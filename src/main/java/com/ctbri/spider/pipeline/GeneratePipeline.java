package com.ctbri.spider.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.model.Entity;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The pipeline which adds the crawling results to {@link Entity} and then adds it to 
 *	<code>CacheHandler.resultLines</code>.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:08:55
 */
public class GeneratePipeline implements Pipeline {
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public void process(ResultItems resultItems, Task task) {
		try {
			Entity e = new Entity();
			e.setItemParams(resultItems.getAll());
			CacheHandler.resultLines.offer(e);
		} catch (Exception e) {
			logger.error("Adding result error",e);
		}
	}
}
