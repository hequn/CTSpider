package com.ctbri.spider.puter;

import java.io.File;

/**
 * 
 * Copyright 2015 CTBRI
 * All right reserved.
 * <p>
 *	The reloading logic of the bad pages.
 *	<ol>
 *		<li>
 *			Create the dir to backup the bad pages {@link #initReloadBackPos()}.
 *		</li>
 *		<li>
 *			Get the reload files {@link #getReloadFiles()}.
 *		</li>
 *		<li>
 *			Combine the params and get the final reload url {@link #pageReloadUrl()}.
 *		</li>
 *	</ol>
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2015年5月22日 下午2:26:10
 */
public interface BadPageReloader {
	
	/**
	 * 
	 * <p>
	 * 	Get the reload files (They will be read by line).
	 * </p>
	 * @param saveLocation the location configured in property files
	 * @return the files should be reloaded.
	 * @throws Exception
	 */
	public File[] getReloadFiles(String saveLocation) throws Exception;
	
	/**
	 * 
	 * <p>
	 * 	Initial the backup directory to store the reloaded files. These files 
	 * 	will be deleted after reloading procedure.
	 * </p>
	 * @param saveLocation the location configured in property files
	 * @return the directory for backup reload files
	 * @throws Exception
	 */
	public File initReloadBackPos(String saveLocation) throws Exception;
	
	/**
	 * 
	 * <p>
	 * 	The logical combing for the url for we will not save the url itself to file.
	 * </p>
	 * @param oneLine one line in the reloaded file
	 * @return the new url to be crawled
	 * @throws Exception
	 */
	public String pageReloadUrl(String oneLine) throws Exception;

}
