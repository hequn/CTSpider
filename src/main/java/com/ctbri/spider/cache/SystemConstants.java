package com.ctbri.spider.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 * The constants to be used in the project , the properties will contain the value,
 * so the key should not be kept the same with the Strings like REDIS_PORT.
 * </p>
 * 
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 上午11:42:57
 */
public class SystemConstants {
	
	//the properties from the jar itself get connection information
	public static Properties propertiesConnection = new Properties();
	//the properties will be parsed from the json string pushed by the CTSpider-Server
	public static Properties propertiesControl = new Properties();
	
	//the name of the configuration file
	public static final String resource = "SystemConstants.properties";

	//the constant String in the propertiesConnection properties
	public static final String PAGE_PROCESSOR = "PAGE_PROCESSOR";
	public static final String DOMAIN_KEY = "DOMAIN_KEY";
	public static final String REDIS_IP = "REDIS_IP";
	public static final String CTSERVER_IP = "CTSERVER_IP";
	public static final String REDIS_PORT = "REDIS_PORT";
	public static final String CTSERVER_PORT = "CTSERVER_PORT";
	public static final String CTSERVER_NAME = "CTSERVER_NAME";
	public static final String REDIS_MAX_ACTIVE="REDIS_MAX_ACTIVE";
	public static final String REDIS_MAX_WAIT="REDIS_MAX_WAIT";
	public static final String REDIS_MAX_IDLE="REDIS_MAX_IDLE";
	public static final String REDIS_TIME_OUT = "REDIS_TIME_OUT";
	public static final String REDIS_PASSWORD = "REDIS_PASSWORD";
	//the constant String in the propertiesControl properties
	public static final String TH_COUNT = "TH_COUNT";
	public static final String CRAWL_RATE = "CRAWL_RATE";
	public static final String RETRIED_TIMES = "RETRIED_TIMES";
	public static final String CYCLE_RETRIED_TIMES = "CYCLE_RETRIED_TIMES";
	public static final String DOWNLOAD_TIMEOUT = "DOWNLOAD_TIMEOUT";
	public static final String WAIT_EMPTY = "WAIT_EMPTY";	
	public static final String PROXY_CRAWL = "PROXY_CRAWL";
	public static final String PROXY_QUEUE = "PROXY_QUEUE";
	public static final String SAVE_LOCATION = "SAVE_LOCATION";
	public static final String PJ_COMMAND = "PJ_COMMAND";
	public static final String RELOAD_WAIT = "RELOAD_WAIT";
	public static final String RESULT_WAIT_CHECK = "RESULT_WAIT_CHECK";
	public static final String RESULT_MAX_QUEUE = "RESULT_MAX_QUEUE";
	public static final String RELOAD_SHORTAGE_PAGE = "RELOAD_SHORTAGE_PAGE";
	public static final String NET_ETH = "NET_ETH";
	public static final String MIXED_VERSION = "MIXED_VERSION";
	
	//the constants of the system
	public static final String SPLITTER = "/";
	
	//the params using in redis hash set
	public static final String QUEUE_PREFIX="queue_";
	public static final String PARAM_PREFIX="param_";
	public static final String LAST_TOTAL_KEY="lastpush";
	public static final String FINISH_PUSHED_KEY="finishpush";
	public static final String HUNGUP_CLIENT_KEY="hungupclient";
	public static final String CONFIGURATION_KEY="configuration";
	public static final String PARAM_UPDATE_KEY="paramupdate";
	
	//the params passing to CTSpider-Server by post methods
	public static final String ENTITY_CONTAINER = "entityContainer";
	public static final String REMOTE_SITE_NAME = "remotesitename";
	public static final String KEY_ONLY = "keyonly";
	public static final String HEART_INIT = "heartinit";
	public static final String CACHED_QUEUE_SIZE = "cachedqueuesize";
	
	//the state of the jar itself
	public static final String RUNNING = "RUNNING";
	public static final String HUNGUP = "HUNGUP";
	//load the key value into properties
	static {
		try {
			InputStream is = ClassLoader.getSystemResourceAsStream(resource);
			propertiesConnection.load(is);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
