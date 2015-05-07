package com.ctbri.spider.processor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.JMException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.monitor.SpiderMonitor;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.RedisScheduler;
import us.codecraft.webmagic.selector.Selectable;

import com.ctbri.spider.downloader.WebAjaxDownloader;
import com.ctbri.spider.model.Entity;
import com.ctbri.spider.pipeline.JsonPipeline;

public class CarHomePageProcessor implements PageProcessor {

    // 部分一：抓取网站的相关配置，包括编码、抓取间隔、重试次数等
    private Site site = Site.me().setRetryTimes(3).setSleepTime(200).setCycleRetryTimes(3);
    private String configUrl = "http://car.autohome.com.cn";
    private Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    // process是定制爬虫逻辑的核心接口，在这里编写抽取逻辑
    public void process(Page page) {
        // 部分二：定义如何抽取页面信息，并保存下来
        //page.putField("author", page.getUrl().regex("https://github\\.com/(\\w+)/.*").toString());
    	List<Entity> result = new ArrayList<>();
		try {
			List<Selectable> topDiv = page.getHtml().xpath("//div[@id='content']").nodes();
			List<Selectable> confDiv = page.getHtml().xpath("//div[@id='config_data']").nodes();
			if(topDiv.size() == 0 || confDiv.size()==0) {
				addNewCrawlingUrls(page);
				throw new Exception("Not leaf page: "+page.getUrl().get());
			}else {
				Selectable contentDiv = topDiv.get(0);
				Selectable paramsDiv = confDiv.get(0);
				
				List<String> headers = contentDiv.xpath("//div[@class='carbox']/div/a/text()").all();
				
				List<Selectable> paramsNodes = paramsDiv.$("tr[id^=tr_]").nodes();
				Map<String,List<String>> valueHolders = new LinkedHashMap<>();

				//取得页面配置参数信息，包括参数名称
				for (Selectable sel : paramsNodes) {
					String paramName = sel.xpath("//th/div/a/text()").get();
					if(paramName==null || "".equals(paramName)) paramName=sel.xpath("//th/div/text()").get();
					
					List<String> params =  sel.xpath("//td/div/a/text()").all();
					if(params.size()==0 || params.get(0) == null || "".equals(params.get(0))) params=sel.xpath("//td/div/text()").all();
					if(params.size()!=0 && params.get(0) != null && !"".equals(params.get(0)))
						valueHolders.put(paramName, params);
					else if(sel.xpath("//td/div/ul/li").nodes().size()!=0){
						List<Selectable> colorNodes = sel.xpath("//td/div").nodes();
						params = new ArrayList<>();
						for (Selectable selOuter : colorNodes) {
							List<String> spansNode = selOuter.xpath("//ul/li/*/@title").all();
							if(spansNode.size() == 0) params.add("");
							else params.add(spansNode.toString());						
						}

						valueHolders.put(paramName, params);
					}		
				}
				//生成实体
				for(int i = 0; i < headers.size() ; i++){
					Entity e = new Entity();
					Map<String,String> tmp = new LinkedHashMap<>();
					e.setItemName(headers.get(i));
					for(Entry<String, List<String>> entry:valueHolders.entrySet()){
						tmp.put(entry.getKey(), entry.getValue().get(i));
					}
					e.setItemParams(tmp);
					result.add(e);
				}
				page.putField("product", result);
			}
		} catch (Exception e) {
			page.setSkip(true);
			logger.info("Not a leaf or empyt page! "+e.getMessage());
			if(e.getMessage().contains("Index")) page.addTargetRequest(page.getUrl().get());
		}
    }
    
    private void addNewCrawlingUrls(Page page) {
    	// 部分三：从页面发现后续的url地址来抓取
    	page.addTargetRequests(page.getHtml().links().regex("(http://www.autohome.com.cn/[0-9]+/price.html.*)").all());
    	//抓取汽车之家spec部分
    	List<String> combinedUrl = new ArrayList<>();
    	for(String tmp : page.getHtml().links().regex("(/spec/[0-9]+/config.html)").all()){
    		String[] split = tmp.split("/");
    		combinedUrl.add(configUrl+"/config/spec/"+split[2]+".html");
    	}
    	if(combinedUrl.size() != 0)	page.addTargetRequests(combinedUrl);
	}

	@Override
    public Site getSite() {
        return site;
    }
    
    public static void main(String[] args) {
    	Spider a = Spider.create(new CarHomePageProcessor())
        //从https://github.com/code4craft开始抓    
        .addUrl("http://www.autohome.com.cn/grade/carhtml/a.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/b.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/c.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/d.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/e.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/f.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/g.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/h.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/i.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/j.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/k.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/l.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/m.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/n.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/o.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/p.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/q.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/r.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/s.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/t.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/u.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/v.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/w.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/x.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/y.html")
        .addUrl("http://www.autohome.com.cn/grade/carhtml/z.html")
        //设置Scheduler，使用Redis来管理URL队列
        .setScheduler(new RedisScheduler(new JedisPool(new JedisPoolConfig(),"192.168.56.128", 6379)))
        //尝试使用SeleniumDownloader
        .setDownloader(new WebAjaxDownloader())
        //设置Pipeline，将结果以json方式保存到文件
        .addPipeline(new JsonPipeline("D:\\data\\webmagic"))
        //开启5个线程同时执行
        .thread(5);
    	//启动监视器，cmd jconsole.exe查看状态 jmx
    	try {
			SpiderMonitor.instance().register(a);
		} catch (JMException e) {
			e.printStackTrace();
		}
        //启动爬虫
        a.run();
    }
}