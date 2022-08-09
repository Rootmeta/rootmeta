package com.meta.community;

import com.meta.community.data.account.AccountDataModel;
import com.meta.community.data.account.TUser;
import com.meta.community.proto.LoadUserByDeTicketReq;
import com.meta.community.proto.LoadUserByDeTicketRsp;
import com.suomee.csp.lib.future.Future;

public class InsideServiceImp extends InsideService {
	@Override
	public Future<LoadUserByDeTicketRsp> loadUserByDeTicket(LoadUserByDeTicketReq req) throws Exception {
		final String deTicket = req.getDeTicket();
		
		final Future<LoadUserByDeTicketRsp> rspFuture = new Future<LoadUserByDeTicketRsp>();
		
		TUser user = AccountDataModel.instance().selectUserByDeTicket(deTicket);
		
		long userId = 0L;
		String nickname = null;
		String deAddress = null;
		if (user != null) {
			userId = user.getUserId();
			nickname = user.getNickname();
			deAddress = user.getDeAddress();
		}
		
		return rspFuture.setResult(new LoadUserByDeTicketRsp().setUserId(userId).setNicknamek(nickname).setDeAddress(deAddress)).complete();
	}
}
