package com.ctbri.spider.downloader;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.downloader.HttpClientGenerator;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.HttpConstant;
import us.codecraft.webmagic.utils.UrlUtils;

import com.ctbri.spider.cache.SystemConstants;
import com.google.common.collect.Sets;

public class OriginalDownloader extends AbstractDownloader{

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, CloseableHttpClient> httpClients = new HashMap<String, CloseableHttpClient>();

    private HttpClientGenerator httpClientGenerator = new HttpClientGenerator();

    private CloseableHttpClient getHttpClient(String url, Site site) {
    	
    	if (site == null) {
    		return httpClientGenerator.getClient(null);
    	}
    	
    	String domain = null;
    	CloseableHttpClient httpClient = null;
    	Boolean mixedVersion = Boolean.valueOf(SystemConstants.propertiesConnection.getProperty(SystemConstants.MIXED_VERSION));   	
    	if(mixedVersion){
    		domain = UrlUtils.getDomain(url);
    	}
    	httpClient = httpClients.get(domain);
    	if (httpClient == null) {
    		synchronized (this) {
    			httpClient = httpClients.get(domain);
    			if (httpClient == null) {
    				httpClient = httpClientGenerator.getClient(site);
    				httpClients.put(domain, httpClient);
    			}
    		}
    	}
        return httpClient;
    }

    @Override
    public Page download(Request request, Task task) {
        Site site = null;
        if (task != null) {
            site = task.getSite();
        }
        Set<Integer> acceptStatCode;
        String charset = null;
        Map<String, String> headers = null;
        if (site != null) {
            acceptStatCode = site.getAcceptStatCode();
            charset = site.getCharset();
            headers = site.getHeaders();
        } else {
            acceptStatCode = Sets.newHashSet(200);
        }
        logger.info("downloading page {}", request.getUrl());
        CloseableHttpResponse httpResponse = null;
        int statusCode=0;
        try {
            HttpUriRequest httpUriRequest = getHttpUriRequest(request, site, headers);
            httpResponse = getHttpClient(request.getUrl(),site).execute(httpUriRequest);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            request.putExtra(Request.STATUS_CODE, statusCode);
            if (statusAccept(acceptStatCode, statusCode)) {
                Page page = handleResponse(request, charset, httpResponse, task);
                onSuccess(request);
                return page;
            } else {
                logger.warn("code error " + statusCode + "\t" + request.getUrl());
                return null;
            }
        } catch (IOException e) {
            logger.warn("download page " + request.getUrl() + " error", e);
            if (site.getCycleRetryTimes() > 0) {
                return addToCycleRetry(request, site);
            }
            onError(request);
            return null;
        } finally {
        	request.putExtra(Request.STATUS_CODE, statusCode);
            try {
                if (httpResponse != null) {
                    //ensure the connection is released back to pool
                    EntityUtils.consume(httpResponse.getEntity());
                }
            } catch (IOException e) {
                logger.warn("close response fail", e);
            }
        }
    }

    @Override
    protected void onError(Request request) {
    	logger.error("The url "+request.getUrl()+" has been retried too many times and can not be gotten, so here gives the error log !");
    };
    
    @Override
    public void setThread(int thread) {
        httpClientGenerator.setPoolSize(thread);
    }

    protected boolean statusAccept(Set<Integer> acceptStatCode, int statusCode) {
        return acceptStatCode.contains(statusCode);
    }

    protected HttpUriRequest getHttpUriRequest(Request request, Site site, Map<String, String> headers) {
        RequestBuilder requestBuilder = selectRequestMethod(request).setUri(request.getUrl());
        if (headers != null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(site.getTimeOut())
                .setSocketTimeout(site.getTimeOut())
                .setConnectTimeout(site.getTimeOut())
                .setCookieSpec(CookieSpecs.BEST_MATCH);
		if (site.getHttpProxyPool().isEnable()) {
			HttpHost host = site.getHttpProxyFromPool();
			requestConfigBuilder.setProxy(host);
			request.putExtra(Request.PROXY, host);
		}
        requestBuilder.setConfig(requestConfigBuilder.build());
        return requestBuilder.build();
    }

    protected RequestBuilder selectRequestMethod(Request request) {
        String method = request.getMethod();
        if (method == null || method.equalsIgnoreCase(HttpConstant.Method.GET)) {
            //default get
            return RequestBuilder.get();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.POST)) {
            RequestBuilder requestBuilder = RequestBuilder.post();
            NameValuePair[] nameValuePair = (NameValuePair[]) request.getExtra("nameValuePair");
            if (nameValuePair.length > 0) {
                requestBuilder.addParameters(nameValuePair);
            }
            return requestBuilder;
        } else if (method.equalsIgnoreCase(HttpConstant.Method.HEAD)) {
            return RequestBuilder.head();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.PUT)) {
            return RequestBuilder.put();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.DELETE)) {
            return RequestBuilder.delete();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.TRACE)) {
            return RequestBuilder.trace();
        }
        throw new IllegalArgumentException("Illegal HTTP Method " + method);
    }

    protected Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task) throws IOException {
        String content = getContent(charset, httpResponse);//getAjaxCotnent(request.getUrl());
        Page page = new Page();
        page.setRawText(content);
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        return page;
    }

    protected String getContent(String charset, HttpResponse httpResponse) throws IOException {
        if (charset == null) {
            byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
            String htmlCharset = getHtmlCharset(httpResponse, contentBytes);
            if (htmlCharset != null) {
                return new String(contentBytes, htmlCharset);
            } else {
                logger.warn("Charset autodetect failed, use {} as charset. Please specify charset in Site.setCharset()", Charset.defaultCharset());
                return new String(contentBytes);
            }
        } else {
            return IOUtils.toString(httpResponse.getEntity().getContent(), charset);
        }
    }

    protected String getHtmlCharset(HttpResponse httpResponse, byte[] contentBytes) throws IOException {
        String charset;
        // charset
        // get encoding in http header Content-Type
        String value = httpResponse.getEntity().getContentType().getValue();
        charset = UrlUtils.getCharset(value);
        if (StringUtils.isNotBlank(charset)) {
            logger.debug("Auto get charset: {}", charset);
            return charset;
        }
        // use default charset to decode first time
        Charset defaultCharset = Charset.defaultCharset();
        String content = new String(contentBytes, defaultCharset.name());
        // harset in meta
        if (StringUtils.isNotEmpty(content)) {
            Document document = Jsoup.parse(content);
            Elements links = document.select("meta");
            for (Element link : links) {
                // 4.01 <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                String metaContent = link.attr("content");
                String metaCharset = link.attr("charset");
                if (metaContent.indexOf("charset") != -1) {
                    metaContent = metaContent.substring(metaContent.indexOf("charset"), metaContent.length());
                    charset = metaContent.split("=")[1];
                    break;
                }
                // <meta charset="UTF-8" />
                else if (StringUtils.isNotEmpty(metaCharset)) {
                    charset = metaCharset;
                    break;
                }
            }
        }
        logger.debug("Auto get charset: {}", charset);
        //use tools as cpdetector for content decode
        return charset;
    }
}