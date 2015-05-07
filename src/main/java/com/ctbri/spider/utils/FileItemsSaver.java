package com.ctbri.spider.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.Spider.Status;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.model.Entity;

public class FileItemsSaver implements Runnable{

	private static int period = 1000*20;//three seconds
	private static int maxQueueSize = 500;
	private static Logger logger = Logger.getLogger(FileItemsSaver.class);
	private static Spider spider = null;
	@Override
	public void run() {

		File file = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION));
		if(!file.exists()) file.mkdir();
		File fileFull = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/FullParams");
		if(!fileFull.exists()) fileFull.mkdir();
		File fileSOB = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfBrand");
		if(!fileSOB.exists()) fileSOB.mkdir();
		File fileSOP = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice");
		if(!fileSOP.exists()) fileSOP.mkdir();
		File fileSOA = new File(SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll");
		if(!fileSOA.exists()) fileSOA.mkdir();
		
		int sleepCount = 0;
		int oldQueueSize = 0;
		int newQueueSize = 0;
		
		while(true){
			try {
				sleepCount++;
				oldQueueSize = CacheHandler.fileLines.size();
				
				Thread.sleep(period);				
				
				if(spider.getStatus()==Status.Stopped){
					writeNewFiles();
					break;
				}
				
				newQueueSize = CacheHandler.fileLines.size();
				if(newQueueSize > maxQueueSize){
					writeNewFiles();
					sleepCount = 0;
				}else if(newQueueSize!=0) {
					if(sleepCount > 10 && newQueueSize - oldQueueSize == 0){
						writeNewFiles();
						sleepCount = 0;
					}
				}else sleepCount = 0;
			} catch (Exception e) {
				logger.error("Error happens",e);
			}
		}
	}
	
	private void writeNewFiles() throws Exception{
		synchronized (CacheHandler.fileLines) {
			synchronized (CacheHandler.readWriteLock) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
				String tmpFull = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/FullParams/"+sdf.format(new Date())+"-all";
				String tmpShortOfBrand = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfBrand/"+sdf.format(new Date())+"-shortOfBrand";
				String tmpShortOfPrice = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfPrice/"+sdf.format(new Date())+"-shortOfPrice";
				String tmpShortOfAll = SystemConstants.properties.getProperty(SystemConstants.SAVE_LOCATION)+"/ShortOfAll/"+sdf.format(new Date())+"-shortOfAll";
				PrintWriter printWriter1 = new PrintWriter(new FileWriter(tmpFull));
				PrintWriter printWriter2 = new PrintWriter(new FileWriter(tmpShortOfBrand));
				PrintWriter printWriter3 = new PrintWriter(new FileWriter(tmpShortOfPrice));
				PrintWriter printWriter4 = new PrintWriter(new FileWriter(tmpShortOfAll));
				for(Entity e : CacheHandler.fileLines){
					String key = e.getItemName();
					String pName = e.getItemParams().get("productName");
					String pPrice = e.getItemParams().get("productPrice");
					if(pName==null&&pPrice==null) printWriter4.println(key+" "+pName+" "+pPrice);
					else if(pName!=null&&pPrice==null) printWriter3.println(key+" "+pName+" "+pPrice);
					else if(pName==null&&pPrice!=null) printWriter2.println(key+" "+pName+" "+pPrice);
					else printWriter1.println(key+" "+pName+" "+pPrice);
				}
				CacheHandler.fileLines.clear();
				printWriter1.close();
				printWriter2.close();
				printWriter3.close();
				printWriter4.close();
			}
		}		
	}

	public static Spider getSpider() {
		return spider;
	}
	
	public static void setSpider(Spider spider) {
		FileItemsSaver.spider = spider;
	}
}
