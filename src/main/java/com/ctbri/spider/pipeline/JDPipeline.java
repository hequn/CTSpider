package com.ctbri.spider.pipeline;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.model.Entity;

public class JDPipeline extends FilePersistentBase implements Pipeline {
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * ctbri pipe with default path "/data/webmagic/" and fix the file not exists bug under windows
	 */
	public JDPipeline() {
		setPath("/data/webmagic");
	}

	public JDPipeline(String path) {
		setPath(path);
	}

	@Override
	public void process(ResultItems resultItems, Task task) {
		try {
			String productId = resultItems.get("productId");
			String productName = resultItems.get("productName");
			String productPrice = resultItems.get("productPrice");
			Entity e = new Entity();
			e.setItemName(productId);
			e.setItemParams(new HashMap<String,String>());
			e.getItemParams().put("productName", productName);
			e.getItemParams().put("productPrice", productPrice);
			
			CacheHandler.fileLines.offer(e);
		} catch (Exception e) {
			logger.warn("Adding result error",e);
		}
	}
}
