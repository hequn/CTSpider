package com.ctbri.spider.model;

import java.util.Map;

public class Entity {

	private String itemName;
	private Map<String,String> itemParams;
	
	public String getItemName() {
		return itemName;
	}
	public void setItemName(String itemName) {
		this.itemName = itemName;
	}
	public Map<String, String> getItemParams() {
		return itemParams;
	}
	public void setItemParams(Map<String, String> itemParams) {
		this.itemParams = itemParams;
	}
}
