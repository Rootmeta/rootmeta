package com.meta.community.handler;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meta.community.data.base.BaseDataModel;
import com.meta.community.data.base.TChain;
import com.meta.community.data.base.TLanguage;
import com.meta.community.data.base.TTag;
import com.meta.community.data.base.TToken;
import com.suomee.csp.lib.future.Future;
import com.suomee.csp.lib.server.Service;

public class BaseHandler {
	public static Future<String> loadLanguageList(String req) throws Exception {
		final Future<String> rspFuture = new Future<String>();
		
		List<TLanguage> languageList = BaseDataModel.instance().selectLanguageList();
		
		JSONArray jsonLanguageList = new JSONArray();
		for (TLanguage language : languageList) {
			jsonLanguageList.put(language.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("languageList", jsonLanguageList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadTagList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", "");
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TTag> tagList = BaseDataModel.instance().selectTagList(lang);
		
		JSONArray jsonTagList = new JSONArray();
		for (TTag tag : tagList) {
			jsonTagList.put(tag.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("tagList", jsonTagList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadChainList(String req) throws Exception {
		final Future<String> rspFuture = new Future<String>();
		
		List<TChain> chainList = BaseDataModel.instance().selectChainList();
		
		JSONArray jsonChainList = new JSONArray();
		for (TChain chain : chainList) {
			jsonChainList.put(chain.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("chainList", jsonChainList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadTokenList(String req) throws Exception {
		final Future<String> rspFuture = new Future<String>();
		
		List<TToken> tokenList = BaseDataModel.instance().selectTokenList();
		
		JSONArray jsonTokenList = new JSONArray();
		for (TToken token : tokenList) {
			jsonTokenList.put(token.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("tokenList", jsonTokenList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
}
