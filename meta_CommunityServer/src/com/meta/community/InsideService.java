package com.meta.community;

import com.meta.community.proto.LoadUserByDeTicketReq;
import com.meta.community.proto.LoadUserByDeTicketRsp;
import com.suomee.csp.lib.future.Future;
import com.suomee.csp.lib.server.Service;

public abstract class InsideService extends Service {
	@Override
	protected String name() {
		return Names.INSIDE_SVC_ANME;
	}
	
	@Override
	protected Future<?> dispatch(String cmd, Object[] parameters) throws Exception {
		return (Future<?>)this.getClass().getMethod(cmd, parameters[0].getClass()).invoke(this, parameters[0]);
	}
	
	public abstract Future<LoadUserByDeTicketRsp> loadUserByDeTicket(LoadUserByDeTicketReq req) throws Exception;
}
