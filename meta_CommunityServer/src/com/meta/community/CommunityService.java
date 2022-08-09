package com.meta.community;

import com.suomee.csp.lib.future.Future;

public abstract class CommunityService extends AbstractService {
	@Override
	protected String name() {
		return Names.COMMUNITY_SVC_ANME;
	}
	
	//base
	public abstract Future<String> loadLanguageList(String req) throws Exception;
	
	public abstract Future<String> loadTagList(String req) throws Exception;
	
	public abstract Future<String> loadChainList(String req) throws Exception;
	
	public abstract Future<String> loadTokenList(String req) throws Exception;
	
	//account
	public abstract Future<String> client(String req) throws Exception;
	
	public abstract Future<String> unique(String req) throws Exception;
	
	public abstract Future<String> sign(String req) throws Exception;
	
	public abstract Future<String> swap(String req) throws Exception;
	
	public abstract Future<String> init(String req) throws Exception;
	
	public abstract Future<String> edit(String req) throws Exception;
	
	public abstract Future<String> quit(String req) throws Exception;
	
	public abstract Future<String> loadUserList(String req) throws Exception;
	
	public abstract Future<String> loadUser(String req) throws Exception;
	
	public abstract Future<String> loadUserInfo(String req) throws Exception;
	
	public abstract Future<String> loadUserNftList(String req) throws Exception;
	
	//talk
	public abstract Future<String> exploreDomainList(String req) throws Exception;
	
	public abstract Future<String> inviteDomainByFriend(String req) throws Exception;
	
	public abstract Future<String> inviteDomainByWhisper(String req) throws Exception;
	
	public abstract Future<String> inviteDomainByDomain(String req) throws Exception;
	
	public abstract Future<String> kickDomainUser(String req) throws Exception;
	
	public abstract Future<String> joinDomain(String req) throws Exception;
	
	public abstract Future<String> leaveDomain(String req) throws Exception;
	
	public abstract Future<String> boostDomain(String req) throws Exception;
	
	public abstract Future<String> buildDomainPoster(String req) throws Exception;
	
	public abstract Future<String> loadUserDomainList(String req) throws Exception;
	
	public abstract Future<String> loadUserMutualDomains(String req) throws Exception;
	
	public abstract Future<String> loadDomain(String req) throws Exception;
	
	public abstract Future<String> loadDomainTeamList(String req) throws Exception;
	
	public abstract Future<String> loadDomainRoleList(String req) throws Exception;
	
	public abstract Future<String> loadDomainUserList(String req) throws Exception;
	
	public abstract Future<String> loadTeamMessageList(String req) throws Exception;
	
	public abstract Future<String> loadTeamMessage(String req) throws Exception;
	
	public abstract Future<String> saveDomainRole(String req) throws Exception;
	
	public abstract Future<String> addDomainRoleUser(String req) throws Exception;
	
	public abstract Future<String> removeDomainRoleUser(String req) throws Exception;
	
	public abstract Future<String> createDomain(String req) throws Exception;
	
	public abstract Future<String> editDomain(String req) throws Exception;
	
	public abstract Future<String> deleteDomain(String req) throws Exception;
	
	public abstract Future<String> createCategory(String req) throws Exception;
	
	public abstract Future<String> editCategory(String req) throws Exception;
	
	public abstract Future<String> deleteCategory(String req) throws Exception;
	
	public abstract Future<String> createTeam(String req) throws Exception;
	
	public abstract Future<String> editTeam(String req) throws Exception;
	
	public abstract Future<String> deleteTeam(String req) throws Exception;
	
	public abstract Future<String> loadTeamLimitListByTeam(String req) throws Exception;
	
	public abstract Future<String> saveTeamLimitList(String req) throws Exception;
	
	public abstract Future<String> sendTeamMessage(String req) throws Exception;
	
	public abstract Future<String> editTeamMessage(String req) throws Exception;
	
	public abstract Future<String> reactTeamMessage(String req) throws Exception;
	
	public abstract Future<String> replyTeamMessage(String req) throws Exception;
	
	public abstract Future<String> revokeTeamMessage(String req) throws Exception;
	
	public abstract Future<String> removeTeamMessage(String req) throws Exception;
	
	public abstract Future<String> loadTeamMessageNotices(String req) throws Exception;
	
	public abstract Future<String> acceptTeamMessageNotice(String req) throws Exception;
	
	//chat
	public abstract Future<String> loadUserFriendList(String req) throws Exception;
	
	public abstract Future<String> loadUserMutualFriends(String req) throws Exception;
	
	public abstract Future<String> inviteUserFriend(String req) throws Exception;
	
	public abstract Future<String> cancelUserFriend(String req) throws Exception;
	
	public abstract Future<String> acceptUserFriend(String req) throws Exception;
	
	public abstract Future<String> loadUserFriendNotice(String req) throws Exception;
	
	public abstract Future<String> acceptUserFriendNotice(String req) throws Exception;
	
	public abstract Future<String> rejectUserFriend(String req) throws Exception;
	
	public abstract Future<String> removeUserFriend(String req) throws Exception;
	
	public abstract Future<String> loadUserWhisperList(String req) throws Exception;
	
	public abstract Future<String> promiseWhisper(String req) throws Exception;
	
	public abstract Future<String> createWhisper(String req) throws Exception;
	
	public abstract Future<String> deleteWhisper(String req) throws Exception;
	
	public abstract Future<String> loadWhisperUserList(String req) throws Exception;
	
	public abstract Future<String> joinWhisperUsers(String req) throws Exception;
	
	public abstract Future<String> kickWhisperUsers(String req) throws Exception;
	
	public abstract Future<String> enterWhisperUser(String req) throws Exception;
	
	public abstract Future<String> leaveWhisperUser(String req) throws Exception;
	
	public abstract Future<String> loadWhisperMessageList(String req) throws Exception;
	
	public abstract Future<String> loadWhisperMessage(String req) throws Exception;
	
	public abstract Future<String> sendWhisperMessage(String req) throws Exception;
	
	public abstract Future<String> editWhisperMessage(String req) throws Exception;
	
	public abstract Future<String> reactWhisperMessage(String req) throws Exception;
	
	public abstract Future<String> replyWhisperMessage(String req) throws Exception;
	
	public abstract Future<String> revokeWhisperMessage(String req) throws Exception;
	
	public abstract Future<String> loadWhisperMessageNotices(String req) throws Exception;
	
	public abstract Future<String> acceptWhisperMessageNotice(String req) throws Exception;
}
