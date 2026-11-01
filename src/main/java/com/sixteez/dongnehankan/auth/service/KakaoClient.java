package com.sixteez.dongnehankan.auth.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;

@Component
public class KakaoClient {

	private static final String AUTHORIZE_URL = "https://kauth.kakao.com/oauth/authorize";
	private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
	private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

	private final RestClient restClient;

	private final String clientId;

	private final String clientSecret;

	private final String redirectUri;

	public KakaoClient(@Value("${app.kakao.client-id}") String clientId,
			@Value("${app.kakao.client-secret}") String clientSecret,
			@Value("${app.kakao.redirect-uri}") String redirectUri) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(3));
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		this.restClient = RestClient.builder().requestFactory(requestFactory).build();
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.redirectUri = redirectUri;
	}

	public record KakaoProfile(String kakaoId, String nickname) {
	}

	private record TokenResponse(@JsonProperty("access_token") String accessToken) {
	}

	private record UserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
	}

	private record KakaoAccount(Profile profile) {
	}

	private record Profile(String nickname) {
	}

	public String authorizeUrl(String state) {
		return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
			.queryParam("client_id", clientId)
			.queryParam("redirect_uri", redirectUri)
			.queryParam("response_type", "code")
			.queryParam("scope", "profile_nickname")
			.queryParam("state", state)
			.encode()
			.toUriString();
	}

	public KakaoProfile fetchProfile(String authorizationCode, String requestRedirectUri) {
		String accessToken = requestAccessToken(authorizationCode, requestRedirectUri);
		try {
			UserResponse user = restClient.get()
				.uri(USER_URL)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.body(UserResponse.class);
			Profile profile = user.kakaoAccount() == null ? null : user.kakaoAccount().profile();
			return new KakaoProfile(user.id().toString(), profile == null ? null : profile.nickname());
		}
		catch (RestClientException e) {
			throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
		}
	}

	private String requestAccessToken(String authorizationCode, String requestRedirectUri) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", clientId);
		form.add("redirect_uri", requestRedirectUri);
		form.add("code", authorizationCode);
		if (StringUtils.hasText(clientSecret)) {
			form.add("client_secret", clientSecret);
		}
		try {
			return restClient.post()
				.uri(TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(TokenResponse.class)
				.accessToken();
		}
		catch (HttpClientErrorException.BadRequest | HttpClientErrorException.Unauthorized e) {
			throw new BusinessException(ErrorCode.KAKAO_CODE_INVALID);
		}
		catch (RestClientException e) {
			throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
		}
	}
}
