package com.meta.community.handler;

import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.meta.community.data.EBoolean;
import com.meta.community.data.account.AccountDataModel;
import com.meta.community.data.account.EAliveStatus;
import com.meta.community.data.account.TUser;
import com.meta.community.data.account.TUserNft;
import com.meta.community.data.base.BaseDataModel;
import com.meta.community.data.base.TTag;
import com.meta.community.data.base.TToken;
import com.meta.community.data.chat.ChatDataModel;
import com.meta.community.data.chat.EChatMessageType;
import com.meta.community.data.chat.TWhisper;
import com.meta.community.data.chat.TWhisperMessage;
import com.meta.community.data.chat.TWhisperMessageNotice;
import com.meta.community.data.chat.TWhisperUser;
import com.meta.community.data.talk.EDomainPrivilege;
import com.meta.community.data.talk.EDomainVisibility;
import com.meta.community.data.talk.ETalkMessageType;
import com.meta.community.data.talk.ETeamLimitType;
import com.meta.community.data.talk.TCategory;
import com.meta.community.data.talk.TDomain;
import com.meta.community.data.talk.TDomainNft;
import com.meta.community.data.talk.TDomainRole;
import com.meta.community.data.talk.TDomainRoleUser;
import com.meta.community.data.talk.TDomainTag;
import com.meta.community.data.talk.TDomainUser;
import com.meta.community.data.talk.TTeam;
import com.meta.community.data.talk.TTeamLimit;
import com.meta.community.data.talk.TTeamMessage;
import com.meta.community.data.talk.TTeamMessageNotice;
import com.meta.community.data.talk.TTeamMessageReaction;
import com.meta.community.data.talk.TalkDataModel;
import com.meta.community.lang.BaseLang;
import com.meta.community.lang.ELang;
import com.meta.community.lang.TalkLang;
import com.meta.community.util.ImageUtil;
import com.meta.push.proto.EDataType;
import com.meta.push.proto.PushData;
import com.meta.push.proto.PushReq;
import com.meta.push.proto.PushRsp;
import com.meta.push.proxy.PushServiceProxy;
import com.suomee.csp.lib.client.ProxyFactory;
import com.suomee.csp.lib.future.Future;
import com.suomee.csp.lib.future.FutureHandler;
import com.suomee.csp.lib.lang.Lang;
import com.suomee.csp.lib.log.Logger;
import com.suomee.csp.lib.proto.CspException;
import com.suomee.csp.lib.proto.EResultCode;
import com.suomee.csp.lib.server.Service;
import com.suomee.csp.lib.util.DateTimeUtil;
import com.suomee.csp.lib.util.IDUtil;
import com.suomee.csp.lib.util.RandomUtil;
import com.suomee.csp.lib.util.StringUtil;

public class TalkHandler {
	public static Future<String> exploreDomainList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long tagId = IDUtil.parse(jsonReqBody.optString("tagId", ""));
		final String text = jsonReqBody.optString("text", "").trim();
		final int start = jsonReqBody.optInt("start", 0);
		final int limit = jsonReqBody.optInt("limit", 20);
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TDomain> domainList = TalkDataModel.instance().selectDomainListByText(tagId, text, EDomainVisibility.PUBLIC.getValue(), start, limit);
		
		List<TDomain> rootDomainList = new LinkedList<TDomain>();
		for (int i = 0; i < domainList.size(); i++) {
			TDomain domain = domainList.get(i);
			if (domain.getRoot() == EBoolean.YES.getValue()) {
				domainList.remove(i);
				rootDomainList.add(0, domain);
				i--;
			}
		}
		for (TDomain rootDomain : rootDomainList) {
			domainList.add(0, rootDomain);
		}
		
