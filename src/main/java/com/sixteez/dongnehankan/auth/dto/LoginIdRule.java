package com.sixteez.dongnehankan.auth.dto;

public final class LoginIdRule {

	public static final String PATTERN = "^[A-Za-z0-9]{4,20}$";

	public static final String MESSAGE = "아이디는 영문과 숫자 4~20자로 입력해 주세요.";

	private LoginIdRule() {
	}
}
