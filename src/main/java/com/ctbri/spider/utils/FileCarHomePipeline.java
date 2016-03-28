package com.ctbri.spider.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.annotation.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Map;

/**
 * 
 * Copyright 2016 CTBRI
 * All right reserved.
 * <p>
 *	The spec file pipline for Car Home.
 * </p>
 * 
 * @author Qun He
 * @version 1.0.0
 * @Create 2016年1月18日 下午5:20:57
 */
@ThreadSafe
public class FileCarHomePipeline extends FilePersistentBase implements Pipeline {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * create a FilePipeline with default path"/data/webmagic/"
     */
    public FileCarHomePipeline() {
        setPath("/data/webmagic/");
    }

    public FileCarHomePipeline(String path) {
        setPath(path);
    }

    @Override
    public void process(ResultItems resultItems, Task task) {
        String path = this.path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR;
        try {
        	String url = resultItems.getRequest().getUrl();
        	logger.warn("Download : "+url);
            PrintWriter printWriter = null;
            	if(url.matches("http://www.autohome.com.cn/spec/[0-9]+/#.*"))	printWriter = new PrintWriter(new OutputStreamWriter(
            		new FileOutputStream(getFile(path + Base64.encodeBase64String(resultItems.getRequest().getUrl().getBytes()) + ".spec"))
            		,"UTF-8"));
            	else if(url.matches("http://www.autohome.com.cn/[0-9]+/#.*")) printWriter = new PrintWriter(new OutputStreamWriter(
                		new FileOutputStream(getFile(path + Base64.encodeBase64String(resultItems.getRequest().getUrl().getBytes()) + ".top"))
                		,"UTF-8"));
            	else if(url.matches("http://www.autohome.com.cn/spec/[0-9]+/config[.]html#.*")) printWriter = new PrintWriter(new OutputStreamWriter(
            			new FileOutputStream(getFile(path + Base64.encodeBase64String(resultItems.getRequest().getUrl().getBytes()) + ".config"))
            			,"UTF-8"));
            if(printWriter == null) return;
            for (Map.Entry<String, Object> entry : resultItems.getAll().entrySet()) {
                if (entry.getValue() instanceof Iterable) {
                    Iterable value = (Iterable) entry.getValue();
                    for (Object o : value) {
                        printWriter.println(o);
                    }
                } else {
                    printWriter.println(entry.getValue());
                }
            }
            printWriter.close();
        } catch (IOException e) {
            logger.warn("write file error", e);
        }
    }
}
