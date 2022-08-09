package com.meta.community;

import com.meta.community.handler.AccountHandler;
import com.meta.community.handler.BaseHandler;
import com.meta.community.handler.ChatHandler;
import com.meta.community.handler.TalkHandler;
import com.suomee.csp.lib.future.Future;

public class CommunityServiceImp extends CommunityService {
	//base
	@Override
	public Future<String> loadLanguageList(String req) throws Exception {
		return BaseHandler.loadLanguageList(req);
	}
	
	@Override
	public Future<String> loadTagList(String req) throws Exception {
		return BaseHandler.loadTagList(req);
	}
	
	@Override
	public Future<String> loadChainList(String req) throws Exception {
		return BaseHandler.loadChainList(req);
	}
	
	@Override
	public Future<String> loadTokenList(String req) throws Exception {
		return BaseHandler.loadTokenList(req);
	}
	
	//account
	@Override
	public Future<String> client(String req) throws Exception {
		return AccountHandler.client(req);
	}
	
	@Override
	public Future<String> unique(String req) throws Exception {
		return AccountHandler.unique(req);
	}
	
	@Override
	public Future<String> sign(String req) throws Exception {
		return AccountHandler.sign(req);
	}
	
	@Override
	public Future<String> swap(String req) throws Exception {
		return AccountHandler.swap(req);
	}
	
	@Override
	public Future<String> init(String req) throws Exception {
		return AccountHandler.init(req);
	}
	
	@Override
	public Future<String> edit(String req) throws Exception {
		return AccountHandler.edit(req);
	}
	
	@Override
	public Future<String> quit(String req) throws Exception {
		return AccountHandler.quit(req);
	}
	
	@Override
	public Future<String> loadUserList(String req) throws Exception {
		return AccountHandler.loadUserList(req);
	}
	
	@Override
	public Future<String> loadUser(String req) throws Exception {
		return AccountHandler.loadUser(req);
	}
	
	@Override
	public Future<String> loadUserInfo(String req) throws Exception {
		return AccountHandler.loadUserInfo(req);
	}
	
	@Override
	public Future<String> loadUserNftList(String req) throws Exception {
		return AccountHandler.loadUserNftList(req);
	}
	
	//talk
	@Override
	public Future<String> exploreDomainList(String req) throws Exception {
		return TalkHandler.exploreDomainList(req);
	}
	
	@Override
	public Future<String> inviteDomainByFriend(String req) throws Exception {
		return TalkHandler.inviteDomainByFriend(req);
	}
	
	@Override
	public Future<String> inviteDomainByWhisper(String req) throws Exception {
		return TalkHandler.inviteDomainByWhisper(req);
	}
	
	@Override
	public Future<String> inviteDomainByDomain(String req) throws Exception {
		return TalkHandler.inviteDomainByDomain(req);
	}
	
	@Override
	public Future<String> kickDomainUser(String req) throws Exception {
		return TalkHandler.kickDomainUser(req);
	}
	
	@Override
	public Future<String> joinDomain(String req) throws Exception {
		return TalkHandler.joinDomain(req);
	}
	
	@Override
	public Future<String> leaveDomain(String req) throws Exception {
		return TalkHandler.leaveDomain(req);
	}
	
	@Override
	public Future<String> boostDomain(String req) throws Exception {
		return TalkHandler.boostDomain(req);
	}
	
	@Override
	public Future<String> buildDomainPoster(String req) throws Exception {
		return TalkHandler.buildDomainPoster(req);
	}
	
	@Override
	public Future<String> loadUserDomainList(String req) throws Exception {
		return TalkHandler.loadUserDomainList(req);
	}
	
	@Override
	public Future<String> loadUserMutualDomains(String req) throws Exception {
		return TalkHandler.loadUserMutualDomains(req);
	}
	
	@Override
	public Future<String> loadDomain(String req) throws Exception {
		return TalkHandler.loadDomain(req);
	}
	
