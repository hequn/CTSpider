package com.ctbri.spider.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The container in charge of the batched results, once the local file writing method
 *	finished, the network sending procedure will be executed.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:02:59
 */
public class EntityContainer {

	//the domain name item.jd.com e.g.
	private String domainKey = null;
	//the params order for printing in the server side 
	private List<String> paramsOrder = new ArrayList<>();
	//the results lists
	private List<Entity> entities = new ArrayList<>();
	
	public String getDomainKey() {
		return domainKey;
	}
	public void setDomainKey(String domainKey) {
		this.domainKey = domainKey;
	}
	public List<String> getParamsOrder() {
		return paramsOrder;
	}
	public void setParamsOrder(List<String> paramsOrder) {
		this.paramsOrder = paramsOrder;
	}
	public List<Entity> getEntities() {
		return entities;
	}
	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}
}
