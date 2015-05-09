package com.ctbri.spider.utils;

import java.util.Calendar;

/**
 * 
 *  Copyright 2014 SOGOU
 *  All right reserved.
 *	<p>
 *	Deal with the time of different formation.
 *	</p>
 * @author Qun He
 * @Creat Time : 2014-9-2 下午4:30:13
 * @TimeTools
 */
public class ThreadTool {
	
	public static Long getLongValueOfTimeInSeconds(StringBuffer strTime){
		int year = 0, month = 0, date = 0 , hour = 0 , minutes = 0 , seconds = 0;
		try{
			 if (strTime.length() == 14) {
				year = Integer.parseInt(strTime.substring(0, 4));
				month = Integer.parseInt(strTime.substring(4, 6)) - 1;
				date = Integer.parseInt(strTime.substring(6, 8));
				hour = Integer.parseInt(strTime.substring(8, 10));
				minutes = Integer.parseInt(strTime.substring(10, 12));
				seconds = Integer.parseInt(strTime.substring(12, 14));
			}else if(strTime.length() == 12){
				year = Integer.parseInt(strTime.substring(0, 4));
				month = Integer.parseInt(strTime.substring(4, 6)) - 1;
				date = Integer.parseInt(strTime.substring(6, 8));
				hour = Integer.parseInt(strTime.substring(8, 10));
				minutes = Integer.parseInt(strTime.substring(10, 12));
			}
			 else if(strTime.length() == 8){
				year = Integer.parseInt(strTime.substring(0, 4));
				month = Integer.parseInt(strTime.substring(4, 6)) - 1;
				date = Integer.parseInt(strTime.substring(6, 8));
			}
		}catch(Exception e){
			return null;
		}
		
		Calendar clstart = Calendar.getInstance();
		clstart.set(year, month, date);
		clstart.set(Calendar.HOUR_OF_DAY, hour);
		clstart.set(Calendar.MINUTE, minutes);
		clstart.set(Calendar.SECOND, seconds);
		clstart.set(Calendar.MILLISECOND, 0);
		return clstart.getTimeInMillis()/1000;
	}
	
	public static void startNewThread(Runnable run , int priority){
		Thread tTmp = new Thread(run);
		tTmp.setDaemon(false);
		tTmp.setPriority(priority);
		tTmp.start();
	}
}
