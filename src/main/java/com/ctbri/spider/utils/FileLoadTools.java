package com.ctbri.spider.utils;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.log4j.Logger;

/**
 * Load files by path, basic tools.
 * @author hequn
 *2014-8-20 12:36:34
 */
public class FileLoadTools {

	public static Logger logger = Logger.getLogger(FileLoadTools.class);
	
	/**
	 * Get the files in the folder.
	 * @param path
	 * @return all the files in the folder
	 */
	public static File[] getFilesByDirectory(String path){
		File file = null;
		try {
			file = new File(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if(file.isDirectory()==false) {
			logger.error("File is null in getFilesByDirectory and path is "+path);
			return null;
		}
		File[] files = file.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				boolean result = false;
				if(pathname.getName().endsWith(".txt")) result = true;
				else if(pathname.getName().indexOf(".")==-1) result = true;
				return result;
			}
		});
		return files;
	}
	
	/**
	 * Get the file from the classpath.
	 * @param fileName the file name
	 * @param path the path where the file located in
	 * @return the file specified
	 */
	public static File getClassPathFileByName(String fileName,String path) {
		
		Class<?> clazz = FileLoadTools.class;
		URLClassLoader loader = (URLClassLoader) clazz.getClassLoader();
		URL url = null;
		url = loader.getResource(path+fileName);
		if(url == null) {
			logger.error("File read failed!");
			return null;	
		}
		File file = null;
		try {
			file = new File(url.getFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return file;
	}
}
