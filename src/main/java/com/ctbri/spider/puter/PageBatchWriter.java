package com.ctbri.spider.puter;

public interface PageBatchWriter {
	
	public void initBatchWritePos() throws Exception;
	
	public void pageBatchWrite(int maxPageCount) throws Exception;

}
