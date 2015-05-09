package com.ctbri.spider.scheduler;

import com.alibaba.fastjson.JSON;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.scheduler.DuplicateRemovedScheduler;
import us.codecraft.webmagic.scheduler.MonitorableScheduler;
import us.codecraft.webmagic.scheduler.component.DuplicateRemover;

/**
 * Use Redis as url scheduler for distributed crawlers.<br>
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class RedisDuplicateScheduler extends DuplicateRemovedScheduler implements MonitorableScheduler, DuplicateRemover {

    private JedisPool pool;

    private static final String QUEUE_PREFIX = "queue_";

    private static final String ITEM_PREFIX = "item_";

    private static Logger logger = LoggerFactory.getLogger(RedisDuplicateScheduler.class);
    
    public RedisDuplicateScheduler(String host) {
        this(new JedisPool(new JedisPoolConfig(), host));
    }

    public RedisDuplicateScheduler(JedisPool pool) {
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
        Jedis jedis = null;
        while(true){
        	try {
        		jedis = pool.getResource();
        		break;
        	} catch (Exception e) {
        		logger.info("Redis connection errors, try it again :",e);
        		pool.returnBrokenResource(jedis);
        		try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
        		continue;
        	}
        }
        try {
            jedis.rpush(getQueueKey(task), request.getUrl());
            if (request.getExtras() != null) {
                String field = DigestUtils.shaHex(request.getUrl());
                String value = JSON.toJSONString(request);
                jedis.hset((ITEM_PREFIX + task.getUUID()), field, value);
            }
        } finally {
            pool.returnResource(jedis);
        }
    }

    @Override
    public synchronized Request poll(Task task) {
        Jedis jedis = null;
        while(true){
        	try {
        		jedis = pool.getResource();
        		break;
        	} catch (Exception e) {
        		logger.info("Redis connection errors, try it again :",e);
        		pool.returnBrokenResource(jedis);
        		try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
        		continue;
        	}
        }
        try {
            String url = jedis.lpop(getQueueKey(task));
            if (url == null) {
                return null;
            }
            String key = ITEM_PREFIX + task.getUUID();
            String field = DigestUtils.shaHex(url);
            byte[] bytes = jedis.hget(key.getBytes(), field.getBytes());
            if (bytes != null) {
                Request o = JSON.parseObject(new String(bytes), Request.class);
                return o;
            }
            Request request = new Request(url);
            return request;
        } finally {
            pool.returnResource(jedis);
        }
    }

    protected String getQueueKey(Task task) {
        return QUEUE_PREFIX + task.getUUID();
    }

    @Override
    public int getLeftRequestsCount(Task task) {
        Jedis jedis = null;
        while(true){
        	try {
        		jedis = pool.getResource();
        		break;
        	} catch (Exception e) {
        		logger.info("Redis connection errors, try it again :",e);
        		pool.returnBrokenResource(jedis);
        		try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
        		continue;
        	}
        }
        try {
            Long size = jedis.llen(getQueueKey(task));
            return size.intValue();
        } finally {
            pool.returnResource(jedis);
        }
    }

    @Override
    public int getTotalRequestsCount(Task task) {
        Jedis jedis = null;
        while(true){
        	try {
        		jedis = pool.getResource();
        		break;
        	} catch (Exception e) {
        		logger.info("Redis connection errors, try it again :",e);
        		pool.returnBrokenResource(jedis);
        		try {
					Thread.sleep(1000*5);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
        		continue;
        	}
        }
        try {
            Long size = jedis.scard(getQueueKey(task));
            return size.intValue();
        } finally {
            pool.returnResource(jedis);
        }
    }
}
