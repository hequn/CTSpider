package com.ctbri.spider.ctbigdata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.Jedis;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.entry.MainEntry;
import com.ctbri.spider.utils.CommonTools;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
	public static String lock = "111";
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
    	final BlockingQueue<String> bq = new LinkedBlockingQueue<String>();
    	class AddingT implements Runnable{

			@Override
			public void run() {
				int i = 0 ;
				while(true){
					synchronized (lock) {
						bq.add(i + "");
						i++;
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
			}
    		
    	}
    	Thread t = new Thread(new AddingT());
    	t.start();
    	try {
			Thread.sleep(1000*1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	System.out.println(bq.size());
        for (String s : bq) {
        	try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			@SuppressWarnings("unused")
			int aaa = lock.indexOf("1");
			System.out.println(s+" Size is: "+bq.size());
        }  
        System.out.println(bq.size());
    }
    
    public static void testSave(){
    	File file = new File("D:/test.txt");
    	PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	pw.println("dddddddddddddddd");
    	
//    	String ta = null;
//    	ta.split("e");
    	pw.close();
    }
    
    public void testTimer() throws InterruptedException{
    	ScheduledExecutorService timerPool = Executors.newScheduledThreadPool(2); 
		timerPool.scheduleAtFixedRate(new TimerTask() {
			
			@Override
			public void run() {
				String a= "";
					    		try {
	    			System.out.println(Thread.currentThread().getId());
	    			if(Thread.currentThread().getId()%2==0){
	    				String test = null;
	    				//test.equals("1");
	    				return;
	    			}
	    		} catch(Exception e){
	    			e.printStackTrace();
	    			a=SystemConstants.HUNGUP;
	    		}
					    		System.out.println(Thread.currentThread().getId()+"--test"+a);
			}
		}, 0 , 10000, TimeUnit.MILLISECONDS);
		
		Thread.sleep(Long.MAX_VALUE);
    }
    
    public void testLianjia() throws Exception{
    	File file = new File("f:/lianjia.txt");
    	PrintWriter pw = new PrintWriter(new FileWriter(file));
    	String tpl = "http://sh.lianjia.com/ershoufang/SH%s.html";
    	//1241024
    	DecimalFormat df = new DecimalFormat("0000000000");
    	for(int i = 275010;i<1241024;i++){
    		pw.println(String.format(tpl, df.format(i)));    		
    	}
    	
    	pw.flush();
    	pw.close();
    }
    
    public void testMatcherUrl(){
    	String a = "http://car.autohome.com.cn/config/spec/17712.html#pvareaid=2023154";
    	System.out.println(a.matches("http://car.autohome.com.cn/config/spec/[0-9]+[.]html.*"));
    	Date last_time = new Date(0);
    	System.out.println(last_time.getTime());
    }
}
