package com.meta.community.handler;

import java.util.Arrays;
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
import com.meta.community.data.account.TUser;
import com.meta.community.data.chat.ChatDataModel;
import com.meta.community.data.chat.EChatMessageType;
import com.meta.community.data.chat.EFriendStatus;
import com.meta.community.data.chat.TUserFriend;
import com.meta.community.data.chat.TUserFriendNotice;
import com.meta.community.data.chat.TWhisper;
import com.meta.community.data.chat.TWhisperMessage;
import com.meta.community.data.chat.TWhisperMessageNotice;
import com.meta.community.data.chat.TWhisperMessageReaction;
import com.meta.community.data.chat.TWhisperUser;
import com.meta.community.data.talk.TDomain;
import com.meta.community.data.talk.TalkDataModel;
import com.meta.community.lang.BaseLang;
import com.meta.community.lang.ChatLang;
import com.meta.community.lang.ELang;
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
import com.suomee.csp.lib.util.StringUtil;

public class ChatHandler {
	public static Future<String> loadUserFriendList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final int friendStatus = jsonReqBody.optInt("friendStatus", 0);
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TUserFriend> userFriendList = ChatDataModel.instance().selectUserFriendListByUser(userId, friendStatus);
		
		Set<Long> userIds = new HashSet<Long>();
		for (TUserFriend userFriend : userFriendList) {
			userIds.add(userFriend.getFriendId());
		}
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		
		JSONArray jsonUserFriendList = new JSONArray();
		for (TUserFriend userFriend : userFriendList) {
			jsonUserFriendList.put(userFriend.toJsonObject());
		}
		JSONArray jsonUsers = new JSONArray();
		for (TUser user : users.values()) {
			jsonUsers.put(user.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("userFriendList", jsonUserFriendList);
		jsonRspBody.put("users", jsonUsers);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> loadUserMutualFriends(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		Map<Long, List<TUserFriend>> userFriendLists = ChatDataModel.instance().selectUserFriendListsByUsers(
				new HashSet<Long>(Arrays.asList(userId, friendId)));
		
		Set<Long> userIds = new HashSet<Long>();
		List<TUserFriend> userFriendList = userFriendLists.get(userId);
		List<TUserFriend> friendFriendList = userFriendLists.get(friendId);
		if (userFriendList != null && friendFriendList != null) {
			Set<Long> userFriendIds = new HashSet<Long>();
			for (TUserFriend userFriend : userFriendList) {
				userFriendIds.add(userFriend.getFriendId());
			}
			for (TUserFriend friendFriend : friendFriendList) {
				if (userFriendIds.contains(friendFriend.getFriendId())) {
					userIds.add(friendFriend.getFriendId());
				}
			}
		}
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		
		JSONArray jsonUsers = new JSONArray();
		for (TUser user : users.values()) {
			jsonUsers.put(user.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("users", jsonUsers);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> inviteUserFriend(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		if (friendId == userId) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser friend = AccountDataModel.instance().selectUser(friendId);
		if (friend == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend userFriend = ChatDataModel.instance().selectUserFriendByUserFriend(userId, friendId);
		if (userFriend == null) {
			userFriend = new TUserFriend();
			userFriend.setUserId(userId);
			userFriend.setFriendId(friendId);
			userFriend.setBuildTime(DateTimeUtil.seconds());
			userFriend.setFriendStatus(EFriendStatus.INVITING.getValue());
			ChatDataModel.instance().saveUserFriend(userFriend);
		}
		else {
			if (userFriend.getFriendStatus() != EFriendStatus.INVITING.getValue() && userFriend.getFriendStatus() != EFriendStatus.ACCEPT.getValue()) {
				userFriend.setBuildTime(DateTimeUtil.seconds());
				userFriend.setFriendStatus(EFriendStatus.INVITING.getValue());
				ChatDataModel.instance().saveUserFriend(userFriend);
			}
		}
		
		boolean invited = false;
		TUserFriend friendUser = ChatDataModel.instance().selectUserFriendByUserFriend(friendId, userId);
		if (friendUser == null) {
			friendUser = new TUserFriend();
			friendUser.setUserId(friendId);
			friendUser.setFriendId(userId);
			friendUser.setBuildTime(DateTimeUtil.seconds());
			friendUser.setFriendStatus(EFriendStatus.INVITED.getValue());
			ChatDataModel.instance().saveUserFriend(friendUser);
			invited = true;
		}
		else {
			if (friendUser.getFriendStatus() != EFriendStatus.INVITED.getValue() && friendUser.getFriendStatus() != EFriendStatus.ACCEPT.getValue()) {
				friendUser.setBuildTime(DateTimeUtil.seconds());
				friendUser.setFriendStatus(EFriendStatus.INVITED.getValue());
				ChatDataModel.instance().saveUserFriend(friendUser);
				invited = true;
			}
		}
		
		if (invited) {
			TUserFriendNotice notice = ChatDataModel.instance().selectUserFriendNoticeByUser(friendId);
			if (notice == null) {
				notice = new TUserFriendNotice();
				notice.setUserId(friendId);
				notice.setCount(1);
				ChatDataModel.instance().saveUserFriendNotice(notice);
				TUserFriendNotice eNotice = ChatDataModel.instance().selectUserFriendNoticeByUser(friendId);
				if (eNotice != null && eNotice.getNoticeId() != notice.getNoticeId()) {
					ChatDataModel.instance().updateUserFriendNotice(eNotice, 1, notice);
				}
			}
			else {
				ChatDataModel.instance().updateUserFriendNotice(notice, 1, null);
				notice.setCount(notice.getCount() + 1);
			}
			
			Set<Long> accounts = new HashSet<Long>();
			accounts.add(friendId);
			PushData data = new PushData();
			data.setDataType(EDataType.FRIEND_INVITE.getValue());
			JSONObject jsonUserFriend = friendUser.toJsonObject();
			jsonUserFriend.put("friend", user.toJsonObject());
			data.setDataContent(jsonUserFriend.toString());
			ProxyFactory.getProxy(PushServiceProxy.class).push(new PushReq().setAccounts(accounts).setData(data)).ready(new FutureHandler<PushRsp>() {
				@Override
				public void complete(Future<PushRsp> pushRspFuture) throws Exception {
					if (!pushRspFuture.isSuccess()) {
						Logger.getLogger("error").error("ChatHandler.inviteUserFriend push business exception.", pushRspFuture.getException());
						return;
					}
					PushRsp pushRsp = pushRspFuture.getResult();
					if (pushRsp.getCode() != EResultCode.OK) {
						Logger.getLogger("error").error("ChatHandler.inviteUserFriend push failed. code=" + pushRsp.getCode() + ", message=" + pushRsp.getMessage());
					}
				}
				@Override
				public void exception(CspException e) {
					Logger.getLogger("error").error("ChatHandler.inviteUserFriend push invoke exception.", e);
				}
			});
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> cancelUserFriend(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		if (friendId == userId) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		TUser user = AccountDataModel.instance().selectUser(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TUser friend = AccountDataModel.instance().selectUser(friendId);
		if (friend == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend userFriend = ChatDataModel.instance().selectUserFriendByUserFriend(userId, friendId);
		if (userFriend == null) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		else {
			if (userFriend.getFriendStatus() != EFriendStatus.INVITING.getValue()) {
				return rspFuture.setResult(Service.headonly(13, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
			}
		}
		
		TUserFriend friendUser = ChatDataModel.instance().selectUserFriendByUserFriend(friendId, userId);
		if (friendUser == null) {
			return rspFuture.setResult(Service.headonly(14, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		else {
			if (friendUser.getFriendStatus() != EFriendStatus.INVITED.getValue()) {
				return rspFuture.setResult(Service.headonly(15, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
			}
		}
		
		userFriend.setFriendStatus(EFriendStatus.CANCEL.getValue());
		ChatDataModel.instance().saveUserFriend(userFriend);
		
		friendUser.setFriendStatus(EFriendStatus.CANCEL.getValue());
		ChatDataModel.instance().saveUserFriend(friendUser);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> acceptUserFriend(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TUser friend = AccountDataModel.instance().selectUser(friendId);
		if (friend == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend userFriend = ChatDataModel.instance().selectUserFriendByUserFriend(userId, friendId);
		if (userFriend == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (userFriend.getFriendStatus() != EFriendStatus.INVITED.getValue()) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend friendUser = ChatDataModel.instance().selectUserFriendByUserFriend(friendId, userId);
		if (friendUser == null) {
			return rspFuture.setResult(Service.headonly(13, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (friendUser.getFriendStatus() != EFriendStatus.INVITING.getValue()) {
			return rspFuture.setResult(Service.headonly(14, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		userFriend.setBuildTime(DateTimeUtil.seconds());
		userFriend.setFriendStatus(EFriendStatus.ACCEPT.getValue());
		ChatDataModel.instance().saveUserFriend(userFriend);
		friendUser.setBuildTime(DateTimeUtil.seconds());
		friendUser.setFriendStatus(EFriendStatus.ACCEPT.getValue());
		ChatDataModel.instance().saveUserFriend(friendUser);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadUserFriendNotice(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		TUserFriendNotice notice = ChatDataModel.instance().selectUserFriendNoticeByUser(userId);
		
		JSONObject jsonNotice = new JSONObject();
		jsonNotice.put("userId", IDUtil.build(userId));
		jsonNotice.put("count", notice != null ? notice.getCount() : 0);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("notice", jsonNotice);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> acceptUserFriendNotice(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		TUserFriendNotice notice = ChatDataModel.instance().selectUserFriendNoticeByUser(userId);
		if (notice != null && notice.getCount() > 0) {
			notice.setCount(0);
			ChatDataModel.instance().saveUserFriendNotice(notice);
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> rejectUserFriend(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TUser friend = AccountDataModel.instance().selectUser(friendId);
		if (friend == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend userFriend = ChatDataModel.instance().selectUserFriendByUserFriend(userId, friendId);
		if (userFriend == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (userFriend.getFriendStatus() != EFriendStatus.INVITED.getValue()) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend friendUser = ChatDataModel.instance().selectUserFriendByUserFriend(friendId, userId);
		if (friendUser == null) {
			return rspFuture.setResult(Service.headonly(13, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (friendUser.getFriendStatus() != EFriendStatus.INVITING.getValue()) {
			return rspFuture.setResult(Service.headonly(14, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		userFriend.setBuildTime(DateTimeUtil.seconds());
		userFriend.setFriendStatus(EFriendStatus.REJECT.getValue());
		ChatDataModel.instance().saveUserFriend(userFriend);
		friendUser.setBuildTime(DateTimeUtil.seconds());
		friendUser.setFriendStatus(EFriendStatus.REJECT.getValue());
		ChatDataModel.instance().saveUserFriend(friendUser);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> removeUserFriend(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long friendId = IDUtil.parse(jsonReqBody.optString("friendId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TUser friend = AccountDataModel.instance().selectUser(friendId);
		if (friend == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend userFriend = ChatDataModel.instance().selectUserFriendByUserFriend(userId, friendId);
		if (userFriend == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (userFriend.getFriendStatus() != EFriendStatus.ACCEPT.getValue()) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TUserFriend friendUser = ChatDataModel.instance().selectUserFriendByUserFriend(friendId, userId);
		if (friendUser == null) {
			return rspFuture.setResult(Service.headonly(13, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (friendUser.getFriendStatus() != EFriendStatus.ACCEPT.getValue()) {
			return rspFuture.setResult(Service.headonly(14, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		userFriend.setBuildTime(DateTimeUtil.seconds());
		userFriend.setFriendStatus(EFriendStatus.REMOVE.getValue());
		ChatDataModel.instance().saveUserFriend(userFriend);
		friendUser.setBuildTime(DateTimeUtil.seconds());
		friendUser.setFriendStatus(EFriendStatus.REMOVE.getValue());
		ChatDataModel.instance().saveUserFriend(friendUser);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadUserWhisperList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByUser(userId);
		Set<Long> whisperIds = new HashSet<Long>();
		for (TWhisperUser whisperUser : whisperUserList) {
			whisperIds.add(whisperUser.getWhisperId());
		}
		Map<Long, TWhisper> whispers = ChatDataModel.instance().selectWhispers(whisperIds);
		Set<Long> userIds = new HashSet<Long>();
		for (TWhisper whisper : whispers.values()) {
			if (whisper.getMemberId() > 0L) {
				userIds.add(whisper.getUserId());
				userIds.add(whisper.getMemberId());
			}
		}
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		Map<Long, Long> messageTimes = ChatDataModel.instance().selectWhisperFinalMessageTimes(whisperIds);
		List<TWhisper> whisperList = new LinkedList<TWhisper>(whispers.values());
		whisperList.sort(new Comparator<TWhisper>() {
			@Override
			public int compare(TWhisper whisper1, TWhisper whisper2) {
				Long time1 = messageTimes.get(whisper1.getWhisperId());
				Long time2 = messageTimes.get(whisper2.getWhisperId());
				if (time1 != null) {
					if (time2 != null) {
						if (time1 < time2) {
							return 1;
						}
						else if (time1 > time2) {
							return -1;
						}
						else {
							return 0;
						}
					}
					else {
						return -1;
					}
				}
				else {
					if (time2 != null) {
						return 1;
					}
					else {
						if (whisper1.getBuildTime() < whisper2.getBuildTime()) {
							return 1;
						}
						else if (whisper1.getBuildTime() > whisper2.getBuildTime()) {
							return -1;
						}
						else {
							return 0;
						}
					}
				}
			}
		});
		
		JSONArray jsonWhisperList = new JSONArray();
		for (TWhisper whisper : whisperList) {
			JSONObject jsonWhisper = whisper.toJsonObject();
			if (whisper.getMemberId() > 0L) {
				if (whisper.getMemberId() == userId) {
					jsonWhisper.put("user", users.get(whisper.getMemberId()).toJsonObject());
					jsonWhisper.put("member", users.get(whisper.getUserId()).toJsonObject());
				}
				else {
					jsonWhisper.put("user", users.get(whisper.getUserId()).toJsonObject());
					jsonWhisper.put("member", users.get(whisper.getMemberId()).toJsonObject());
				}
			}
			jsonWhisperList.put(jsonWhisper);
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("whisperList", jsonWhisperList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> promiseWhisper(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long memberId = IDUtil.parse(jsonReqBody.optString("member", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		if (memberId <= 0L) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		TWhisper whisper = ChatDataModel.instance().selectWhisperByUserMember(userId, memberId);
		if (whisper == null) {
			whisper = new TWhisper();
			whisper.setUserId(userId);
			whisper.setMemberId(memberId);
			whisper.setBuildTime(DateTimeUtil.seconds());
			whisper.setMemberCount(2);
			whisper.setName("");
			
			List<TWhisperUser> whisperUserList = new LinkedList<TWhisperUser>();
			TWhisperUser whisperUser = new TWhisperUser();
			whisperUser.setUserId(userId);
			whisperUser.setBuildTime(DateTimeUtil.seconds());
			whisperUserList.add(whisperUser);
			whisperUser = new TWhisperUser();
			whisperUser.setUserId(memberId);
			whisperUser.setBuildTime(DateTimeUtil.seconds());
			whisperUserList.add(whisperUser);
			
			ChatDataModel.instance().saveWhisper(whisper, whisperUserList);
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("whisper", whisper.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> createWhisper(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		JSONArray jsonMembers = jsonReqBody.optJSONArray("members");
		
		final Future<String> rspFuture = new Future<String>();
		
		if (jsonMembers == null || jsonMembers.length() == 0) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		Set<Long> memberIds = new HashSet<Long>();
		for (int i = 0; i < jsonMembers.length(); i++) {
			memberIds.add(IDUtil.parse(jsonMembers.optString(i, "")));
		}
		memberIds.add(userId);
		Map<Long, TUser> members = AccountDataModel.instance().selectUsers(memberIds);
		
		StringBuilder nameBuilder = new StringBuilder();
		int nameCount = 0;
		
		TWhisper whisper = new TWhisper();
		whisper.setUserId(userId);
		whisper.setMemberId(0L);
		whisper.setBuildTime(DateTimeUtil.seconds());
		List<TWhisperUser> whisperUserList = new LinkedList<TWhisperUser>();
		for (int i = 0; i < jsonMembers.length(); i++) {
			long memberId = IDUtil.parse(jsonMembers.optString(i, ""));
			TUser member = members.get(memberId);
			if (member == null) {
				return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
			}
			TWhisperUser whisperUser = new TWhisperUser();
			whisperUser.setUserId(memberId);
			whisperUser.setBuildTime(DateTimeUtil.seconds());
			whisperUserList.add(whisperUser);
			if (nameCount < 3) {
				if (nameCount > 0) {
					nameBuilder.append("、");
				}
				nameBuilder.append(member.getNickname());
				nameCount++;
			}
		}
		TUser user = members.get(userId);
		if (user == null) {
			return rspFuture.setResult(Service.headonly(13, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TWhisperUser whisperUser = new TWhisperUser();
		whisperUser.setUserId(userId);
		whisperUser.setBuildTime(DateTimeUtil.seconds());
		whisperUserList.add(whisperUser);
		if (nameCount < 3) {
			if (nameCount > 0) {
				nameBuilder.append("、");
			}
			nameBuilder.append(user.getNickname());
			nameCount++;
		}
		
		if (nameCount > 3) {
			nameBuilder.append("、...");
		}
		whisper.setMemberCount(memberIds.size());
		whisper.setName(nameBuilder.toString());
		ChatDataModel.instance().saveWhisper(whisper, whisperUserList);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("whisper", whisper.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> deleteWhisper(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisper whisper = ChatDataModel.instance().selectWhisper(whisperId);
		if (whisper == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (whisper.getUserId() != userId) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByWhisper(whisperId);
		ChatDataModel.instance().deleteWhisper(whisper, whisperUserList);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadWhisperUserList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByWhisper(whisperId);
		Set<Long> userIds = new HashSet<Long>();
		for (TWhisperUser whisperUser : whisperUserList) {
			userIds.add(whisperUser.getUserId());
		}
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		List<TUser> userList = new LinkedList<TUser>(users.values());
		userList.sort(new Comparator<TUser>() {
			@Override
			public int compare(TUser u1, TUser u2) {
				return u1.getNickname().compareTo(u2.getNickname());
			}
		});
		
		JSONArray jsonUserList = new JSONArray();
		for (TUser user : userList) {
			jsonUserList.put(user.toJsonObject());
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("userList", jsonUserList);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> joinWhisperUsers(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		final JSONArray jsonUserIds = jsonReqBody.optJSONArray("userIds");
		
		final Future<String> rspFuture = new Future<String>();
		
		if (jsonUserIds == null || jsonUserIds.length() == 0) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		
		TWhisper whisper = ChatDataModel.instance().selectWhisper(whisperId);
		if (whisper == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByWhisper(whisperId);
		Map<Long, TWhisperUser> whisperUsers = new HashMap<Long, TWhisperUser>();
		for (TWhisperUser whisperUser : whisperUserList) {
			whisperUsers.put(whisperUser.getUserId(), whisperUser);
		}
		
		for (int i = 0; i < jsonUserIds.length(); i++) {
			long userId = IDUtil.parse(jsonUserIds.getString(i));
			if (userId <= 0L) {
				continue;
			}
			TWhisperUser whisperUser = whisperUsers.get(userId);
			if (whisperUser == null) {
				whisperUser = new TWhisperUser();
				whisperUser.setWhisperId(whisperId);
				whisperUser.setUserId(userId);
				whisperUser.setBuildTime(DateTimeUtil.seconds());
				ChatDataModel.instance().saveWhisperUser(whisperUser);
				ChatDataModel.instance().updateWhisper(whisper, 1, whisperUser, null);
				whisperUsers.put(whisperUser.getUserId(), whisperUser);
				whisper.setMemberCount(whisper.getMemberCount() + 1);
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("whisper", whisper.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> kickWhisperUsers(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		final JSONArray jsonUserIds = jsonReqBody.optJSONArray("userIds");
		
		final Future<String> rspFuture = new Future<String>();
		
		if (jsonUserIds == null || jsonUserIds.length() == 0) {
			return rspFuture.setResult(Service.headonly().toString()).complete();
		}
		
		TWhisper whisper = ChatDataModel.instance().selectWhisper(whisperId);
		if (whisper == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByWhisper(whisperId);
		Map<Long, TWhisperUser> whisperUsers = new HashMap<Long, TWhisperUser>();
		for (TWhisperUser whisperUser : whisperUserList) {
			whisperUsers.put(whisperUser.getUserId(), whisperUser);
		}
		
		for (int i = 0; i < jsonUserIds.length(); i++) {
			long userId = IDUtil.parse(jsonUserIds.getString(i));
			if (userId <= 0L) {
				continue;
			}
			TWhisperUser whisperUser = whisperUsers.get(userId);
			if (whisperUser != null) {
				ChatDataModel.instance().deleteWhisperUser(whisperUser);
				ChatDataModel.instance().updateWhisper(whisper, -1, whisperUser, null);
				whisper.setMemberCount(whisper.getMemberCount() - 1);
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("whisper", whisper.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> enterWhisperUser(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisper whisper = ChatDataModel.instance().selectWhisper(whisperId);
		if (whisper == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByWhisper(whisperId);
		boolean exist = false;
		for (TWhisperUser whisperUser : whisperUserList) {
			if (whisperUser.getUserId() == userId) {
				exist = true;
				break;
			}
		}
		
		if (!exist) {
			TWhisperUser whisperUser = new TWhisperUser();
			whisperUser.setWhisperId(whisperId);
			whisperUser.setUserId(userId);
			whisperUser.setBuildTime(DateTimeUtil.seconds());
			ChatDataModel.instance().saveWhisperUser(whisperUser);
			ChatDataModel.instance().updateWhisper(whisper, 1, whisperUser, null);
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> leaveWhisperUser(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisper whisper = ChatDataModel.instance().selectWhisper(whisperId);
		if (whisper == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		TWhisperUser whisperUser = ChatDataModel.instance().selectWhisperUserByWhisperUser(whisperId, userId);
		if (whisperUser == null) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		ChatDataModel.instance().deleteWhisperUser(whisperUser);
		ChatDataModel.instance().updateWhisper(whisper, -1, null, whisperUser);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadWhisperMessageList(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		final long fromId = IDUtil.parse(jsonReqBody.optString("fromId", ""));
		final long toId = IDUtil.parse(jsonReqBody.optString("toId", ""));
		final int size = jsonReqBody.optInt("size", 50);
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TWhisperMessage> messageList = ChatDataModel.instance().selectWhisperMessageList(whisperId, fromId, toId, size);
		
		Set<Long> messageIds = new HashSet<Long>();
		Set<Long> userIds = new HashSet<Long>();
		Set<Long> domainIds = new HashSet<Long>();
		Set<Long> replyMessageIds = new HashSet<Long>();
		for (TWhisperMessage message : messageList) {
			messageIds.add(message.getMessageId());
			userIds.add(message.getUserId());
			if (message.getInviteDomainId() > 0L) {
				domainIds.add(message.getInviteDomainId());
			}
			if (message.getReplyMessageId() > 0L) {
				replyMessageIds.add(message.getReplyMessageId());
			}
		}
		Map<Long, TWhisperMessage> replyMessages = ChatDataModel.instance().selectWhisperMessages(replyMessageIds);
		for (TWhisperMessage replyMessage : replyMessages.values()) {
			userIds.add(replyMessage.getUserId());
			if (replyMessage.getInviteDomainId() > 0L) {
				domainIds.add(replyMessage.getInviteDomainId());
			}
		}
		Map<Long, List<TWhisperMessageReaction>> reactionLists = ChatDataModel.instance().selectWhisperMessageReactionListsByMessages(messageIds);
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
		
		JSONArray jsonMessageList = new JSONArray();
		for (TWhisperMessage message : messageList) {
			JSONObject jsonMessage = message.toJsonObject();
			JSONArray jsonReactionList = new JSONArray();
			jsonMessage.put("reactionList", jsonReactionList);
			jsonMessageList.put(jsonMessage);
			
			Map<String, JSONObject> reactionCounts = new HashMap<String, JSONObject>();
			List<TWhisperMessageReaction> reactionList = reactionLists.get(message.getMessageId());
			if (reactionList != null) {
				for (TWhisperMessageReaction reaction : reactionList) {
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
		for (TDomain domain : domains.values()) {
			jsonDomains.put(domain.toJsonObject());
		}
		JSONArray jsonReplyMessages = new JSONArray();
		for (TWhisperMessage replyMessage : replyMessages.values()) {
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
	
	public static Future<String> loadWhisperMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisperMessage message = ChatDataModel.instance().selectWhisperMessage(messageId);
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
		TWhisperMessage replyMessage = null;
		if (message.getReplyMessageId() > 0L) {
			replyMessage = ChatDataModel.instance().selectWhisperMessage(message.getReplyMessageId());
			if (replyMessage != null) {
				userIds.add(replyMessage.getUserId());
				if (replyMessage.getInviteDomainId() > 0L) {
					domainIds.add(replyMessage.getInviteDomainId());
				}
			}
		}
		List<TWhisperMessageReaction> reactionList = ChatDataModel.instance().selectWhisperMessageReactionListByMessage(messageId);
		Map<Long, TUser> users = AccountDataModel.instance().selectUsers(userIds);
		Map<Long, TDomain> domains = TalkDataModel.instance().selectDomains(domainIds);
		
		JSONObject jsonMessage = message.toJsonObject();
		JSONArray jsonReactionList = new JSONArray();
		jsonMessage.put("reactionList", jsonReactionList);
		Map<String, JSONObject> reactionCounts = new HashMap<String, JSONObject>();
		for (TWhisperMessageReaction reaction : reactionList) {
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
	
	public static Future<String> sendWhisperMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final String client = jsonReqHead.optString("client", "");
		final String series = jsonReqHead.optString("series", "");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		final int messageType = jsonReqBody.optInt("messageType", 0);
		final String textContent = jsonReqBody.optString("textContent", "").trim();
		final String url = jsonReqBody.optString("url", "").trim();
		final long replyMessageId = IDUtil.parse(jsonReqBody.optString("replyMessageId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisper whisper = ChatDataModel.instance().selectWhisper(whisperId);
		if (whisper == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (EChatMessageType.toEnum(messageType) == null) {
			return rspFuture.setResult(Service.headonly(12, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (messageType == EChatMessageType.TEXT.getValue() && StringUtil.isNullOrEmpty(textContent)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(ChatLang.MESSAGE_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		
		TWhisperMessage message = new TWhisperMessage();
		message.setWhisperId(whisperId);
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
		ChatDataModel.instance().saveWhisperMessage(message);
		
		Map<Long, TWhisperMessageNotice> notices = ChatDataModel.instance().selectWhisperMessageNoticesByWhisper(whisperId);
		List<TWhisperUser> whisperUserList = ChatDataModel.instance().selectWhisperUserListByWhisper(whisperId);
		for (TWhisperUser whisperUser : whisperUserList) {
			TWhisperMessageNotice notice = notices.get(whisperUser.getUserId());
			if (notice == null) {
				notice = new TWhisperMessageNotice();
				notice.setWhisperId(whisperId);
				notice.setUserId(whisperUser.getUserId());
				notice.setCount(1);
				ChatDataModel.instance().saveWhisperMessageNotice(notice);
				TWhisperMessageNotice eNotice = ChatDataModel.instance().selectWhisperMessageNoticeByWhisperUser(whisperId, whisperUser.getUserId());
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
					Logger.getLogger("error").error("ChatHandler.sendTeamMessage push business exception.", pushRspFuture.getException());
					return;
				}
				PushRsp pushRsp = pushRspFuture.getResult();
				if (pushRsp.getCode() != EResultCode.OK) {
					Logger.getLogger("error").error("ChatHandler.sendTeamMessage push failed. code=" + pushRsp.getCode() + ", message=" + pushRsp.getMessage());
				}
			}
			@Override
			public void exception(CspException e) {
				Logger.getLogger("error").error("ChatHandler.sendTeamMessage push invoke exception.", e);
			}
		});
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> editWhisperMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		final String textContent = jsonReqBody.optString("textContent", "").trim();
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisperMessage message = ChatDataModel.instance().selectWhisperMessage(messageId);
		if (message == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (message.getUserId() != userId) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		if (message.getMessageType() == EChatMessageType.TEXT.getValue() && StringUtil.isNullOrEmpty(textContent)) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(ChatLang.MESSAGE_CAN_NOT_BE_EMPTY, lang)).toString()).complete();
		}
		
		message.setTextContent(textContent);
		message.setEdited(EBoolean.YES.getValue());
		ChatDataModel.instance().saveWhisperMessage(message);
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("message", message.toJsonObject());
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> reactWhisperMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		final String emojiContent = jsonReqBody.optString("emojiContent", "").trim();
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisperMessage message = ChatDataModel.instance().selectWhisperMessage(messageId);
		if (message == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		List<TWhisperMessageReaction> reactionList = ChatDataModel.instance().selectWhisperMessageReactionListByMessageUser(messageId, userId);
		TWhisperMessageReaction reaction = null;
		for (TWhisperMessageReaction ireaction : reactionList) {
			if (ireaction.getReaction().equals(emojiContent)) {
				reaction = ireaction;
				break;
			}
		}
		if (reaction == null) {
			reaction = new TWhisperMessageReaction();
			reaction.setWhisperId(message.getWhisperId());
			reaction.setMessageId(message.getMessageId());
			reaction.setUserId(userId);
			reaction.setReaction(emojiContent);
			ChatDataModel.instance().saveWhisperMessageReaction(reaction);
		}
		else {
			ChatDataModel.instance().deleteWhisperMessageReaction(reaction);
		}
		
		reactionList = ChatDataModel.instance().selectWhisperMessageReactionListByMessage(messageId);
		
		JSONObject jsonMessage = message.toJsonObject();
		JSONArray jsonReactionList = new JSONArray();
		jsonMessage.put("reactionList", jsonReactionList);
		Map<String, JSONObject> reactionCounts = new HashMap<String, JSONObject>();
		for (TWhisperMessageReaction ireaction : reactionList) {
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
	
	public static Future<String> replyWhisperMessage(String req) throws Exception {
		return sendWhisperMessage(req);
	}
	
	public static Future<String> revokeWhisperMessage(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final String lang = jsonReqHead.optString("lang", ELang.EN.getCode());
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long messageId = IDUtil.parse(jsonReqBody.optString("messageId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisperMessage message = ChatDataModel.instance().selectWhisperMessage(messageId);
		if (message == null) {
			return rspFuture.setResult(Service.headonly(10, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		if (message.getUserId() != userId) {
			return rspFuture.setResult(Service.headonly(11, Lang.content(BaseLang.INVALID_PARAMETER, lang)).toString()).complete();
		}
		
		ChatDataModel.instance().deleteWhisperMessage(message);
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
	
	public static Future<String> loadWhisperMessageNotices(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		
		final Future<String> rspFuture = new Future<String>();
		
		List<TWhisperMessageNotice> noticeList = ChatDataModel.instance().selectWhisperMessageNoticesByUser(userId);
		
		JSONArray jsonNotices = new JSONArray();
		for (TWhisperMessageNotice notice : noticeList) {
			if (notice.getCount() > 0) {
				JSONObject jsonNotice = new JSONObject();
				jsonNotice.put("whisperId", IDUtil.build(notice.getWhisperId()));
				jsonNotice.put("count", notice.getCount());
				jsonNotices.put(jsonNotice);
			}
		}
		
		JSONObject jsonRsp = Service.headonly();
		JSONObject jsonRspBody = jsonRsp.getJSONObject("body");
		jsonRspBody.put("notices", jsonNotices);
		return rspFuture.setResult(jsonRsp.toString()).complete();
	}
	
	public static Future<String> acceptWhisperMessageNotice(String req) throws Exception {
		JSONObject jsonReq = new JSONObject(req);
		JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		final long userId = jsonReqHead.optLong("userId", 0L);
		JSONObject jsonReqBody = jsonReq.getJSONObject("body");
		final long whisperId = IDUtil.parse(jsonReqBody.optString("whisperId", ""));
		
		final Future<String> rspFuture = new Future<String>();
		
		TWhisperMessageNotice notice = ChatDataModel.instance().selectWhisperMessageNoticeByWhisperUser(whisperId, userId);
		if (notice != null && notice.getCount() > 0) {
			notice.setCount(0);
			ChatDataModel.instance().saveWhisperMessageNotice(notice);
		}
		
		return rspFuture.setResult(Service.headonly().toString()).complete();
	}
}
