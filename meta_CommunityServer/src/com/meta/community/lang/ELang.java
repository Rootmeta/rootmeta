package com.meta.community.lang;

public enum ELang {
	EN("en", "English US"),
	ZH("zh", "简体中文");
	
	public static final String NAME = "LANG";
	
	private String code;
	private String name;
	
	public String getCode() {
		return code;
	}
	public String getName() {
		return name;
	}
	
	private ELang(String code, String name) {
		this.code = code;
		this.name = name;
	}
}
