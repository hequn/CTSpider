package com.ctbri.spider.puter;

import java.io.File;

public interface FailurePageReload {
	
	public File[] getReloadFiles() throws Exception;
	
	public File initReloadBackPos() throws Exception;
	
	public String pageReloadUrl(String oneLine) throws Exception;

}