		JSONArray jsonDomainList = new JSONArray();
		for (TDomain domain : domainList) {
			jsonDomainList.put(domain.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domainList", jsonDomainList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> inviteDomainByFriend(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		final long inviteDomainId = IDUtil.parse(jsonReqBody.optString("inviteDomainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TUser friend = AccountDataModel.instance().selectUser(friendId);
		if (friend == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain inviteDomain = TalkDataModel.instance().selectDomain(inviteDomainId);
		if (inviteDomain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (inviteDomain.getUserId() != userId && !checkDomainPrivilege(inviteDomainId, userId, EDomainPrivilege.INVITE_MEMBER.getValue())) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TWhisper whisper = ChatDataModel.instance().selectWhisperByUserMember(userId, friendId);
		if (whisper == null) {
			whisper = new TWhisper();
			whisper.setUserId(userId);
			whisper.setMemberId(friendId);
			whisper.setBuildTime(DateTimeUtil.seconds());
			whisper.setMemberCount(2);
			whisper.setName("");
			
			List<TWhisperUser> whisperUserList = new LinkedList<TWhisperUser>();
			TWhisperUser whisperUser = new TWhisperUser();
			whisperUser.setUserId(userId);
			whisperUser.setBuildTime(DateTimeUtil.seconds());
			whisperUserList.add(whisperUser);
			whisperUser = new TWhisperUser();
			whisperUser.setUserId(friendId);
			whisperUser.setBuildTime(DateTimeUtil.seconds());
			whisperUserList.add(whisperUser);
			
			ChatDataModel.instance().saveWhisper(whisper, whisperUserList);
		}
		
		TWhisperMessage message = new TWhisperMessage();
		message.setWhisperId(whisper.getWhisperId());
		message.setUserId(userId);
		message.setClient("");
		message.setSeries("");
		message.setMessageTime(DateTimeUtil.seconds());
		message.setMessageType(EChatMessageType.DOMAIN_INVITE.getValue());
		message.setTextContent("");
		message.setImageUrl("");
		message.setAudioUrl("");
		message.setVideoUrl("");
		message.setFileUrl("");
		message.setInviteDomainId(inviteDomainId);
		ChatDataModel.instance().saveWhisperMessage(message);
		
		Map<Long, TWhisperMessageNotice> notices = ChatDataModel.instance().selectWhisperMessageNoticesByWhisper(whisper.getWhisperId());
		TWhisperMessageNotice notice = notices.get(friendId);
		if (notice == null) {
			notice = new TWhisperMessageNotice();
			notice.setWhisperId(whisper.getWhisperId());
			notice.setUserId(friendId);
			notice.setCount(1);
			ChatDataModel.instance().saveWhisperMessageNotice(notice);
			TWhisperMessageNotice eNotice = ChatDataModel.instance().selectWhisperMessageNoticeByWhisperUser(whisper.getWhisperId(), friendId);
			if (eNotice != null && eNotice.getNoticeId() != notice.getNoticeId()) {
				ChatDataModel.instance().updateWhisperMessageNotice(eNotice, 1, notice);
			}
		}
		else {
			ChatDataModel.instance().updateWhisperMessageNotice(notice, 1, null);
		}
		Set<Long> accounts = new HashSet<Long>();
		accounts.add(friendId);
		PushData data = new PushData();
		data.setDataType(EDataType.CHAT_MESSAGE.getValue());
		data.setDataContent(message.toJsonObject().toString());
		ProxyFactory.getProxy(PushServiceProxy.class).push(new PushReq().setAccounts(accounts).setData(data)).ready(new FutureHandler<PushRsp>() {
			@Override
			public void complete(Future<PushRsp> pushRspFuture) throws Exception {
				if (!pushRspFuture.isSuccess()) {
					Logger.getLogger("error").error("TalkHandler.inviteDomainByFriend push business exception.", pushRspFuture.getException());
					return;
				}
				PushRsp pushRsp = pushRspFuture.getResult();
				if (pushRsp.getCode() != EResultCode.OK) {
					Logger.getLogger("error").error("TalkHandler.inviteDomainByFriend push failed. code=" + pushRsp.getCode() + ", message=" + pushRsp.getMessage());
				}
			}
			@Override
			public void exception(CspException e) {
				Logger.getLogger("error").error("TalkHandler.inviteDomainByFriend push invoke exception.", e);
			}
		});
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> inviteDomainByWhisper(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		final long inviteDomainId = IDUtil.parse(jsonReqBody.optString("inviteDomainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisper whisper = ChatDataModel.instance().selectWhisper(whisperId);
		if (whisper == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain inviteDomain = TalkDataModel.instance().selectDomain(inviteDomainId);
		if (inviteDomain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (inviteDomain.getUserId() != userId && !checkDomainPrivilege(inviteDomainId, userId, EDomainPrivilege.INVITE_MEMBER.getValue())) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TWhisperMessage message = new TWhisperMessage();
		message.setWhisperId(whisper.getWhisperId());
		message.setUserId(userId);
		message.setClient("");
		message.setSeries("");
		message.setMessageTime(DateTimeUtil.seconds());
		message.setMessageType(EChatMessageType.DOMAIN_INVITE.getValue());
		message.setTextContent("");
		message.setImageUrl("");
		message.setAudioUrl("");
		message.setVideoUrl("");
		message.setFileUrl("");
		message.setInviteDomainId(inviteDomainId);
		ChatDataModel.instance().saveWhisperMessage(message);
		
		Map<Long, TWhisperMessageNotice> notices = ChatDataModel.instance().selectWhisperMessageNoticesByWhisper(whisper.getWhisperId());
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByWhisper(whisper.getWhisperId());
		for (TWhisperUser whisperUser : whisperUserList) {
			TWhisperMessageNotice notice = notices.get(whisperUser.getUserId());
			if (notice == null) {
				notice = new TWhisperMessageNotice();
				notice.setWhisperId(whisper.getWhisperId());
				notice.setUserId(whisperUser.getUserId());
				notice.setCount(1);
				ChatDataModel.instance().saveWhisperMessageNotice(notice);
				TWhisperMessageNotice eNotice = ChatDataModel.instance().selectWhisperMessageNoticeByWhisperUser(whisper.getWhisperId(), whisperUser.getUserId());
				if (eNotice != null && eNotice.getNoticeId() != notice.getNoticeId()) {
					ChatDataModel.instance().updateWhisperMessageNotice(eNotice, 1, notice);
				}
			}
			else {
				ChatDataModel.instance().updateWhisperMessageNotice(notice, 1, null);
			}
		}
		Set<Long> accounts = new HashSet<Long>();
		for (TWhisperUser whisperUser : whisperUserList) {
			accounts.add(whisperUser.getUserId());
		}
		PushData data = new PushData();
		data.setDataType(EDataType.CHAT_MESSAGE.getValue());
		data.setDataContent(message.toJsonObject().toString());
		ProxyFactory.getProxy(PushServiceProxy.class).push(new PushReq().setAccounts(accounts).setData(data)).ready(new FutureHandler<PushRsp>() {
			@Override
			public void complete(Future<PushRsp> pushRspFuture) throws Exception {
				if (!pushRspFuture.isSuccess()) {
					Logger.getLogger("error").error("TalkHandler.inviteDomainByWhisper push business exception.", pushRspFuture.getException());
					return;
				}
				PushRsp pushRsp = pushRspFuture.getResult();
				if (pushRsp.getCode() != EResultCode.OK) {
					Logger.getLogger("error").error("TalkHandler.inviteDomainByWhisper push failed. code=" + pushRsp.getCode() + ", message=" + pushRsp.getMessage());
				}
			}
			@Override
			public void exception(CspException e) {
				Logger.getLogger("error").error("TalkHandler.inviteDomainByWhisper push invoke exception.", e);
			}
		});
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> inviteDomainByDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final long inviteDomainId = IDUtil.parse(jsonReqBody.optString("inviteDomainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TTeam team = null;
		List<TTeam> teamList = TalkDataModel.instance().selectTeamListByDomain(domainId);
		if (teamList.size() > 0) {
			team = teamList.get(0);
		}
		if (team == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain inviteDomain = TalkDataModel.instance().selectDomain(inviteDomainId);
		if (inviteDomain == null) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (inviteDomain.getUserId() != userId && !checkDomainPrivilege(inviteDomainId, userId, EDomainPrivilege.INVITE_MEMBER.getValue())) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TTeamMessage message = new TTeamMessage();
		message.setDomainId(domain.getDomainId());
		message.setTeamId(team.getTeamId());
		message.setUserId(userId);
		message.setClient("");
		message.setSeries("");
		message.setMessageTime(DateTimeUtil.seconds());
		message.setMessageType(EChatMessageType.DOMAIN_INVITE.getValue());
		message.setTextContent("");
		message.setImageUrl("");
		message.setAudioUrl("");
		message.setVideoUrl("");
		message.setFileUrl("");
		message.setInviteDomainId(inviteDomainId);
		TalkDataModel.instance().saveTeamMessage(message);
		
		Map<Long, TTeamMessageNotice> notices = TalkDataModel.instance().selectTeamMessageNoticesByDomainTeam(domain.getDomainId(), team.getTeamId());
		TTeamMessageNotice notice = notices.get(domain.getUserId());
		if (notice == null) {
			notice = new TTeamMessageNotice();
			notice.setDomainId(domain.getDomainId());
			notice.setTeamId(team.getTeamId());
			notice.setUserId(domain.getUserId());
			notice.setCount(1);
			TalkDataModel.instance().saveTeamMessageNotice(notice);
			TTeamMessageNotice eNotice = TalkDataModel.instance().selectTeamMessageNoticesByTeamUser(team.getTeamId(), domain.getUserId());
			if (eNotice != null && eNotice.getNoticeId() != notice.getNoticeId()) {
				TalkDataModel.instance().updateTeamMessageNotice(eNotice, 1, notice);
			}
		}
		else {
			TalkDataModel.instance().updateTeamMessageNotice(notice, 1, null);
		}
		List<TDomainUser> domainUserList = TalkDataModel.instance().selectDomainUserListByDomain(domain.getDomainId());
		for (TDomainUser domainUser : domainUserList) {
			notice = notices.get(domainUser.getUserId());
			if (notice == null) {
				notice = new TTeamMessageNotice();
				notice.setDomainId(domain.getDomainId());
				notice.setTeamId(team.getTeamId());
				notice.setUserId(domainUser.getUserId());
				notice.setCount(1);
				TalkDataModel.instance().saveTeamMessageNotice(notice);
				TTeamMessageNotice eNotice = TalkDataModel.instance().selectTeamMessageNoticesByTeamUser(team.getTeamId(), domainUser.getUserId());
				if (eNotice != null && eNotice.getNoticeId() != notice.getNoticeId()) {
					TalkDataModel.instance().updateTeamMessageNotice(eNotice, 1, notice);
				}
			}
			else {
				TalkDataModel.instance().updateTeamMessageNotice(notice, 1, null);
			}
		}
		
		Set<Long> accounts = new HashSet<Long>();
		accounts.add(domain.getUserId());
		for (TDomainUser domainUser : domainUserList) {
			accounts.add(domainUser.getUserId());
		}
		JSONObject jsonMessage = message.toJsonObject();
		jsonMessage.put("inviteDomain", inviteDomain.toJsonObject());
		PushData data = new PushData();
		data.setDataType(EDataType.TALK_MESSAGE.getValue());
		data.setDataContent(jsonMessage.toString());
		ProxyFactory.getProxy(PushServiceProxy.class).push(new PushReq().setAccounts(accounts).setData(data)).ready(new FutureHandler<PushRsp>() {
			@Override
			public void complete(Future<PushRsp> pushRspFuture) throws Exception {
				if (!pushRspFuture.isSuccess()) {
					Logger.getLogger("error").error("TalkHandler.inviteDomainByDomain push business exception.", pushRspFuture.getException());
					return;
				}
				PushRsp pushRsp = pushRspFuture.getResult();
				if (pushRsp.getCode() != EResultCode.OK) {
					Logger.getLogger("error").error("TalkHandler.inviteDomainByDomain push failed. code=" + pushRsp.getCode() + ", message=" + pushRsp.getMessage());
				}
			}
			@Override
			public void exception(CspException e) {
				Logger.getLogger("error").error("TalkHandler.inviteDomainByDomain push invoke exception.", e);
			}
		});
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> kickDomainUser(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final long memberId = IDUtil.parse(jsonReqBody.optString("memberId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser member = AccountDataModel.instance().selectUser(memberId);
		if (member == null) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domainId, userId, EDomainPrivilege.REMOVE_MEMBER.getValue())) {
			return rspFuture.setResult(Service.headonly(13, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		TDomainUser domainMember = TalkDataModel.instance().selectDomainUserByDomainUser(domainId, memberId);
		if (domainMember == null) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		
		TalkDataModel.instance().deleteDomainUser(domainMember);
		
		List<TDomainRoleUser> domainRoleUserList = TalkDataModel.instance().selectDomainRoleUserListByDomainUser(domainId, memberId);
		for (TDomainRoleUser domainRoleUser : domainRoleUserList) {
			TalkDataModel.instance().deleteDomainRoleUser(domainRoleUser);
		}
		
		domain.setTotalValue(domain.getTotalValue() - member.getDeAmount());
		domain.setMemberCount(domain.getMemberCount() - 1);
		if (domain.getMemberCount() < 0) {
			domain.setMemberCount(0);
		}
		if (member.getAliveStatus() == EAliveStatus.ONLINE.getValue()) {
			domain.setOnlineCount(domain.getOnlineCount() - 1);
			if (domain.getOnlineCount() < 0) {
				domain.setOnlineCount(0);
			}
		}
		TalkDataModel.instance().saveDomain(domain, null);
		
		//domain_nft
		List<TUserNft> userNftList = AccountDataModel.instance().selectUserNftListByUser(memberId);
		for (TUserNft userNft : userNftList) {
			TDomainNft domainNft = TalkDataModel.instance().selectDomainNftByDomainOpenid(domainId, userNft.getOpenid());
			if (domainNft == null) {
				continue;
			}
			domainNft.setCount(domainNft.getCount() - userNft.getCount());
			if (domainNft.getCount() < 0) {
				domainNft.setCount(0);
			}
			if (domainNft.getCount() > 0) {
				TalkDataModel.instance().saveDomainNft(domainNft);
			}
			else {
				TalkDataModel.instance().deleteDomainNft(domainNft);
			}
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> joinDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() == userId) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		TDomainUser domainUser = TalkDataModel.instance().selectDomainUserByDomainUser(domainId, userId);
		if (domainUser != null) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		
		domainUser = new TDomainUser();
		domainUser.setDomainId(domainId);
		domainUser.setUserId(userId);
		TalkDataModel.instance().saveDomainUser(domainUser);
		
		TDomainRole defaultRole = TalkDataModel.instance().selectDefaultDomainRoleByDomain(domainId);
		if (defaultRole != null) {
			TDomainRoleUser domainRoleUser = new TDomainRoleUser();
			domainRoleUser.setDomainId(domainId);
			domainRoleUser.setRoleId(defaultRole.getRoleId());
			domainRoleUser.setUserId(userId);
			TalkDataModel.instance().saveDomainRoleUser(domainRoleUser);
		}
		
		domain.setTotalValue(domain.getTotalValue() + user.getDeAmount());
		domain.setMemberCount(domain.getMemberCount() + 1);
		if (user.getAliveStatus() == EAliveStatus.ONLINE.getValue()) {
			domain.setOnlineCount(domain.getOnlineCount() + 1);
			if (domain.getOnlineCount() > domain.getMemberCount()) {
				domain.setOnlineCount(domain.getMemberCount());
			}
		}
		TalkDataModel.instance().saveDomain(domain, null);
		
		//domain_nft
		List<TUserNft> userNftList = AccountDataModel.instance().selectUserNftListByUser(userId);
		for (TUserNft userNft : userNftList) {
			TDomainNft domainNft = TalkDataModel.instance().selectDomainNftByDomainOpenid(domainId, userNft.getOpenid());
			if (domainNft == null) {
				domainNft = new TDomainNft();
				domainNft.setDomainId(domain.getDomainId());
				domainNft.setChainId(userNft.getChainId());
				domainNft.setOpenid(userNft.getOpenid());
				domainNft.setContract(userNft.getContract());
				domainNft.setName(userNft.getName());
				domainNft.setDescription(userNft.getDescription());
				domainNft.setContentUrl(userNft.getContentUrl());
				domainNft.setSourceUrl(userNft.getSourceUrl());
				domainNft.setNftType(userNft.getNftType());
				domainNft.setCount(userNft.getCount());
				domainNft.setPrice(userNft.getPrice());
				TalkDataModel.instance().saveDomainNft(domainNft);
			}
			else {
				domainNft.setCount(domainNft.getCount() + userNft.getCount());
				TalkDataModel.instance().saveDomainNft(domainNft);
			}
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> leaveDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() == userId) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomainUser domainUser = TalkDataModel.instance().selectDomainUserByDomainUser(domainId, userId);
		if (domainUser == null) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		
		TalkDataModel.instance().deleteDomainUser(domainUser);
		
		List<TDomainRoleUser> domainRoleUserList = TalkDataModel.instance().selectDomainRoleUserListByDomainUser(domainId, userId);
		for (TDomainRoleUser domainRoleUser : domainRoleUserList) {
			TalkDataModel.instance().deleteDomainRoleUser(domainRoleUser);
		}
		
		domain.setTotalValue(domain.getTotalValue() - user.getDeAmount());
		domain.setMemberCount(domain.getMemberCount() - 1);
		if (domain.getMemberCount() < 0) {
			domain.setMemberCount(0);
		}
		if (user.getAliveStatus() == EAliveStatus.ONLINE.getValue()) {
			domain.setOnlineCount(domain.getOnlineCount() - 1);
			if (domain.getOnlineCount() < 0) {
				domain.setOnlineCount(0);
			}
		}
		TalkDataModel.instance().saveDomain(domain, null);
		
		//domain_nft
		List<TUserNft> userNftList = AccountDataModel.instance().selectUserNftListByUser(userId);
		for (TUserNft userNft : userNftList) {
			TDomainNft domainNft = TalkDataModel.instance().selectDomainNftByDomainOpenid(domainId, userNft.getOpenid());
			if (domainNft == null) {
				continue;
			}
			domainNft.setCount(domainNft.getCount() - userNft.getCount());
			if (domainNft.getCount() < 0) {
				domainNft.setCount(0);
			}
			if (domainNft.getCount() > 0) {
				TalkDataModel.instance().saveDomainNft(domainNft);
			}
			else {
				TalkDataModel.instance().deleteDomainNft(domainNft);
			}
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> boostDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != user.getUserId()) {
			TDomainUser domainUser = TalkDataModel.instance().selectDomainUserByDomainUser(domainId, userId);
			if (domainUser == null) {
				return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
			}
		}
		Calendar calendar = Calendar.getInstance();
		int newDate = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH);
		calendar.setTimeInMillis(user.getDeAmountBoostTime() * 1000L);
		int oldDate = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH);
		if (newDate <= oldDate) {
			return rspFuture.setResult(Service.headonly(21, Lang.content(TalkLang.DOMAIN_BOOST_NOT_EXPIRE, lang)).toString()).complete();
		}
		
		user.setDeAmountBoostTime(DateTimeUtil.seconds());
		AccountDataModel.instance().saveUser(user);
		
		TalkDataModel.instance().updateDomainBoostValue(domain, user.getDeAmount());
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> buildDomainPoster(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		byte[] share = ImageUtil.share(domain.getName(), domain.getAbout(), domain.getTotalValue() / 100, domain.getMemberCount(), "https://app.rootmeta.xyz");
		
		String guid = StringUtil.guid();
		FileOutputStream out = new FileOutputStream("/data/local/web/www/share/" + guid + ".png");
		out.write(share);
		out.close();
		
		String url = "https://app.rootmeta.xyz/share/" + guid + ".png";
		String path = "/share/" + guid + ".png";
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("url", url);
		jsonRspBody.put("path", path);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadUserDomainList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		//owner
		List<TDomain> domainList = TalkDataModel.instance().selectDomainListByUser(userId);
		//join
		List<TDomainUser> domainUserList = TalkDataModel.instance().selectDomainUserListByUser(userId);
		Set<Long> domainIds = new HashSet<Long>();
		for (TDomainUser domainUser : domainUserList) {
			domainIds.add(domainUser.getDomainId());
		}
		Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
		for (TDomainUser domainUser : domainUserList) {
			TDomain domain = domains.get(domainUser.getDomainId());
			if (domain != null) {
				domainList.add(domain);
			}
		}
		//privilege
		List<TDomainRoleUser> domainRoleUserList = TalkDataModel.instance().selectDomainRoleUserListByUser(userId);
		Set<Long> roleIds = new HashSet<Long>();
		for (TDomainRoleUser domainRoleUser : domainRoleUserList) {
			roleIds.add(domainRoleUser.getRoleId());
		}
		Map<Long, TDomainRole> domainRoles = TalkDataModel.instance().selectDomainRoles(roleIds);
		Map<Long, List<TDomainRole>> domainRoleLists = new HashMap<Long, List<TDomainRole>>();
		for (TDomainRole domainRole : domainRoles.values()) {
			List<TDomainRole> domainRoleList = domainRoleLists.get(domainRole.getDomainId());
			if (domainRoleList == null) {
				domainRoleList = new LinkedList<TDomainRole>();
				domainRoleLists.put(domainRole.getDomainId(), domainRoleList);
			}
			domainRoleList.add(domainRole);
		}
		
		List<TDomain> rootDomainList = new LinkedList<TDomain>();
		for (int i = 0; i < domainList.size(); i++) {
			TDomain domain = domainList.get(i);
			if (domain.getRoot() == EBoolean.YES.getValue()) {
				domainList.remove(i);
				rootDomainList.add(0, domain);
				i--;
			}
		}
		for (TDomain rootDomain : rootDomainList) {
			domainList.add(0, rootDomain);
		}
		
		JSONArray jsonDomainList = new JSONArray();
		for (TDomain domain : domainList) {
			JSONObject jsonDomain = domain.toJsonObject();
			jsonDomainList.put(jsonDomain);
			JSONArray jsonPrivileges = new JSONArray();
			jsonDomain.put("privileges", jsonPrivileges);
			
			Set<Integer> privileges = new HashSet<Integer>();
			List<TDomainRole> domainRoleList = domainRoleLists.get(domain.getDomainId());
			if (domainRoleList != null) {
				for (TDomainRole domainRole : domainRoleList) {
					JSONArray jsoniPrivileges = new JSONArray(domainRole.getPrivileges());
					for (int i = 0; i < jsoniPrivileges.length(); i++) {
						privileges.add(jsoniPrivileges.getInt(i));
					}
				}
			}
			jsonPrivileges.put(privileges);
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domainList", jsonDomainList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadUserMutualDomains(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		Map<Long, List<TDomainUser>> domainUserLists = TalkDataModel.instance().selectDomainUserListsByUsers(new HashSet<Long>(Arrays.asList(userId, friendId)));
		List<TDomain> userOwnDomains = TalkDataModel.instance().selectDomainListByUser(userId);
		List<TDomain> friendOwnDomains = TalkDataModel.instance().selectDomainListByUser(friendId);
		
		Set<Long> domainIds = new HashSet<Long>();
		
		Set<Long> userDomainIds = new HashSet<Long>();
		List<TDomainUser> userDomainList = domainUserLists.get(userId);
		if (userDomainList != null) {
			for (TDomainUser userDomain : userDomainList) {
				userDomainIds.add(userDomain.getDomainId());
			}
		}
		for (TDomain userOwnDomain : userOwnDomains) {
			userDomainIds.add(userOwnDomain.getDomainId());
		}
		
		List<TDomainUser> friendDomainList = domainUserLists.get(friendId);
		if (friendDomainList != null) {
			for (TDomainUser friendDomain : friendDomainList) {
				if (userDomainIds.contains(friendDomain.getDomainId())) {
					domainIds.add(friendDomain.getDomainId());
				}
			}
		}
		for (TDomain friendOwnDomain : friendOwnDomains) {
			if (userDomainIds.contains(friendOwnDomain.getDomainId())) {
				domainIds.add(friendOwnDomain.getDomainId());
			}
		}
		
		Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
		
		JSONArray jsonDomains = new JSONArray();
		for (TDomain domain : domains.values()) {
			jsonDomains.put(domain.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domains", jsonDomains);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final boolean withTag = jsonReqBody.optBoolean("withTag", false);
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		
		Set<Integer> privileges = new HashSet<Integer>();
		if (domain != null) {
			List<TDomainRoleUser> domainRoleUserList = TalkDataModel.instance().selectDomainRoleUserListByDomainUser(domainId, userId);
			Set<Long> roleIds = new HashSet<Long>();
			for (TDomainRoleUser domainRoleUser : domainRoleUserList) {
				roleIds.add(domainRoleUser.getRoleId());
			}
			Map<Long, TDomainRole> domainRoles = TalkDataModel.instance().selectDomainRoles(roleIds);
			for (TDomainRole domainRole : domainRoles.values()) {
				JSONArray jsonPrivileges = new JSONArray(domainRole.getPrivileges());
				for (int i = 0; i < jsonPrivileges.length(); i++) {
					privileges.add(jsonPrivileges.getInt(i));
				}
			}
		}
		
		List<TTag> tagList = new LinkedList<TTag>();
		if (withTag) {
			List<TDomainTag> domainTagList = TalkDataModel.instance().selectDomainTagListByDomain(domainId);
			Set<Long> tagIds = new HashSet<Long>();
			for (TDomainTag domainTag : domainTagList) {
				tagIds.add(domainTag.getTagId());
			}
			Map<Long, TTag> tags = BaseDataModel.instance().selectTags(tagIds, lang);
			for (TDomainTag domainTag : domainTagList) {
				TTag tag = tags.get(domainTag.getTagId());
				if (tag != null) {
					tagList.add(tag);
				}
			}
		}
		
		JSONObject jsonDomain = null;
		if (domain != null) {
			jsonDomain = domain.toJsonObject();
			jsonDomain.put("privileges", new JSONArray(privileges));
		}
		JSONArray jsonTagList = new JSONArray();
		for (TTag tag : tagList) {
			jsonTagList.put(tag.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domain", jsonDomain != null ? jsonDomain : JSONObject.NULL);
		jsonRspBody.put("tagList", jsonTagList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadDomainTeamList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TCategory> categoryList = TalkDataModel.instance().selectCategoryListByDomain(domainId);
		List<TTeam> teamList = TalkDataModel.instance().selectTeamListByDomain(domainId);
		
		JSONArray jsonCategoryList = new JSONArray();
		for (TCategory category : categoryList) {
			jsonCategoryList.put(category.toJsonObject());
		}
		JSONArray jsonTeamList = new JSONArray();
		for (TTeam team : teamList) {
			jsonTeamList.put(team.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("categoryList", jsonCategoryList);
		jsonRspBody.put("teamList", jsonTeamList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadDomainRoleList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TDomainRole> domainRoleList = TalkDataModel.instance().selectDomainRoleListByDomain(domainId);
		
		JSONArray jsonDomainRoleList = new JSONArray();
		for (TDomainRole domainRole : domainRoleList) {
			jsonDomainRoleList.put(domainRole.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domainRoleList", jsonDomainRoleList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadDomainUserList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser owner = AccountDataModel.instance().selectUser(domain.getUserId());
		
		List<TDomainUser> domainUserList = TalkDataModel.instance().selectDomainUserListByDomain(domainId);
		Map<Long, TDomainUser> domainUsers = new HashMap<Long, TDomainUser>();
		for (TDomainUser domainUser : domainUserList) {
			domainUsers.put(domainUser.getUserId(), domainUser);
		}
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(domainUsers.keySet());
		List<TUser> userList = new LinkedList<TUser>(users.values());
		if (owner != null) {
			userList.add(owner);
		}
		userList.sort(new Comparator<TUser>() {
			@Override
			public int compare(TUser u1, TUser u2) {
				//return u1.getNickname().compareTo(u2.getNickname());
				if (u1.getDeAmount() < u2.getDeAmount()) {
					return 11;
				}
				else if (u1.getDeAmount() > u2.getDeAmount()) {
					return -1;
				}
				else {
					return 0;
				}
			}
		});
		List<TDomainRole> domainRoleList = TalkDataModel.instance().selectDomainRoleListByDomain(domainId);
		List<TDomainRoleUser> domainRoleUserList = TalkDataModel.instance().selectDomainRoleUserListByDomain(domainId);
		
		JSONArray jsonUserList = new JSONArray();
		for (TUser user : userList) {
			TDomainUser domainUser = domainUsers.get(user.getUserId());
			if (domainUser != null) {
				if (!StringUtil.isNullOrEmpty(domainUser.getNickname())) {
					user.setNickname(domainUser.getNickname());
				}
				if (!StringUtil.isNullOrEmpty(domainUser.getAvatar())) {
					user.setAvatar(domainUser.getAvatar());
				}
			}
			JSONObject jsonUser = user.toJsonObject();
			if (owner != null && user.getUserId() == owner.getUserId()) {
				jsonUser.put("owner", true);
			}
			jsonUserList.put(jsonUser);
		}
		JSONArray jsonDomainRoleList = new JSONArray();
		for (TDomainRole domainRole : domainRoleList) {
			jsonDomainRoleList.put(domainRole.toJsonObject());
		}
		JSONArray jsonDomainRoleUserList = new JSONArray();
		for (TDomainRoleUser domainRoleUser : domainRoleUserList) {
			jsonDomainRoleUserList.put(domainRoleUser.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("userList", jsonUserList);
		jsonRspBody.put("domainRoleList", jsonDomainRoleList);
		jsonRspBody.put("domainRoleUserList", jsonDomainRoleUserList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadTeamMessageList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long teamId = IDUtil.parse(jsonReqBody.optString("teamId", ""));
		final long fromId = IDUtil.parse(jsonReqBody.optString("fromId", ""));
		final long toId = IDUtil.parse(jsonReqBody.optString("toId", ""));
		final int size = jsonReqBody.optInt("size", 50);
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeam team = TalkDataModel.instance().selectTeam(teamId);
		if (team == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(team.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.VIEW_TEAM.getValue())) {
			JSONObject jsonRsp = Service.headonly();
			JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
			jsonRspBody.put("messageList", new JSONArray());
			jsonRspBody.put("users", new JSONArray());
			jsonRspBody.put("domains", new JSONArray());
			jsonRspBody.put("replyMessages", new JSONArray());
			return rspFuture.setResult(jsonRsp.toString()).complete();
		}
		
		List<TTeamMessage> messageList = TalkDataModel.instance().selectTeamMessageList(teamId, fromId, toId, size);
		
		Set<Long> messageIds = new HashSet<Long>();
		Set<Long> userIds = new HashSet<Long>();
		Set<Long> domainIds = new HashSet<Long>();
		Set<Long> replyMessageIds = new HashSet<Long>();
		for (TTeamMessage message : messageList) {
			messageIds.add(message.getMessageId());
			userIds.add(message.getUserId());
			if (message.getInviteDomainId() > 0L) {
				domainIds.add(message.getInviteDomainId());
			}
			if (message.getReplyMessageId() > 0L) {
				replyMessageIds.add(message.getReplyMessageId());
			}
		}
		Map<Long, TTeamMessage> replyMessages = TalkDataModel.instance().selectTeamMessages(replyMessageIds);
		for (TTeamMessage replyMessage : replyMessages.values()) {
			userIds.add(replyMessage.getUserId());
			if (replyMessage.getInviteDomainId() > 0L) {
				domainIds.add(replyMessage.getInviteDomainId());
			}
		}
		Map<Long, List<TTeamMessageReaction>> reactionLists = TalkDataModel.instance().selectTeamMessageReactionListsByMessages(messageIds);
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
		
		JSONArray jsonMessageList = new JSONArray();
		for (TTeamMessage message : messageList) {
			JSONObject jsonMessage = message.toJsonObject();
			JSONArray jsonReactionList = new JSONArray();
			jsonMessage.put("reactionList", jsonReactionList);
			jsonMessageList.put(jsonMessage);
			
			Map<String, JSONObject> reactionCounts = new HashMap<String, JSONObject>();
			List<TTeamMessageReaction> reactionList = reactionLists.get(message.getMessageId());
			if (reactionList != null) {
				for (TTeamMessageReaction reaction : reactionList) {
					JSONObject reactionCount = reactionCounts.get(reaction.getReaction());
					if (reactionCount == null) {
						reactionCount = new JSONObject();
						reactionCount.put("reaction", reaction.getReaction());
						reactionCount.put("count", 0);
						reactionCounts.put(reaction.getReaction(), reactionCount);
					}
					reactionCount.put("count", reactionCount.getInt("count") + 1);
				}
			}
			for (JSONObject reactionCount : reactionCounts.values()) {
				jsonReactionList.put(reactionCount);
			}
		}
		JSONArray jsonUsers = new JSONArray();
		for (TUser user : users.values()) {
			jsonUsers.put(user.toJsonObject());
		}
		JSONArray jsonDomains = new JSONArray();
		for (TDomain idomain : domains.values()) {
			jsonDomains.put(idomain.toJsonObject());
		}
		JSONArray jsonReplyMessages = new JSONArray();
		for (TTeamMessage replyMessage : replyMessages.values()) {
			jsonReplyMessages.put(replyMessage.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("messageList", jsonMessageList);
		jsonRspBody.put("users", jsonUsers);
		jsonRspBody.put("domains", jsonDomains);
		jsonRspBody.put("replyMessages", jsonReplyMessages);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadTeamMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeamMessage message = TalkDataModel.instance().selectTeamMessage(messageId);
		if (message == null) {
			JSONObject jsonRsp = Service.headonly();
			JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
			jsonRspBody.put("message", JSONObject.NULL);
			return rspFuture.setResult(jsonRsp.toString()).complete();
		}
		
		Set<Long> userIds = new HashSet<Long>();
		Set<Long> domainIds = new HashSet<Long>();
		userIds.add(message.getUserId());
		if (message.getInviteDomainId() > 0L) {
			domainIds.add(message.getInviteDomainId());
		}
		TTeamMessage replyMessage = null;
		if (message.getReplyMessageId() > 0L) {
			replyMessage = TalkDataModel.instance().selectTeamMessage(message.getReplyMessageId());
			if (replyMessage != null) {
				userIds.add(replyMessage.getUserId());
				if (replyMessage.getInviteDomainId() > 0L) {
					domainIds.add(replyMessage.getInviteDomainId());
				}
			}
		}
		List<TTeamMessageReaction> reactionList = TalkDataModel.instance().selectTeamMessageReactionListByMessage(messageId);
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
		
		JSONObject jsonMessage = message.toJsonObject();
		JSONArray jsonReactionList = new JSONArray();
		jsonMessage.put("reactionList", jsonReactionList);
		Map<String, JSONObject> reactionCounts = new HashMap<String, JSONObject>();
		for (TTeamMessageReaction reaction : reactionList) {
			JSONObject reactionCount = reactionCounts.get(reaction.getReaction());
			if (reactionCount == null) {
				reactionCount = new JSONObject();
				reactionCount.put("reaction", reaction.getReaction());
				reactionCount.put("count", 0);
				reactionCounts.put(reaction.getReaction(), reactionCount);
			}
			reactionCount.put("count", reactionCount.getInt("count") + 1);
		}
		for (JSONObject reactionCount : reactionCounts.values()) {
			jsonReactionList.put(reactionCount);
		}
		TUser user = users.get(message.getUserId());
		if (user != null) {
			jsonMessage.put("user", user.toJsonObject());
		}
		if (message.getInviteDomainId() > 0L) {
			TDomain inviteDomain = domains.get(message.getInviteDomainId());
			if (inviteDomain != null) {
				jsonMessage.put("inviteDomain", inviteDomain.toJsonObject());
			}
		}
		if (replyMessage != null) {
			JSONObject jsonReplyMessage = replyMessage.toJsonObject();
			jsonMessage.put("replyMessage", jsonReplyMessage);
			user = users.get(replyMessage.getUserId());
			if (user != null) {
				jsonReplyMessage.put("user", user.toJsonObject());
			}
			if (replyMessage.getInviteDomainId() > 0L) {
				TDomain inviteDomain = domains.get(replyMessage.getInviteDomainId());
				if (inviteDomain != null) {
					jsonReplyMessage.put("inviteDomain", inviteDomain.toJsonObject());
				}
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("message", jsonMessage);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> saveDomainRole(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long roleId = IDUtil.parse(jsonReqBody.optString("roleId", ""));
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final String name = jsonReqBody.optString("name", "").trim();
		final String color = jsonReqBody.optString("color", "").trim();
		final JSONArray jsonPrivileges = jsonReqBody.optJSONArray("privileges");
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domainId, userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TDomainRole domainRole = TalkDataModel.instance().selectDomainRole(roleId);
		if (domainRole == null) {
			domainRole = new TDomainRole();
			domainRole.setDomainId(domainId);
			domainRole.setName(Lang.content(TalkLang.NEW_ROLE_NAME, lang));
			domainRole.setColor("");
			domainRole.setPrivileges(new JSONArray().toString());
			domainRole.setIsdefault(EBoolean.NO.getValue());
			TalkDataModel.instance().saveDomainRole(domainRole);
		}
		else {
			domainRole.setName(name);
			domainRole.setColor(color);
			domainRole.setPrivileges(jsonPrivileges != null ? jsonPrivileges.toString() : new JSONArray().toString());
			TalkDataModel.instance().saveDomainRole(domainRole);
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domainRole", domainRole.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> addDomainRoleUser(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final long roleId = IDUtil.parse(jsonReqBody.optString("roleId", ""));
		final JSONArray jsonUserIds = jsonReqBody.optJSONArray("userIds");
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (jsonUserIds == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domainId, userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		for (int i = 0; i < jsonUserIds.length(); i++) {
			long iuserId = IDUtil.parse(jsonUserIds.optString(i, ""));
			if (iuserId > 0L) {
				TDomainRoleUser domainRoleUser = TalkDataModel.instance().selectDomainRoleUserByDomainRoleUser(domainId, roleId, iuserId);
				if (domainRoleUser == null) {
					domainRoleUser = new TDomainRoleUser();
					domainRoleUser.setDomainId(domainId);
					domainRoleUser.setRoleId(roleId);
					domainRoleUser.setUserId(iuserId);
					TalkDataModel.instance().saveDomainRoleUser(domainRoleUser);
				}
			}
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> removeDomainRoleUser(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final long roleId = IDUtil.parse(jsonReqBody.optString("roleId", ""));
		final JSONArray jsonUserIds = jsonReqBody.optJSONArray("userIds");
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (jsonUserIds == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domainId, userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		for (int i = 0; i < jsonUserIds.length(); i++) {
			long iuserId = IDUtil.parse(jsonUserIds.optString(i, ""));
			TDomainRoleUser domainRoleUser = TalkDataModel.instance().selectDomainRoleUserByDomainRoleUser(domainId, roleId, iuserId);
			if (domainRoleUser != null) {
				TalkDataModel.instance().deleteDomainRoleUser(domainRoleUser);
			}
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> createDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final String name = jsonReqBody.optString("name", "").trim();
		final String avatar = jsonReqBody.optString("avatar", "").trim();
		final String banner = jsonReqBody.optString("banner", "").trim();
		final String about = jsonReqBody.optString("about", "").trim();
		final String address = jsonReqBody.optString("address", "").trim();
		JSONArray jsonTagIds = jsonReqBody.optJSONArray("tagIds");
		
		final Future<String> rspFuture = new Future<String>();
		
		if (StringUtil.isNullOrEmpty(name)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.DOMAIN_NAME_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		String domainBanner = null;
		if (StringUtil.isNullOrEmpty(banner)) {
			domainBanner = "/assets/img/domain_banner_0" + RandomUtil.randomInt(1, 5) + ".png";
		}
		else {
			domainBanner = banner;
		}
		
		TDomain domain = new TDomain();
		domain.setUserId(userId);
		domain.setBuildTime(DateTimeUtil.seconds());
		domain.setTotalValue(user.getDeAmount());
		domain.setMemberCount(1);
		domain.setName(name);
		domain.setAvatar(avatar);
		domain.setBanner(domainBanner);
		domain.setAbout(about);
		domain.setAddress(address);
		domain.setVisibility(EDomainVisibility.PUBLIC.getValue());
		
		List<TDomainTag> domainTagList = new LinkedList<TDomainTag>();
		if (jsonTagIds != null) {
			for (int i = 0; i < jsonTagIds.length(); i++) {
				long tagId = IDUtil.parse(jsonTagIds.optString(i, ""));
				if (tagId > 0L) {
					TDomainTag domainTag = new TDomainTag();
					domainTag.setTagId(tagId);
					domainTagList.add(domainTag);
				}
			}
		}
		TalkDataModel.instance().saveDomain(domain, domainTagList);
		
		//domain_nft
		List<TUserNft> userNftList = AccountDataModel.instance().selectUserNftListByUser(userId);
		for (TUserNft userNft : userNftList) {
			TDomainNft domainNft = new TDomainNft();
			domainNft.setDomainId(domain.getDomainId());
			domainNft.setChainId(userNft.getChainId());
			domainNft.setOpenid(userNft.getOpenid());
			domainNft.setContract(userNft.getContract());
			domainNft.setName(userNft.getName());
			domainNft.setDescription(userNft.getDescription());
			domainNft.setContentUrl(userNft.getContentUrl());
			domainNft.setSourceUrl(userNft.getSourceUrl());
			domainNft.setNftType(userNft.getNftType());
			domainNft.setCount(userNft.getCount());
			domainNft.setPrice(userNft.getPrice());
			TalkDataModel.instance().saveDomainNft(domainNft);
		}
		
		TCategory category = new TCategory();
		category.setDomainId(domain.getDomainId());
		category.setName(Lang.content(TalkLang.DEFAULT_CATEGORY_NAME, lang));
		TalkDataModel.instance().saveCategory(category);
		
		TTeam team = new TTeam();
		team.setDomainId(domain.getDomainId());
		team.setCategoryId(category.getCategoryId());
		team.setName(Lang.content(TalkLang.DEFAULT_TEAM_NAME, lang));
		team.setAbout("");
		TalkDataModel.instance().saveTeam(team);
		
		TDomainRole domainRole = new TDomainRole();
		domainRole.setDomainId(domain.getDomainId());
		domainRole.setName(Lang.content(TalkLang.DEFAULT_ROLE_NAME, lang));
		domainRole.setColor("");
		JSONArray jsonPrivileges = new JSONArray();
		jsonPrivileges.put(EDomainPrivilege.VIEW_TEAM.getValue());
		jsonPrivileges.put(EDomainPrivilege.INVITE_MEMBER.getValue());
		jsonPrivileges.put(EDomainPrivilege.SEND_MESSAGE.getValue());
		domainRole.setPrivileges(jsonPrivileges.toString());
		domainRole.setIsdefault(EBoolean.YES.getValue());
		TalkDataModel.instance().saveDomainRole(domainRole);
		
		TDomainRoleUser domainRoleUser = new TDomainRoleUser();
		domainRoleUser.setDomainId(domain.getDomainId());
		domainRoleUser.setRoleId(domainRole.getRoleId());
		domainRoleUser.setUserId(userId);
		TalkDataModel.instance().saveDomainRoleUser(domainRoleUser);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domain", domain.toJsonObject());
		jsonRspBody.put("category", category.toJsonObject());
		jsonRspBody.put("team", team.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> editDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final String name = jsonReqBody.optString("name", "").trim();
		final String avatar = jsonReqBody.optString("avatar", "").trim();
		final String banner = jsonReqBody.optString("banner", "").trim();
		final String about = jsonReqBody.optString("about", "").trim();
		final String address = jsonReqBody.optString("address", "").trim();
		final int visibility = jsonReqBody.optInt("visibility", 0);
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domainId, userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		if (!StringUtil.isNullOrEmpty(name)) {
			domain.setName(name);
		}
		if (!StringUtil.isNullOrEmpty(avatar)) {
			domain.setAvatar(avatar);
		}
		if (!StringUtil.isNullOrEmpty(banner)) {
			domain.setBanner(banner);
		}
		if (!StringUtil.isNullOrEmpty(about)) {
			domain.setAbout(about);
		}
		if (!StringUtil.isNullOrEmpty(address)) {
			domain.setAddress(address);
		}
		if (EDomainVisibility.toEnum(visibility) != null) {
			domain.setVisibility(visibility);
		}
		TalkDataModel.instance().saveDomain(domain, null);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domain", domain.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> deleteDomain(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TalkDataModel.instance().deleteDomain(domain);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> createCategory(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final String name = jsonReqBody.optString("name", "").trim();
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domainId, userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		if (StringUtil.isNullOrEmpty(name)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.CATEGORY_NAME_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		TCategory category = TalkDataModel.instance().selectCategoryByName(domainId, name);
		if (category != null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.CATEGORY_NAME_ALREADY_EXIST, lang)).toString()).complete();
		}
		
		category = new TCategory();
		category.setDomainId(domainId);
		category.setName(name);
		TalkDataModel.instance().saveCategory(category);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("category", category.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> editCategory(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long categoryId = IDUtil.parse(jsonReqBody.optString("categoryId", ""));
		final String name = jsonReqBody.optString("name", "").trim();
		
		final Future<String> rspFuture = new Future<String>();
		
		TCategory category = TalkDataModel.instance().selectCategory(categoryId);
		if (category == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(category.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		if (StringUtil.isNullOrEmpty(name)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.CATEGORY_NAME_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		TCategory nameCategory = TalkDataModel.instance().selectCategoryByName(domain.getDomainId(), name);
		if (nameCategory != null && nameCategory.getCategoryId() != category.getCategoryId()) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.CATEGORY_NAME_ALREADY_EXIST, lang)).toString()).complete();
		}
		
		category.setName(name);
		TalkDataModel.instance().saveCategory(category);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("category", category.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> deleteCategory(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long categoryId = IDUtil.parse(jsonReqBody.optString("categoryId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TCategory category = TalkDataModel.instance().selectCategory(categoryId);
		if (category == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(category.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TalkDataModel.instance().deleteCategory(category);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> createTeam(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long domainId = IDUtil.parse(jsonReqBody.optString("domainId", ""));
		final long categoryId = IDUtil.parse(jsonReqBody.optString("categoryId", ""));
		final String name = jsonReqBody.optString("name", "").trim();
		final String about = jsonReqBody.optString("about", "").trim();
		final int adminOnly = jsonReqBody.optInt("adminOnly", 0);
		
		final Future<String> rspFuture = new Future<String>();
		
		TDomain domain = TalkDataModel.instance().selectDomain(domainId);
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domainId, userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		TCategory category = TalkDataModel.instance().selectCategory(categoryId);
		if (categoryId > 0L && category == null) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (category != null && category.getDomainId() != domainId) {
			return rspFuture.setResult(Service.headonly(13, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (StringUtil.isNullOrEmpty(name)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.TEAM_NAME_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		TTeam team = TalkDataModel.instance().selectTeamByName(domainId, name);
		if (team != null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.TEAM_NAME_ALREADY_EXIST, lang)).toString()).complete();
		}
		
		team = new TTeam();
		team.setDomainId(domainId);
		team.setCategoryId(categoryId);
		team.setName(name);
		team.setAbout(about);
		team.setAdminOnly(adminOnly);
		TalkDataModel.instance().saveTeam(team);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("team", team.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> editTeam(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long teamId = IDUtil.parse(jsonReqBody.optString("teamId", ""));
		final String name = jsonReqBody.optString("name", "").trim();
		final String about = jsonReqBody.optString("about", "").trim();
		final int adminOnly = jsonReqBody.optInt("adminOnly", 0);
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeam team = TalkDataModel.instance().selectTeam(teamId);
		if (team == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(team.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		if (StringUtil.isNullOrEmpty(name)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.TEAM_NAME_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		TTeam nameTeam = TalkDataModel.instance().selectTeamByName(domain.getDomainId(), name);
		if (nameTeam != null && nameTeam.getTeamId() != team.getTeamId()) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.TEAM_NAME_ALREADY_EXIST, lang)).toString()).complete();
		}
		
		team.setName(name);
		team.setAbout(about);
		team.setAdminOnly(adminOnly);
		TalkDataModel.instance().saveTeam(team);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("team", team.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> deleteTeam(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long teamId = IDUtil.parse(jsonReqBody.optString("teamId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeam team = TalkDataModel.instance().selectTeam(teamId);
		if (team == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(team.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TalkDataModel.instance().deleteTeam(team);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadTeamLimitListByTeam(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long teamId = IDUtil.parse(jsonReqBody.optString("teamId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeam team = TalkDataModel.instance().selectTeam(teamId);
		if (team == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(team.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			JSONObject jsonRsp = Service.headonly();
			JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
			jsonRspBody.put("teamLimitList", new JSONArray());
			jsonRspBody.put("tokens", new JSONArray());
			return rspFuture.setResult(jsonRsp.toString()).complete();
		}
		
		List<TTeamLimit> teamLimitList = TalkDataModel.instance().selectTeamLimitListByTeam(teamId);
		Set<Long> tokenIds = new HashSet<Long>();
		for (TTeamLimit teamLimit : teamLimitList) {
			tokenIds.add(teamLimit.getTokenId());
		}
		Map<Long, TToken> tokens = BaseDataModel.instance().selectTokens(tokenIds);
		
		JSONArray jsonTeamLimitList = new JSONArray();
		for (TTeamLimit teamLimit : teamLimitList) {
			jsonTeamLimitList.put(teamLimit.toJsonObject());
		}
		JSONArray jsonTokens = new JSONArray();
		for (TToken token : tokens.values()) {
			jsonTokens.put(token.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("teamLimitList", jsonTeamLimitList);
		jsonRspBody.put("tokens", jsonTokens);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> saveTeamLimitList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long teamId = IDUtil.parse(jsonReqBody.optString("teamId", ""));
		final JSONArray jsonTeamLimitList = jsonReqBody.optJSONArray("teamLimitList");
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeam team = TalkDataModel.instance().selectTeam(teamId);
		if (team == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(team.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		if (jsonTeamLimitList == null || jsonTeamLimitList.length() == 0) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		
		List<TTeamLimit> limitList = TalkDataModel.instance().selectTeamLimitListByTeam(teamId);
		Map<Long, TTeamLimit> limits = new HashMap<Long, TTeamLimit>();
		for (TTeamLimit limit : limitList) {
			limits.put(limit.getLimitId(), limit);
		}
		
		TTeamLimit tvfTeamLimit = null;
		List<TTeamLimit> teamLimitList = new LinkedList<TTeamLimit>();
		for (int i = 0; i < jsonTeamLimitList.length(); i++) {
			JSONObject jsonTeamLimit = jsonTeamLimitList.getJSONObject(i);
			long limitId = IDUtil.parse(jsonTeamLimit.optString("limitId", ""));
			long tvfLimit = jsonTeamLimit.optLong("tvfLimit", 0L);
			long tokenId = IDUtil.parse(jsonTeamLimit.optString("tokenId", ""));
			long tokenLimit = jsonTeamLimit.optLong("tokenLimit", 0L);
			String nftContract = jsonTeamLimit.optString("nftContract", "").trim();
			int limitType = jsonTeamLimit.optInt("limitType", 0);
			int enabled = jsonTeamLimit.optInt("enabled", 0);
			
			TTeamLimit teamLimit = limits.remove(limitId);
			if (teamLimit == null) {
				teamLimit = new TTeamLimit();
				teamLimit.setDomainId(team.getDomainId());
				teamLimit.setTeamId(team.getTeamId());
			}
			if (limitType == ETeamLimitType.TVF.getValue()) {
				teamLimit.setTvfLimit(tvfLimit);
				teamLimit.setTokenId(0L);
				teamLimit.setTokenLimit(0L);
				teamLimit.setNftContract("");
				if (tvfTeamLimit == null) {
					tvfTeamLimit = teamLimit;
				}
				else {
					return rspFuture.setResult(Service.headonly(21, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
				}
			}
			else if (limitType == ETeamLimitType.TOKEN.getValue()) {
				if (teamLimit.getLimitId() <= 0L && (tokenId <= 0L || tokenLimit <= 0L)) {
					continue;
				}
				teamLimit.setTvfLimit(0L);
				teamLimit.setTokenId(tokenId);
				teamLimit.setTokenLimit(tokenLimit);
				teamLimit.setNftContract("");
			}
			else if (limitType == ETeamLimitType.NFT.getValue()) {
				if (teamLimit.getLimitId() <= 0L && StringUtil.isNullOrEmpty(nftContract)) {
					continue;
				}
				teamLimit.setTvfLimit(0L);
				teamLimit.setTokenId(0L);
				teamLimit.setTokenLimit(0L);
				teamLimit.setNftContract(nftContract);
			}
			else {
				return rspFuture.setResult(Service.headonly(22, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
			}
			teamLimit.setLimitType(limitType);
			teamLimit.setEnabled(enabled == EBoolean.YES.getValue() ? EBoolean.YES.getValue() : EBoolean.NO.getValue());
			teamLimitList.add(teamLimit);
		}
		
		TalkDataModel.instance().saveTeamLimitList(teamLimitList, new LinkedList<TTeamLimit>(limits.values()));
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> sendTeamMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final String client = jsonReqHead.optString("client", "");
		final String series = jsonReqHead.optString("series", "");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long teamId = IDUtil.parse(jsonReqBody.optString("teamId", ""));
		final int messageType = jsonReqBody.optInt("messageType", 0);
		final String textContent = jsonReqBody.optString("textContent", "").trim();
		final String url = jsonReqBody.optString("url", "").trim();
		final long replyMessageId = IDUtil.parse(jsonReqBody.optString("replyMessageId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeam team = TalkDataModel.instance().selectTeam(teamId);
		if (team == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(team.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (ETalkMessageType.toEnum(messageType) == null) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (messageType == ETalkMessageType.TEXT.getValue() && StringUtil.isNullOrEmpty(textContent)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.MESSAGE_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		if (team.getAdminOnly() != EBoolean.YES.getValue()) {
			if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.SEND_MESSAGE.getValue())) {
				return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
			}
		}
		else {
			if (domain.getUserId() != userId && !checkDomainPrivilege(domain.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
				return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
			}
		}
		
		TTeamMessage message = new TTeamMessage();
		message.setDomainId(domain.getDomainId());
		message.setTeamId(teamId);
		message.setUserId(userId);
		message.setClient(client);
		message.setSeries(series);
		message.setMessageTime(DateTimeUtil.seconds());
		message.setMessageType(messageType);
		message.setTextContent(textContent);
		message.setFileUrl(url);
		message.setReplyMessageId(replyMessageId);
		message.setInviteDomainId(0L);
		message.setEdited(EBoolean.NO.getValue());
		TalkDataModel.instance().saveTeamMessage(message);
		
		Map<Long, TTeamMessageNotice> notices = TalkDataModel.instance().selectTeamMessageNoticesByDomainTeam(domain.getDomainId(), teamId);
		TTeamMessageNotice notice = notices.get(domain.getUserId());
		if (notice == null) {
			notice = new TTeamMessageNotice();
			notice.setDomainId(domain.getDomainId());
			notice.setTeamId(teamId);
			notice.setUserId(domain.getUserId());
			notice.setCount(1);
			TalkDataModel.instance().saveTeamMessageNotice(notice);
			TTeamMessageNotice eNotice = TalkDataModel.instance().selectTeamMessageNoticesByTeamUser(teamId, domain.getUserId());
			if (eNotice != null && eNotice.getNoticeId() != notice.getNoticeId()) {
				TalkDataModel.instance().updateTeamMessageNotice(eNotice, 1, notice);
			}
		}
		else {
			TalkDataModel.instance().updateTeamMessageNotice(notice, 1, null);
		}
		List<TDomainUser> domainUserList = TalkDataModel.instance().selectDomainUserListByDomain(domain.getDomainId());
		for (TDomainUser domainUser : domainUserList) {
			notice = notices.get(domainUser.getUserId());
			if (notice == null) {
				notice = new TTeamMessageNotice();
				notice.setDomainId(domain.getDomainId());
				notice.setTeamId(teamId);
				notice.setUserId(domainUser.getUserId());
				notice.setCount(1);
				TalkDataModel.instance().saveTeamMessageNotice(notice);
				TTeamMessageNotice eNotice = TalkDataModel.instance().selectTeamMessageNoticesByTeamUser(teamId, domainUser.getUserId());
				if (eNotice != null && eNotice.getNoticeId() != notice.getNoticeId()) {
					TalkDataModel.instance().updateTeamMessageNotice(eNotice, 1, notice);
				}
			}
			else {
				TalkDataModel.instance().updateTeamMessageNotice(notice, 1, null);
			}
		}
		
		Set<Long> accounts = new HashSet<Long>();
		accounts.add(domain.getUserId());
		for (TDomainUser domainUser : domainUserList) {
			accounts.add(domainUser.getUserId());
		}
		PushData data = new PushData();
		data.setDataType(EDataType.TALK_MESSAGE.getValue());
		data.setDataContent(message.toJsonObject().toString());
		ProxyFactory.getProxy(PushServiceProxy.class).push(new PushReq().setAccounts(accounts).setData(data)).ready(new FutureHandler<PushRsp>() {
			@Override
			public void complete(Future<PushRsp> pushRspFuture) throws Exception {
				if (!pushRspFuture.isSuccess()) {
					Logger.getLogger("error").error("TalkHandler.sendTeamMessage push business exception.", pushRspFuture.getException());
					return;
				}
				PushRsp pushRsp = pushRspFuture.getResult();
				if (pushRsp.getCode() != EResultCode.OK) {
					Logger.getLogger("error").error("TalkHandler.sendTeamMessage push failed. code=" + pushRsp.getCode() + ", message=" + pushRsp.getMessage());
				}
			}
			@Override
			public void exception(CspException e) {
				Logger.getLogger("error").error("TalkHandler.sendTeamMessage push invoke exception.", e);
			}
		});
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> editTeamMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		final String textContent = jsonReqBody.optString("textContent", "").trim();
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeamMessage message = TalkDataModel.instance().selectTeamMessage(messageId);
		if (message == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (message.getUserId() != userId) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		if (message.getMessageType() == ETalkMessageType.TEXT.getValue() && StringUtil.isNullOrEmpty(textContent)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(TalkLang.MESSAGE_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		
		message.setTextContent(textContent);
		message.setEdited(EBoolean.YES.getValue());
		TalkDataModel.instance().saveTeamMessage(message);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("message", message.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> reactTeamMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		final String emojiContent = jsonReqBody.optString("emojiContent", "").trim();
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeamMessage message = TalkDataModel.instance().selectTeamMessage(messageId);
		if (message == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		List<TTeamMessageReaction> reactionList = TalkDataModel.instance().selectTeamMessageReactionListByMessageUser(messageId, userId);
		TTeamMessageReaction reaction = null;
		for (TTeamMessageReaction ireaction : reactionList) {
			if (ireaction.getReaction().equals(emojiContent)) {
				reaction = ireaction;
				break;
			}
		}
		if (reaction == null) {
			reaction = new TTeamMessageReaction();
			reaction.setDomainId(message.getDomainId());
			reaction.setTeamId(message.getTeamId());
			reaction.setMessageId(message.getMessageId());
			reaction.setUserId(userId);
			reaction.setReaction(emojiContent);
			TalkDataModel.instance().saveTeamMessageReaction(reaction);
		}
		else {
			TalkDataModel.instance().deleteTeamMessageReaction(reaction);
		}
		
		reactionList = TalkDataModel.instance().selectTeamMessageReactionListByMessage(messageId);
		
		JSONObject jsonMessage = message.toJsonObject();
		JSONArray jsonReactionList = new JSONArray();
		jsonMessage.put("reactionList", jsonReactionList);
		Map<String, JSONObject> reactionCounts = new HashMap<String, JSONObject>();
		for (TTeamMessageReaction ireaction : reactionList) {
			JSONObject reactionCount = reactionCounts.get(ireaction.getReaction());
			if (reactionCount == null) {
				reactionCount = new JSONObject();
				reactionCount.put("reaction", ireaction.getReaction());
				reactionCount.put("count", 0);
				reactionCounts.put(ireaction.getReaction(), reactionCount);
			}
			reactionCount.put("count", reactionCount.getInt("count") + 1);
		}
		for (JSONObject reactionCount : reactionCounts.values()) {
			jsonReactionList.put(reactionCount);
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("message", jsonMessage);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> replyTeamMessage(String req) throws Exception {
		return sendTeamMessage(req);
	}
	
	public static Future<String> revokeTeamMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeamMessage message = TalkDataModel.instance().selectTeamMessage(messageId);
		if (message == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (message.getUserId() != userId) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TalkDataModel.instance().deleteTeamMessage(message);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> removeTeamMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeamMessage message = TalkDataModel.instance().selectTeamMessage(messageId);
		if (message == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TDomain domain = TalkDataModel.instance().selectDomain(message.getDomainId());
		if (domain == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (domain.getUserId() != userId && !checkDomainPrivilege(message.getDomainId(), userId, EDomainPrivilege.ADMIN_DOMAIN.getValue())) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.PERMISSION_DENIED, lang)).toString()).complete();
		}
		
		TalkDataModel.instance().deleteTeamMessage(message);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadTeamMessageNotices(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		Map<Long, List<TTeamMessageNotice>> noticeLists = TalkDataModel.instance().selectTeamMessageNoticesByUser(userId);
		
		JSONArray jsonDomainNotices = new JSONArray();
		for (Map.Entry<Long, List<TTeamMessageNotice>> entry : noticeLists.entrySet()) {
			long domainId = entry.getKey();
			JSONObject jsonDomainNotice = new JSONObject();
			jsonDomainNotice.put("domainId", IDUtil.build(domainId));
			jsonDomainNotice.put("count", 0);
			JSONArray jsonTeamNotices = new JSONArray();
			jsonDomainNotice.put("teamNotices", jsonTeamNotices);
			for (TTeamMessageNotice notice : entry.getValue()) {
				if (notice.getCount() > 0) {
					jsonDomainNotice.put("count", jsonDomainNotice.getInt("count") + notice.getCount());
					JSONObject jsonTeamNotice = new JSONObject();
					jsonTeamNotice.put("teamId", IDUtil.build(notice.getTeamId()));
					jsonTeamNotice.put("count", notice.getCount());
					jsonTeamNotices.put(jsonTeamNotice);
				}
			}
			if (jsonDomainNotice.getInt("count") > 0) {
				jsonDomainNotices.put(jsonDomainNotice);
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("domainNotices", jsonDomainNotices);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> acceptTeamMessageNotice(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long teamId = IDUtil.parse(jsonReqBody.optString("teamId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TTeamMessageNotice notice = TalkDataModel.instance().selectTeamMessageNoticesByTeamUser(teamId, userId);
		if (notice != null && notice.getCount() > 0) {
			notice.setCount(0);
			TalkDataModel.instance().saveTeamMessageNotice(notice);
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	private static boolean checkDomainPrivilege(long domainId, long userId, int privilege) throws Exception {
		List<TDomainRoleUser> domainRoleUserList = TalkDataModel.instance().selectDomainRoleUserListByDomainUser(domainId, userId);
		if (domainRoleUserList.isEmpty()) {
			return false;
		}
		Set<Long> roleIds = new HashSet<Long>();
		for (TDomainRoleUser domainRoleUser : domainRoleUserList) {
			roleIds.add(domainRoleUser.getRoleId());
		}
		Map<Long, TDomainRole> roles = TalkDataModel.instance().selectDomainRoles(roleIds);
		for (TDomainRole role : roles.values()) {
			JSONArray jsonPrivileges = new JSONArray(role.getPrivileges());
			for (int i = 0; i < jsonPrivileges.length(); i++) {
				int iprivilege = jsonPrivileges.getInt(i);
				if (iprivilege == privilege) {
					return true;
				}
			}
		}
		return false;
	}
}
