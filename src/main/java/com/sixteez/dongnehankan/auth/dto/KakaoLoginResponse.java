package com.sixteez.dongnehankan.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record KakaoLoginResponse(boolean signupRequired, String accessToken, String refreshToken, AuthUser user,
		String signupToken, String kakaoNickname) {

	public static KakaoLoginResponse loggedIn(TokenResponse tokens) {
		return new KakaoLoginResponse(false, tokens.accessToken(), tokens.refreshToken(), tokens.user(), null, null);
	}

	public static KakaoLoginResponse needsSignup(String signupToken, String kakaoNickname) {
		return new KakaoLoginResponse(true, null, null, null, signupToken, kakaoNickname);
	}
}
