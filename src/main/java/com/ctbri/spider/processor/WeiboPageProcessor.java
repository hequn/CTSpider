package com.ctbri.spider.processor;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.processor.PageProcessor;

import com.ctbri.spider.cache.CacheHandler;
import com.ctbri.spider.cache.SystemConstants;
import com.ctbri.spider.downloader.OriginalDownloader;
import com.ctbri.spider.downloader.WebAjaxDownloader;
import com.ctbri.spider.entry.SpiderConfig;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.model.EntityContainer;
import com.ctbri.spider.puter.BadPageReloader;
import com.ctbri.spider.puter.PageBatchWriter;

public class WeiboPageProcessor implements PageProcessor,SpiderConfig,PageBatchWriter,BadPageReloader {
	
	//定义logger，用于记录日志
	private Logger logger = LoggerFactory.getLogger(getClass());
	//定义spider，null即可
	private Spider spider = null;
	//定义downloader，包括spider拿来定义在此的原因是暴露给process方法使用PageProcessor接口就是为此而设计
	private Downloader downloader = new WebAjaxDownloader();

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等，header很重要，不同的业务逻辑需要的也不同，差异性极大
    private Site site = Site.me()
    		.setRetryTimes(6)
    		.setSleepTime(200)
    		.setCycleRetryTimes(6)
    		.setUserAgent("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.130 Safari/537.36")
    		.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
    		.addHeader("Accept-Encoding", "gzip, deflate, sdch")
    		.addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    		.addHeader("Cache-Control", "no-cache")
    		.addHeader("Connection", "keep-alive")
    		.addHeader("Cookie", "SINAGLOBAL=8926377859897.912.1427704806534; YF-V5-G0=35ff6d315d1a536c0891f71721feb16e; _s_tentry=www.chinaavl.com; Apache=4438256893772.632.1436938597057; ULV=1436938597064:13:1:1:4438256893772.632.1436938597057:1434436321904; YF-Page-G0=f0e89c46e7ea678e9f91d029ec552e92; likeDot=1; YF-Ugrow-G0=5b31332af1361e117ff29bb32e4d8439; myuid=1793002255; login_sid_t=2d0d7a27c18dee32e9ba207273b6109f; WBtopGlobal_register_version=dac39c945742325f; SUS=SID-1793002255-1436941715-JA-hnx2a-7dbe7ded7ea37cc091f7103ef56e3e9b; SUE=es%3D3689125cc9fb6c16405680351c071370%26ev%3Dv1%26es2%3De01446ed9c28b016a3c3cdf617b7d969%26rs0%3DOcRqxkZeyp32W9xJnjA6enKHCKzWh9aZo5bVwO5tSqN4JKJGyaTBhVijx0NqWFK2Gsuxmg8u27IHY3UsIuZ4aNXXy2KIt%252BqubSVQKgPrPKaLjodXSAjVvRMVJZCep0t6aebU1Wrz2jUyg2IGtRsMv9TGhyxw17S8MO%252FdZ27kP4c%253D%26rv%3D0; SUP=cv%3D1%26bt%3D1436941715%26et%3D1437028115%26d%3Dc909%26i%3D3e9b%26us%3D1%26vf%3D0%26vt%3D0%26ac%3D0%26st%3D0%26uid%3D1793002255%26name%3Dhequn0815%2540163.com%26nick%3Dhequn0815%26fmp%3D%26lcp%3D2011-12-22%252011%253A55%253A31; SUB=_2A254oY3DDeTxGedJ4lER8CzOzjmIHXVb1vgLrDV8PUNbuNAMLWvGkW9aXag5ctdLNIjiFH2AKHqEP_64AA..; SUBP=0033WrSXqPxfM725Ws9jqgMF55529P9D9W55w.g0Kq4ddGPAsF6Z7l2V5JpX5K2t; SUHB=0d4PFenR_VgCXD; ALF=1437546572; SSOLoginState=1436941715; un=hequn0815@163.com; UOR=www.testwo.com,widget.weibo.com,www.baidu.com")
    		.addHeader("Host","weibo.com");
    
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑，JD的专属process逻辑
    /**
     * 需要强调的是当爬虫被封锁时，有几种方式。
     * <p>1、异常情况。异常情况的url在此架构下会回到redis中，放置于末尾处理。</p>
     * <p>2、页面需要输入验证码或者其他返回页面。需要特异性识别页面并使各个线程休眠适当时间
     * ,即在判断页面是封锁页面时使用
     * 			spider.addUrl(page.getUrl().get());//返回当前问题url到redis供其他客户端爬取
     * 			Thread.sleep(时间);//同时睡眠线程
     * 处理此客户端。</p>
     * 
     * process the page, extract urls to fetch, extract the data and store
     *
     * @param page
     */
    @Override
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
		try {
			String pUrl = page.getUrl().get();
			int ind = pUrl.indexOf(".html");
			String productId = pUrl.substring(19,20);
			page.putField("productKey", productId);
			String productName = page.getHtml().xpath("//div[@class='breadcrumb']").$("a[clstag$=mbNav-4]").xpath("/a/text()").get();
			page.putField("productName", productName);
			String productPrice = page.getHtml().xpath("//strong[@id='jd-price']/text()").get().substring(1);
			page.putField("productPrice", productPrice);

			if(productName == null || "".equals(productName)) logger.info("No brand info : "+pUrl);
		} catch (Exception e) {
			logger.error("Part or Null info page : "+page.getUrl().get()+" Info: "+e.getMessage());
		}
    }
	
	@Override
	public void initBatchWritePos(String saveLocation) throws Exception{
		File file = new File(saveLocation);
		if(!file.exists()) file.mkdir();
		File fileFull = new File(saveLocation+"/PageItems");
		if(!fileFull.exists()) fileFull.mkdir();
	}
	
	@Override
	public EntityContainer pageBatchWrite(int maxPageCount,String saveLocation) throws Exception{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		String dateTime = sdf.format(new Date());
		String keyID = UUID.randomUUID().toString();
		String tmpFull = saveLocation+"/FullParams/"+keyID;
		PrintWriter printWriter1 = new PrintWriter(new File(tmpFull),"gbk");
		EntityContainer ec = new EntityContainer();
		ec.setDomainKey(site.getDomain());
		ec.getParamsOrder().add("productString");
		ec.getParamsOrder().add("productKey");
		ec.getParamsOrder().add("productName");
		ec.getParamsOrder().add("productPrice");
		ec.getParamsOrder().add("dateTime");
		for(int i = 0; i< maxPageCount ; i++){
			Entity e = CacheHandler.resultLines.poll();
			if(e == null) break;
			String key = (String) e.getItemParams().get("productKey");
			String pName = (String) e.getItemParams().get("productName");
			String pPrice = (String) e.getItemParams().get("productPrice");

			if(pPrice == null) e.getItemParams().put("productPrice","-1");
			e.getItemParams().put("productString", "1101");
			e.getItemParams().put("dateTime", dateTime);
			ec.getEntities().add(e);
		}
		printWriter1.close();
		
		return ec;
	}
	
	@Override
	public File initReloadBackPos(String saveLocation) throws Exception {
		return null;
	}
	
	@Override
	public File[] getReloadFiles(String saveLocation) throws Exception{
		return null;
	}

	@Override
	public String pageReloadUrl(String oneLine) throws Exception{
		return null;		
	}
	//上面三个重写的方法逻辑和内容均不需要改变
	@Override
    public Site getSite() {
        return site;
    }

	@Override
	public Downloader getDownloader() {
		return downloader;
	}

	@Override
	public void setSpider(Spider spider) {
		this.spider = spider;
	}
}