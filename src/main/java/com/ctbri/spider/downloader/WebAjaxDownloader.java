package com.ctbri.spider.downloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.selector.PlainText;

import com.ctbri.spider.cache.SystemConstants;

public class WebAjaxDownloader extends AbstractDownloader{

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public Page download(Request request, Task task) {
        try {
            Page page = handleResponse(request,task);
            onSuccess(request);
            return page;
        } catch (IOException e) {
            logger.warn("download page " + request.getUrl() + " error", e);
            onError(request);
            return null;
        } finally {

        }
    }

    @Override
    public void setThread(int thread) {
        
    }

    protected Page handleResponse(Request request,  Task task) throws IOException {
        String content = getAjaxContent(request.getUrl());//getContent(charset, httpResponse);
        Page page = new Page();
        page.setRawText(content);
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(200);
        return page;
    }
   
    protected String getAjaxContent(String url) throws IOException {
        Runtime rt = Runtime.getRuntime();
        //Process p = rt.exec(SystemConstants.propertiesControl.getProperty(SystemConstants.PJ_COMMAND)+url);//PhantomJS 处理
        //Process p = rt.exec("phantomjs /ctbri/phantomjs/bin/codes.js "+url);//PhantomJS 处理
        Process p = rt.exec("D:/Software/phantomjs-2.0.0-windows/bin/phantomjs.exe D:/Software/phantomjs-2.0.0-windows/bin/codes.js "+url);//PhantomJS 处理
        InputStream is = p.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuffer sbf = new StringBuffer();
        String tmp = "";
        while((tmp = br.readLine())!=null){
            sbf.append(tmp);
        }
        return sbf.toString();
    }
}