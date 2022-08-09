package com.meta.community.handler;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meta.community.data.EBoolean;
import com.meta.community.data.account.AccountDataModel;
import com.meta.community.data.account.EAliveStatus;
import com.meta.community.data.account.EUserStatus;
import com.meta.community.data.account.TUser;
import com.meta.community.data.account.TUserHistory;
import com.meta.community.data.account.TUserNft;
import com.meta.community.data.account.TUserToken;
import com.meta.community.data.base.BaseDataModel;
import com.meta.community.data.base.TChain;
import com.meta.community.data.base.TToken;
import com.meta.community.data.talk.TDomain;
import com.meta.community.data.talk.TDomainUser;
import com.meta.community.data.talk.TalkDataModel;
import com.meta.community.lang.AccountLang;
import com.meta.community.lang.BaseLang;
import com.meta.community.lang.ELang;
import com.meta.community.util.Web3Util;
import com.suomee.csp.lib.future.Future;
import com.suomee.csp.lib.lang.Lang;
import com.suomee.csp.lib.security.AES;
import com.suomee.csp.lib.server.Service;
import com.suomee.csp.lib.util.DateTimeUtil;
import com.suomee.csp.lib.util.IDUtil;
import com.suomee.csp.lib.util.StringUtil;

public class AccountHandler {
	public static Future<String> client(String req) throws Exception {
		final Future<String> rspFuture = new Future<String>();
		
		String client = StringUtil.guid();
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("client", client);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> unique(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final String address = jsonReqBody.optString("address", "");
		
		final Future<String> rspFuture = new Future<String>();
		
		if (StringUtil.isNullOrEmpty(address)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(AccountLang.INVALID_ADDRESS, lang)).toString()).complete();
		}
		
		TUser user = AccountDataModel.instance().selectUserByDeAddress(address);
		if (user == null) {
			String code = StringUtil.getRandomString(20);
			while (AccountDataModel.instance().selectUserByCode(code) != null) {
				code = StringUtil.getRandomString(20);
			}
			user = new TUser();
			user.setBuildTime(DateTimeUtil.seconds());
			user.setCode(code);
			user.setNickname("meta" + StringUtil.getRandomString(4));
			user.setDeAddress(address);
			user.setInited(EBoolean.NO.getValue());
			user.setUserStatus(EUserStatus.RAW.getValue());
			user.setAliveStatus(EAliveStatus.OFFLINE.getValue());
		}
		user.setDeNonce(StringUtil.getRandomString(20));
		AccountDataModel.instance().saveUser(user);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("address", user.getDeAddress());
		jsonRspBody.put("nonce", user.getDeNonce());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> sign(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final String address = jsonReqBody.optString("address", "");
		final String signature = jsonReqBody.optString("signature", "");
		
		final Future<String> rspFuture = new Future<String>();
		
		if (StringUtil.isNullOrEmpty(address)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(AccountLang.INVALID_ADDRESS, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUserByDeAddress(address);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(AccountLang.INVALID_ADDRESS, lang)).toString()).complete();
		}
		
