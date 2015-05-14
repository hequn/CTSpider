package com.ctbri.spider.ctbigdata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
}
