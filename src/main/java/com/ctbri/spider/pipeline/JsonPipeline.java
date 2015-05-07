package com.ctbri.spider.pipeline;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.codecraft.webmagic.ResultItems;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.pipeline.Pipeline;
import us.codecraft.webmagic.utils.FilePersistentBase;

import com.alibaba.fastjson.JSON;

public class JsonPipeline extends FilePersistentBase implements Pipeline {
	private Logger logger = LoggerFactory.getLogger(getClass());

	/**
	 * ctbri pipe with default path "/data/webmagic/" and fix the file not exists bug under windows
	 */
	public JsonPipeline() {
		setPath("/data/webmagic");
	}

	public JsonPipeline(String path) {
		setPath(path);
	}

	@Override
	public void process(ResultItems resultItems, Task task) {
		String path = this.path + "/" + task.getUUID() + "/";
		File tmp = new File(path);
		if(!tmp.exists()) tmp.mkdir();
		tmp = getFile(path
				+ DigestUtils.md5Hex(resultItems.getRequest()
						.getUrl()) + ".json");
		try {
			tmp.createNewFile();
			PrintWriter printWriter = new PrintWriter(new FileWriter(tmp));
			printWriter.write(JSON.toJSONString(resultItems.getAll(),true));
			printWriter.close();
		} catch (IOException e) {
			logger.warn("write file error", e);
		}
	}
}