		String uticket = null;
		String nonce = user.getDeNonce();
		if (Web3Util.verifySignature(address, nonce, signature)) {
			boolean online = user.getAliveStatus() == EAliveStatus.OFFLINE.getValue();
			user.setDeTicket(AES.getDefault().encrypt(user.getDeAddress() + "|" + user.getDeNonce()));
			user.setDeTicketTime(DateTimeUtil.seconds());
			user.setAliveStatus(EAliveStatus.ONLINE.getValue());
			user.setUserStatus(EUserStatus.NORMAL.getValue());
			AccountDataModel.instance().saveUser(user);
			
			if (online) {
				List<TDomain> domainList = TalkDataModel.instance().selectDomainListByUser(user.getUserId());
				for (TDomain domain : domainList) {
					domain.setOnlineCount(domain.getOnlineCount() + 1);
					if (domain.getOnlineCount() > domain.getMemberCount()) {
						domain.setOnlineCount(domain.getMemberCount());
					}
					TalkDataModel.instance().saveDomain(domain, null);
				}
				List<TDomainUser> domainUserList = TalkDataModel.instance().selectDomainUserListByUser(user.getUserId());
				Set<Long> domainIds = new HashSet<Long>();
				for (TDomainUser domainUser : domainUserList) {
					domainIds.add(domainUser.getDomainId());
				}
				Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
				for (TDomain domain : domains.values()) {
					domain.setOnlineCount(domain.getOnlineCount() + 1);
					if (domain.getOnlineCount() > domain.getMemberCount()) {
						domain.setOnlineCount(domain.getMemberCount());
					}
					TalkDataModel.instance().saveDomain(domain, null);
				}
				
				//TODO: push online
			}
			
			jsonReqHead.put("userId", user.getUserId());
			List<TDomain> rootDomainList = TalkDataModel.instance().selectDomainListByRoot();
			for (TDomain rootDomain : rootDomainList) {
				jsonReqBody.put("domainId", IDUtil.build(rootDomain.getDomainId()));
				TalkHandler.joinDomain(jsonReq.toString());
			}
			
			uticket = user.getDeTicket();
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("uticket", uticket != null ? uticket : JSONObject.NULL);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> swap(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final String address = jsonReqBody.optString("address", "");
		
		final Future<String> rspFuture = new Future<String>();
		
		if (StringUtil.isNullOrEmpty(address)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(AccountLang.INVALID_ADDRESS, lang)).toString()).complete();
		}
		TUser ouser = AccountDataModel.instance().selectUser(userId);
		if (ouser == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(AccountLang.INVALID_ADDRESS, lang)).toString()).complete();
		}
		ouser.setAliveStatus(EAliveStatus.OFFLINE.getValue());
		AccountDataModel.instance().saveUser(ouser);
		
		TUser user = AccountDataModel.instance().selectUserByDeAddress(address);
		if (user == null) {
			user = new TUser();
			user.setBuildTime(DateTimeUtil.seconds());
			user.setNickname("meta" + StringUtil.getRandomString(4));
			user.setDeAddress(address);
			user.setUserStatus(EUserStatus.NORMAL.getValue());
		}
		user.setAliveStatus(EAliveStatus.ONLINE.getValue());
		user.setDeNonce(StringUtil.getRandomString(20));
		user.setDeTicket(AES.getDefault().encrypt(StringUtil.getRandomString(64)));
		user.setDeTicketTime(DateTimeUtil.seconds());
		AccountDataModel.instance().saveUser(user);
		
		//TODO: push offline
		
		jsonReqHead.put("userId", user.getUserId());
		List<TDomain> rootDomainList = TalkDataModel.instance().selectDomainListByRoot();
		for (TDomain rootDomain : rootDomainList) {
			jsonReqBody.put("domainId", IDUtil.build(rootDomain.getDomainId()));
			TalkHandler.joinDomain(jsonReq.toString());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("uticket", user.getDeTicket());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> init(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final String nickname = jsonReqBody.optString("nickname", "").trim();
		final String avatar = jsonReqBody.optString("avatar", "").trim();
		final int nftavatar = jsonReqBody.optInt("nftavatar", EBoolean.NO.getValue());
		
		final Future<String> rspFuture = new Future<String>();
		
		if (StringUtil.isNullOrEmpty(nickname)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(AccountLang.NICKNAME_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		if (StringUtil.isNullOrEmpty(avatar)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(AccountLang.AVATAR_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, BaseLang.INVALID_PARAMETER).toString()).complete();
		}
		
		user.setNickname(nickname);
		user.setAvatar(avatar);
		user.setNftavatar(nftavatar);
		user.setInited(EBoolean.YES.getValue());
		AccountDataModel.instance().saveUser(user);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("user", user.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> edit(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final String nickname = jsonReqBody.optString("nickname", "").trim();
		final String avatar = jsonReqBody.optString("avatar", "").trim();
		final String banner = jsonReqBody.optString("banner", "").trim();
		final String color = jsonReqBody.optString("color", "").trim();
		final String email = jsonReqBody.optString("email", "").trim();
		final String about = jsonReqBody.optString("about", "").trim();
		final int nftavatar = jsonReqBody.optInt("nftavatar", EBoolean.NO.getValue());
		
		final Future<String> rspFuture = new Future<String>();
		
		if (StringUtil.isNullOrEmpty(nickname)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(AccountLang.NICKNAME_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, BaseLang.INVALID_PARAMETER).toString()).complete();
		}
		
		user.setNickname(nickname);
		user.setAvatar(avatar);
		user.setBanner(banner);
		user.setColor(color);
		user.setEmail(email);
		user.setAbout(about);
		user.setNftavatar(nftavatar);
		user.setInited(EBoolean.YES.getValue());
		AccountDataModel.instance().saveUser(user);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("user", user.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> quit(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(AccountLang.INVALID_ADDRESS, lang)).toString()).complete();
		}
		user.setDeTicket("");
		user.setDeTicketTime(0L);
		user.setAliveStatus(EAliveStatus.OFFLINE.getValue());
		AccountDataModel.instance().saveUser(user);
		
		List<TDomain> domainList = TalkDataModel.instance().selectDomainListByUser(user.getUserId());
		for (TDomain domain : domainList) {
			domain.setOnlineCount(domain.getOnlineCount() - 1);
			if (domain.getOnlineCount() < 0) {
				domain.setOnlineCount(0);
			}
			TalkDataModel.instance().saveDomain(domain, null);
		}
		List<TDomainUser> domainUserList = TalkDataModel.instance().selectDomainUserListByUser(user.getUserId());
		Set<Long> domainIds = new HashSet<Long>();
		for (TDomainUser domainUser : domainUserList) {
			domainIds.add(domainUser.getDomainId());
		}
		Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
		for (TDomain domain : domains.values()) {
			domain.setOnlineCount(domain.getOnlineCount() - 1);
			if (domain.getOnlineCount() < 0) {
				domain.setOnlineCount(0);
			}
			TalkDataModel.instance().saveDomain(domain, null);
		}
		
		//TODO: push offline
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadUserList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final String nickname = jsonReqBody.optString("nickname", "").trim();
		final int start = jsonReqBody.optInt("start", 0);
		final int limit = jsonReqBody.optInt("limit", 20);
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TUser> userList = AccountDataModel.instance().selectUserList(nickname, start, limit);
		
		JSONArray jsonUserList = new JSONArray();
		for (TUser user : userList) {
			jsonUserList.put(user.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("userList", jsonUserList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadUser(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final boolean withAsset = jsonReqBody.optBoolean("withAsset", false);
		
		final Future<String> rspFuture = new Future<String>();
		
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user != null) {
			boolean online = user.getAliveStatus() == EAliveStatus.OFFLINE.getValue();
			if (online) {
				user.setAliveStatus(EAliveStatus.ONLINE.getValue());
				AccountDataModel.instance().saveUser(user);
				
				List<TDomain> domainList = TalkDataModel.instance().selectDomainListByUser(user.getUserId());
				for (TDomain domain : domainList) {
					domain.setOnlineCount(domain.getOnlineCount() + 1);
					if (domain.getOnlineCount() > domain.getMemberCount()) {
						domain.setOnlineCount(domain.getMemberCount());
					}
					TalkDataModel.instance().saveDomain(domain, null);
				}
				List<TDomainUser> domainUserList = TalkDataModel.instance().selectDomainUserListByUser(user.getUserId());
				Set<Long> domainIds = new HashSet<Long>();
				for (TDomainUser domainUser : domainUserList) {
					domainIds.add(domainUser.getDomainId());
				}
				Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
				for (TDomain domain : domains.values()) {
					domain.setOnlineCount(domain.getOnlineCount() + 1);
					if (domain.getOnlineCount() > domain.getMemberCount()) {
						domain.setOnlineCount(domain.getMemberCount());
					}
					TalkDataModel.instance().saveDomain(domain, null);
				}
				
				//TODO: push online
			}
		}
		List<TUserToken> userTokenList = null;
		List<TUserNft> userNftList = null;
		List<TUserHistory> userHistoryList = null;
		Map<Long, TChain> chains = null;
		Map<Long, TToken> tokens = null;
		if (user != null && withAsset) {
			userTokenList = AccountDataModel.instance().selectUserTokenListByUser(userId);
			userNftList = AccountDataModel.instance().selectUserNftListByUser(userId);
			userHistoryList = AccountDataModel.instance().selectUserHistoryListByUser(userId, 0, 50);
		}
		Set<Long> chainIds = new HashSet<Long>();
		Set<Long> tokenIds = new HashSet<Long>();
		if (userTokenList != null) {
			for (TUserToken userToken : userTokenList) {
				chainIds.add(userToken.getChainId());
				tokenIds.add(userToken.getTokenId());
			}
		}
		if (userNftList != null) {
			for (TUserNft userNft : userNftList) {
				chainIds.add(userNft.getChainId());
			}
		}
		if (userHistoryList != null) {
			for (TUserHistory userHistory : userHistoryList) {
				chainIds.add(userHistory.getChainId());
				tokenIds.add(userHistory.getTokenId());
				tokenIds.add(userHistory.getGasTokenId());
			}
		}
		chains = BaseDataModel.instance().selectChains(chainIds);
		tokens = BaseDataModel.instance().selectTokens(tokenIds);
		
		JSONArray jsonUserTokenList = new JSONArray();
		if (userTokenList != null) {
			for (TUserToken userToken : userTokenList) {
				jsonUserTokenList.put(userToken.toJsonObject());
			}
		}
		JSONArray jsonUserNftList = new JSONArray();
		if (userNftList != null) {
			for (TUserNft userNft : userNftList) {
				jsonUserNftList.put(userNft.toJsonObject());
			}
		}
		JSONArray jsonUserHistoryList = new JSONArray();
		if (userHistoryList != null) {
			for (TUserHistory userHistory : userHistoryList) {
				jsonUserHistoryList.put(userHistory.toJsonObject());
			}
		}
		JSONArray jsonChains = new JSONArray();
		if (chains != null) {
			for (TChain chain : chains.values()) {
				jsonChains.put(chain.toJsonObject());
			}
		}
		JSONArray jsonTokens = new JSONArray();
		if (tokens != null) {
			for (TToken token : tokens.values()) {
				jsonTokens.put(token.toJsonObject());
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("user", user != null ? user.toJsonObject() : JSONObject.NULL);
		jsonRspBody.put("userTokenList", jsonUserTokenList);
		jsonRspBody.put("userNftList", jsonUserNftList);
		jsonRspBody.put("userHistoryList", jsonUserHistoryList);
		jsonRspBody.put("chains", jsonChains);
		jsonRspBody.put("tokens", jsonTokens);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadUserInfo(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long userId = IDUtil.parse(jsonReqBody.optString("userId", ""));
		final boolean withAsset = jsonReqBody.optBoolean("withAsset", false);
		
		final Future<String> rspFuture = new Future<String>();
		
		TUser user = AccountDataModel.instance().selectUser(userId);
		List<TUserToken> userTokenList = null;
		List<TUserNft> userNftList = null;
		List<TUserHistory> userHistoryList = null;
		Map<Long, TChain> chains = null;
		Map<Long, TToken> tokens = null;
		if (user != null && withAsset) {
			userTokenList = AccountDataModel.instance().selectUserTokenListByUser(userId);
			userNftList = AccountDataModel.instance().selectUserNftListByUser(userId);
			userHistoryList = AccountDataModel.instance().selectUserHistoryListByUser(userId, 0, 50);
		}
		Set<Long> chainIds = new HashSet<Long>();
		Set<Long> tokenIds = new HashSet<Long>();
		if (userTokenList != null) {
			for (TUserToken userToken : userTokenList) {
				chainIds.add(userToken.getChainId());
				tokenIds.add(userToken.getTokenId());
			}
		}
		if (userNftList != null) {
			for (TUserNft userNft : userNftList) {
				chainIds.add(userNft.getChainId());
			}
		}
		if (userHistoryList != null) {
			for (TUserHistory userHistory : userHistoryList) {
				chainIds.add(userHistory.getChainId());
				tokenIds.add(userHistory.getTokenId());
				tokenIds.add(userHistory.getGasTokenId());
			}
		}
		chains = BaseDataModel.instance().selectChains(chainIds);
		tokens = BaseDataModel.instance().selectTokens(tokenIds);
		
		JSONArray jsonUserTokenList = new JSONArray();
		if (userTokenList != null) {
			for (TUserToken userToken : userTokenList) {
				jsonUserTokenList.put(userToken.toJsonObject());
			}
		}
		JSONArray jsonUserNftList = new JSONArray();
		if (userNftList != null) {
			for (TUserNft userNft : userNftList) {
				jsonUserNftList.put(userNft.toJsonObject());
			}
		}
		JSONArray jsonUserHistoryList = new JSONArray();
		if (userHistoryList != null) {
			for (TUserHistory userHistory : userHistoryList) {
				jsonUserHistoryList.put(userHistory.toJsonObject());
			}
		}
		JSONArray jsonChains = new JSONArray();
		if (chains != null) {
			for (TChain chain : chains.values()) {
				jsonChains.put(chain.toJsonObject());
			}
		}
		JSONArray jsonTokens = new JSONArray();
		if (tokens != null) {
			for (TToken token : tokens.values()) {
				jsonTokens.put(token.toJsonObject());
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("user", user != null ? user.toJsonObject() : JSONObject.NULL);
		jsonRspBody.put("userTokenList", jsonUserTokenList);
		jsonRspBody.put("userNftList", jsonUserNftList);
		jsonRspBody.put("userHistoryList", jsonUserHistoryList);
		jsonRspBody.put("chains", jsonChains);
		jsonRspBody.put("tokens", jsonTokens);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadUserNftList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TUserNft> userNftList = AccountDataModel.instance().selectUserNftListByUser(userId);
		Set<Long> chainIds = new HashSet<Long>();
		Set<Long> tokenIds = new HashSet<Long>();
		for (TUserNft userNft : userNftList) {
			chainIds.add(userNft.getChainId());
		}
		Map<Long, TChain> chains = BaseDataModel.instance().selectChains(chainIds);
		Map<Long, TToken> tokens = BaseDataModel.instance().selectTokens(tokenIds);
		
		JSONArray jsonUserNftList = new JSONArray();
		for (TUserNft userNft : userNftList) {
			jsonUserNftList.put(userNft.toJsonObject());
		}
		JSONArray jsonChains = new JSONArray();
		if (chains != null) {
			for (TChain chain : chains.values()) {
				jsonChains.put(chain.toJsonObject());
			}
		}
		JSONArray jsonTokens = new JSONArray();
		if (tokens != null) {
			for (TToken token : tokens.values()) {
				jsonTokens.put(token.toJsonObject());
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("userNftList", jsonUserNftList);
		jsonRspBody.put("chains", jsonChains);
		jsonRspBody.put("tokens", jsonTokens);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
}