	@Override
	public Future<String> loadDomainTeamList(String req) throws Exception {
		return TalkHandler.loadDomainTeamList(req);
	}
	
	@Override
	public Future<String> loadDomainRoleList(String req) throws Exception {
		return TalkHandler.loadDomainRoleList(req);
	}
	
	@Override
	public Future<String> loadDomainUserList(String req) throws Exception {
		return TalkHandler.loadDomainUserList(req);
	}
	
	@Override
	public Future<String> loadTeamMessageList(String req) throws Exception {
		return TalkHandler.loadTeamMessageList(req);
	}
	
	@Override
	public Future<String> loadTeamMessage(String req) throws Exception {
		return TalkHandler.loadTeamMessage(req);
	}
	
	@Override
	public Future<String> saveDomainRole(String req) throws Exception {
		return TalkHandler.saveDomainRole(req);
	}
	
	@Override
	public Future<String> addDomainRoleUser(String req) throws Exception {
		return TalkHandler.addDomainRoleUser(req);
	}
	
	@Override
	public Future<String> removeDomainRoleUser(String req) throws Exception {
		return TalkHandler.removeDomainRoleUser(req);
	}
	
	@Override
	public Future<String> createDomain(String req) throws Exception {
		return TalkHandler.createDomain(req);
	}
	
	@Override
	public Future<String> editDomain(String req) throws Exception {
		return TalkHandler.editDomain(req);
	}
	
	@Override
	public Future<String> deleteDomain(String req) throws Exception {
		return TalkHandler.deleteDomain(req);
	}
	
	@Override
	public Future<String> createCategory(String req) throws Exception {
		return TalkHandler.createCategory(req);
	}
	
	@Override
	public Future<String> editCategory(String req) throws Exception {
		return TalkHandler.editCategory(req);
	}
	
	@Override
	public Future<String> deleteCategory(String req) throws Exception {
		return TalkHandler.deleteCategory(req);
	}
	
	@Override
	public Future<String> createTeam(String req) throws Exception {
		return TalkHandler.createTeam(req);
	}
	
	@Override
	public Future<String> editTeam(String req) throws Exception {
		return TalkHandler.editTeam(req);
	}
	
	@Override
	public Future<String> deleteTeam(String req) throws Exception {
		return TalkHandler.deleteTeam(req);
	}
	
	@Override
	public Future<String> loadTeamLimitListByTeam(String req) throws Exception {
		return TalkHandler.loadTeamLimitListByTeam(req);
	}
	
	@Override
	public Future<String> saveTeamLimitList(String req) throws Exception {
		return TalkHandler.saveTeamLimitList(req);
	}
	
	@Override
	public Future<String> sendTeamMessage(String req) throws Exception {
		return TalkHandler.sendTeamMessage(req);
	}
	
	@Override
	public Future<String> editTeamMessage(String req) throws Exception {
		return TalkHandler.editTeamMessage(req);
	}
	
	@Override
	public Future<String> reactTeamMessage(String req) throws Exception {
		return TalkHandler.reactTeamMessage(req);
	}
	
	@Override
	public Future<String> replyTeamMessage(String req) throws Exception {
		return TalkHandler.replyTeamMessage(req);
	}
	
	@Override
	public Future<String> revokeTeamMessage(String req) throws Exception {
		return TalkHandler.revokeTeamMessage(req);
	}
	
	@Override
	public Future<String> removeTeamMessage(String req) throws Exception {
		return TalkHandler.removeTeamMessage(req);
	}
	
	@Override
	public Future<String> loadTeamMessageNotices(String req) throws Exception {
		return TalkHandler.loadTeamMessageNotices(req);
	}
	
	@Override
	public Future<String> acceptTeamMessageNotice(String req) throws Exception {
		return TalkHandler.acceptTeamMessageNotice(req);
	}
	
	//chat
	@Override
	public Future<String> loadUserFriendList(String req) throws Exception {
		return ChatHandler.loadUserFriendList(req);
	}
	
