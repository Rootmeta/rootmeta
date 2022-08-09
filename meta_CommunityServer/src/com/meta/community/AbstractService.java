package com.meta.community;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;

import com.meta.community.data.account.AccountDataModel;
import com.meta.community.data.account.TUser;
import com.meta.community.lang.AccountLang;
import com.suomee.csp.lib.future.Future;
import com.suomee.csp.lib.future.FutureHandler;
import com.suomee.csp.lib.log.Logger;
import com.suomee.csp.lib.proto.CspException;
import com.suomee.csp.lib.proto.HttpWraper;
import com.suomee.csp.lib.server.Service;
import com.suomee.csp.lib.util.DateTimeUtil;
import com.suomee.csp.lib.util.StringUtil;

public abstract class AbstractService extends Service {
	private Set<String> cmds = new HashSet<String>(Arrays.asList(new String[] {
			"load_language_list",
			"load_tag_list",
			"load_chain_list",
			"load_token_list",
			"client",
			"unique",
			"sign"
	}));
	
	@Override
	protected Future<?> dispatch(String cmd, Object[] parameters) throws Exception {
		final Future<String> rspFuture = new Future<String>();
		
		HttpWraper httpWraper = (HttpWraper)parameters[0];
		final JSONObject jsonReq = new JSONObject(httpWraper.getHttpBody());
		final JSONObject jsonReqHead = jsonReq.getJSONObject("head");
		String uticket = jsonReqHead.optString("uticket", "");
		
		TUser user = AccountDataModel.instance().selectUserByDeTicket(uticket);
		if (!AbstractService.this.cmds.contains(cmd)) {
			if (user == null) {
				return rspFuture.setResult(Service.headonly(9, AccountLang.NOT_LOGIN).toString()).complete();
			}
			if (StringUtil.isNullOrEmpty(user.getDeTicket()) || DateTimeUtil.seconds() - user.getDeTicketTime() > 86400 * 14) {
				return rspFuture.setResult(Service.headonly(8, AccountLang.LOGIN_EXPIRE).toString()).complete();
			}
		}
		if (user != null) {
			jsonReqHead.put("userId", user.getUserId());
		}
		
		@SuppressWarnings("unchecked")
		Future<Object> invokeFuture = (Future<Object>)AbstractService.this.getClass().getMethod(cmd2fun(cmd), String.class).invoke(AbstractService.this, jsonReq.toString());
		invokeFuture.ready(new FutureHandler<Object>() {
			@Override
			public void complete(Future<Object> invokeFuture) {
				if (!invokeFuture.isSuccess()) {
					rspFuture.setException(invokeFuture.getException());
				}
				else {
					rspFuture.setResult((String)invokeFuture.getResult());
				}
				rspFuture.complete();
			}
			@Override
			public void exception(CspException e) {
				Logger.getLogger("error").error("AbstractService.dispatch invoke exception.", e);
				rspFuture.setException(e).complete();
			}
		});
		
		return rspFuture;
	}
}
