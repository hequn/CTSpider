package com.ctbri.spider.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.model.Entity;

public class GeneratePipeline implements Pipeline {
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * ctbri pipe with default path "/data/webmagic/" and fix the file not exists bug under windows
	 */
	@Override
	public void process(ResultItems resultItems, Task task) {
		try {
			Entity e = new Entity();
			e.setItemParams(resultItems.getAll());
			CacheHandler.resultLines.offer(e);
		} catch (Exception e) {
			logger.warn("Adding result error",e);
		}
	}
}