	@Override
	public Future<String> loadUserMutualFriends(String req) throws Exception {
		return ChatHandler.loadUserMutualFriends(req);
	}
	
	@Override
	public Future<String> inviteUserFriend(String req) throws Exception {
		return ChatHandler.inviteUserFriend(req);
	}
	
	@Override
	public Future<String> cancelUserFriend(String req) throws Exception {
		return ChatHandler.cancelUserFriend(req);
	}
	
	@Override
	public Future<String> acceptUserFriend(String req) throws Exception {
		return ChatHandler.acceptUserFriend(req);
	}
	
	@Override
	public Future<String> loadUserFriendNotice(String req) throws Exception {
		return ChatHandler.loadUserFriendNotice(req);
	}
	
	@Override
	public Future<String> acceptUserFriendNotice(String req) throws Exception {
		return ChatHandler.acceptUserFriendNotice(req);
	}
	
	@Override
	public Future<String> rejectUserFriend(String req) throws Exception {
		return ChatHandler.rejectUserFriend(req);
	}
	
	@Override
	public Future<String> removeUserFriend(String req) throws Exception {
		return ChatHandler.removeUserFriend(req);
	}
	
	@Override
	public Future<String> loadUserWhisperList(String req) throws Exception {
		return ChatHandler.loadUserWhisperList(req);
	}
	
	@Override
	public Future<String> promiseWhisper(String req) throws Exception {
		return ChatHandler.promiseWhisper(req);
	}
	
	@Override
	public Future<String> createWhisper(String req) throws Exception {
		return ChatHandler.createWhisper(req);
	}
	
	@Override
	public Future<String> deleteWhisper(String req) throws Exception {
		return ChatHandler.deleteWhisper(req);
	}
	
	@Override
	public Future<String> loadWhisperUserList(String req) throws Exception {
		return ChatHandler.loadWhisperUserList(req);
	}
	
	@Override
	public Future<String> joinWhisperUsers(String req) throws Exception {
		return ChatHandler.joinWhisperUsers(req);
	}
	
	@Override
	public Future<String> kickWhisperUsers(String req) throws Exception {
		return ChatHandler.kickWhisperUsers(req);
	}
	
	@Override
	public Future<String> enterWhisperUser(String req) throws Exception {
		return ChatHandler.enterWhisperUser(req);
	}
	
	@Override
	public Future<String> leaveWhisperUser(String req) throws Exception {
		return ChatHandler.leaveWhisperUser(req);
	}
	
	@Override
	public Future<String> loadWhisperMessageList(String req) throws Exception {
		return ChatHandler.loadWhisperMessageList(req);
	}
	
	@Override
	public Future<String> loadWhisperMessage(String req) throws Exception {
		return ChatHandler.loadWhisperMessage(req);
	}
	
	@Override
	public Future<String> sendWhisperMessage(String req) throws Exception {
		return ChatHandler.sendWhisperMessage(req);
	}
	
	@Override
	public Future<String> editWhisperMessage(String req) throws Exception {
		return ChatHandler.editWhisperMessage(req);
	}
	
	@Override
	public Future<String> reactWhisperMessage(String req) throws Exception {
		return ChatHandler.reactWhisperMessage(req);
	}
	
	@Override
	public Future<String> replyWhisperMessage(String req) throws Exception {
		return ChatHandler.replyWhisperMessage(req);
	}
	
	@Override
	public Future<String> revokeWhisperMessage(String req) throws Exception {
		return ChatHandler.revokeWhisperMessage(req);
	}
	
	@Override
	public Future<String> loadWhisperMessageNotices(String req) throws Exception {
		return ChatHandler.loadWhisperMessageNotices(req);
	}
	
	@Override
	public Future<String> acceptWhisperMessageNotice(String req) throws Exception {
		return ChatHandler.acceptWhisperMessageNotice(req);
	}
}
