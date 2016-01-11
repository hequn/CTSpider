package com.ctbri.spider.scheduler;

import org.apache.commons.codec.digest.DigestUtils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.scheduler.DuplicateRemovedScheduler;
import us.codecraft.webmagic.scheduler.MonitorableScheduler;
import us.codecraft.webmagic.scheduler.component.DuplicateRemover;

import com.alibaba.fastjson.JSON;
import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.utils.CommonTools;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	A customized scheduler that allows pool the url from the 
 *	{@link CacheHandler} <code>cachedNoExtraUrls</code> which will save the cost time 
 *	connecting to the redis server.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:57:24
 */
public class RedisCachedScheduler extends DuplicateRemovedScheduler implements MonitorableScheduler, DuplicateRemover {

    private JedisPool pool;

    private static final String QUEUE_PREFIX = "queue_";

    private static final String ITEM_PREFIX = "item_";
    
    public RedisCachedScheduler(String host) {
        this(new JedisPool(new JedisPoolConfig(), host));
    }

    public RedisCachedScheduler(JedisPool pool) {
        this.pool = pool;
        setDuplicateRemover(this);
    }

    @Override
    public void resetDuplicateCheck(Task task) {
        
    }

    @Override
    public boolean isDuplicate(Request request, Task task) {
        return false;
    }

    @Override
    protected void pushWhenNoDuplicate(Request request, Task task) {
        Jedis jedis = CommonTools.getJedisResource();
        while(true){
        	try {
        		jedis.rpush(getQueueKey(task), request.getUrl());
        		if (request.getExtras() != null) {
        			String field = DigestUtils.shaHex(request.getUrl());
        			String value = JSON.toJSONString(request);
        			jedis.hset((ITEM_PREFIX + task.getUUID()), field, value);
        		}
        	}catch(Exception e){
				logger.error("Error happens when connecting to the redis to push the url to redis",e);
				if(jedis!=null) jedis.close();
				try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
        	}finally {
        		if(jedis!=null&&jedis.isConnected()) pool.returnResourceObject(jedis);
        	}
        	break;
        }
    }

    @Override
    public synchronized Request poll(Task task) {
    	//return if hung up, hung up happens in two ways
    	if(CacheHandler.hungUp==true||
    			CacheHandler.workerSignal.equals(SystemConstants.HUNGUP)) return null;
    	Jedis jedis = null;
    	while(true){
	        try {
	            String url = CacheHandler.cachedNoExtraUrls.poll();
	            if (url == null) {
	                return null;
	            }
	            jedis = CommonTools.getJedisResource();
	            String key = ITEM_PREFIX + task.getUUID();
	            String field = DigestUtils.shaHex(url);
	            //for retried urls
	            byte[] bytes = jedis.hget(key.getBytes(), field.getBytes());
	            if (bytes != null) {
	                Request o = JSON.parseObject(new String(bytes), Request.class);
	                jedis.hdel(key.getBytes(), field.getBytes());
	                return o;
	            }
	            Request request = new Request(url);
	            return request;
	        } catch(Exception e){
	        	logger.error("Error when getting the to be retried url (Request object) for "+ task.getUUID(),e);
	        	if(jedis!=null) jedis.close();
				try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
	        }
	        finally {
	           if(jedis!=null&&jedis.isConnected()) 
	        	   pool.returnResourceObject(jedis);
	        }
    	}
    }

    protected String getQueueKey(Task task) {
        return QUEUE_PREFIX + task.getUUID();
    }

    @Override
    public int getLeftRequestsCount(Task task) {
        Jedis jedis = CommonTools.getJedisResource();
        while(true){
	        try {
	            Long size = jedis.llen(getQueueKey(task));
	            return size.intValue();
	        }catch(Exception e){
				logger.error("Error happens when connecting to the redis to get the left queue count",e);
				if(jedis!=null) jedis.close();
				try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
	    	}finally {
	            if(jedis!=null&&jedis.isConnected()) pool.returnResourceObject(jedis);
	        }
        }
    }

    @Override
    public int getTotalRequestsCount(Task task) {
        Jedis jedis = CommonTools.getJedisResource();
        while(true){
	        try {
	            Long size = jedis.scard(getQueueKey(task));
	            return size.intValue();
	        }catch(Exception e){
				logger.error("Error happens when connecting to the redis to get the request count",e);
				if(jedis!=null) jedis.close();
				try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
	    	}finally {
	            if(jedis!=null&&jedis.isConnected()) pool.returnResourceObject(jedis);
	        }
        }
    }
}
