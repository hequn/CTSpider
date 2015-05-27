package com.ctbri.spider.model;

import java.util.Map;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The model that holds the parsed items from the page.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:02:09
 */
public class Entity {

	private Map<String,Object> itemParams;
	
	public Map<String, Object> getItemParams() {
		return itemParams;
	}
	public void setItemParams(Map<String, Object> itemParams) {
		this.itemParams = itemParams;
	}
}
