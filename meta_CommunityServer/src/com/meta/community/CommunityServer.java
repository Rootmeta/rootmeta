package com.meta.community;

import java.util.HashMap;
import java.util.Map;

import com.meta.community.lang.AccountLang;
import com.meta.community.lang.BaseLang;
import com.meta.community.lang.ChatLang;
import com.meta.community.lang.ELang;
import com.meta.community.lang.TalkLang;
import com.meta.community.routine.DeAmountRoutine;
import com.suomee.csp.lib.config.Configs;
import com.suomee.csp.lib.db.ConnectionManager;
import com.suomee.csp.lib.lang.Lang;
import com.suomee.csp.lib.server.Server;
import com.suomee.csp.lib.server.Service;

public class CommunityServer extends Server {
	public static final String DB_META = "db_meta";
	
	@Override
	protected String name() {
		return Names.SERVER_FULL_NAME;
	}
	
	@Override
	protected Map<String, Class<? extends Service>> services() {
		Map<String, Class<? extends Service>> services = new HashMap<String, Class<? extends Service>>();
		services.put(Names.COMMUNITY_SVC_ANME, CommunityServiceImp.class);
		services.put(Names.INSIDE_SVC_ANME, InsideServiceImp.class);
		return services;
	}
	
	@Override
	protected void starting() {
		Map<String, String> languages = new HashMap<String, String>();
		languages.put(ELang.EN.getCode(), ELang.EN.getName());
		languages.put(ELang.ZH.getCode(), ELang.ZH.getName());
		Lang.initLanguages(languages);
		
		Map<String, Map<String, String>> codeResources = new HashMap<String, Map<String, String>>();
		
		Map<String, String> resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Invalid Parameters");
		resources.put(ELang.ZH.getCode(), "参数异常");
		codeResources.put(BaseLang.INVALID_PARAMETER, resources);
		resources.put(ELang.EN.getCode(), "Permission Denied");
		resources.put(ELang.ZH.getCode(), "没有权限");
		codeResources.put(BaseLang.PERMISSION_DENIED, resources);
		
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Not Login");
		resources.put(ELang.ZH.getCode(), "未登录");
		codeResources.put(AccountLang.NOT_LOGIN, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Login Expired");
		resources.put(ELang.ZH.getCode(), "登录超时");
		codeResources.put(AccountLang.LOGIN_EXPIRE, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Invalid Address");
		resources.put(ELang.ZH.getCode(), "无效地址");
		codeResources.put(AccountLang.INVALID_ADDRESS, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Nickname can not be empay.");
		resources.put(ELang.ZH.getCode(), "昵称不可为空");
		codeResources.put(AccountLang.NICKNAME_CAN_NOT_BE_EMPTY, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Avatar can not be empty.");
		resources.put(ELang.ZH.getCode(), "头像不能为空");
		codeResources.put(AccountLang.AVATAR_CAN_NOT_BE_EMPTY, resources);
		
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Island name can not be empty.");
		resources.put(ELang.ZH.getCode(), "岛屿名称不能为空");
		codeResources.put(TalkLang.DOMAIN_NAME_CAN_NOT_BE_EMPTY, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Island name already exist.");
		resources.put(ELang.ZH.getCode(), "岛屿名称冲突");
		codeResources.put(TalkLang.DOMAIN_NAME_ALREADY_EXIST, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Category name can not be empty.");
		resources.put(ELang.ZH.getCode(), "类别名称不能为空");
		codeResources.put(TalkLang.CATEGORY_NAME_CAN_NOT_BE_EMPTY, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Category name already exist.");
		resources.put(ELang.ZH.getCode(), "类别名称冲突");
		codeResources.put(TalkLang.CATEGORY_NAME_ALREADY_EXIST, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Tribal name can not be empty.");
		resources.put(ELang.ZH.getCode(), "部落名称不能为空");
		codeResources.put(TalkLang.TEAM_NAME_CAN_NOT_BE_EMPTY, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Tribal name already exist.");
		resources.put(ELang.ZH.getCode(), "部落名称冲突");
		codeResources.put(TalkLang.TEAM_NAME_ALREADY_EXIST, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Tribal Token value can not be none.");
		resources.put(ELang.ZH.getCode(), "部落Token阈值不能为空");
		codeResources.put(TalkLang.TEAM_LIMIT_TOKEN_VALUE_CAN_NOT_BE_EMPTY, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Tribal NFT Contract Address can not be empty.");
		resources.put(ELang.ZH.getCode(), "部落NFT合约地址不能为空");
		codeResources.put(TalkLang.TEAM_LIMIT_NFT_CONTRACT_CAN_NOT_BE_EMPTY, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Message can not be mepty.");
		resources.put(ELang.ZH.getCode(), "消息不能为空");
		codeResources.put(TalkLang.MESSAGE_CAN_NOT_BE_EMPTY, resources);
		
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Text Tribal");
		resources.put(ELang.ZH.getCode(), "文字部落");
		codeResources.put(TalkLang.DEFAULT_CATEGORY_NAME, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "General");
		resources.put(ELang.ZH.getCode(), "常规");
		codeResources.put(TalkLang.DEFAULT_TEAM_NAME, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "@everyone");
		resources.put(ELang.ZH.getCode(), "@任何人");
		codeResources.put(TalkLang.DEFAULT_ROLE_NAME, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "New Role");
		resources.put(ELang.ZH.getCode(), "新身份组");
		codeResources.put(TalkLang.NEW_ROLE_NAME, resources);
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Boost allowed only once per Month.");
		resources.put(ELang.ZH.getCode(), "每个月只能助力一次");
		codeResources.put(TalkLang.DOMAIN_BOOST_NOT_EXPIRE, resources);
		
		resources = new HashMap<String, String>();
		resources.put(ELang.EN.getCode(), "Message can not be mepty.");
		resources.put(ELang.ZH.getCode(), "消息不能为空");
		codeResources.put(ChatLang.MESSAGE_CAN_NOT_BE_EMPTY, resources);
		
		Lang.initResource(codeResources);
		
		Lang.enable(true);
	}
	
	@Override
	protected void started() {
		Map<String, Map<String, String>> dbs = new HashMap<String, Map<String, String>>();
		dbs.put(DB_META, Configs.getConfig().getMap("/conf/dbs/" + DB_META));
		ConnectionManager.instance().start(dbs);
		
		DeAmountRoutine.instance().start();
	}
	
	public static void main(String[] args) {
		new CommunityServer().start();
	}
}
