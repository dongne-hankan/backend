package com.sixteez.dongnehankan.auth.service;

import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.auth.dto.AuthUser;
import com.sixteez.dongnehankan.auth.dto.AuthorizeUrlResponse;
import com.sixteez.dongnehankan.auth.dto.KakaoLoginRequest;
import com.sixteez.dongnehankan.auth.dto.KakaoLoginResponse;
import com.sixteez.dongnehankan.auth.dto.SignupRequest;
import com.sixteez.dongnehankan.auth.dto.TokenResponse;
import com.sixteez.dongnehankan.auth.entity.RefreshToken;
import com.sixteez.dongnehankan.auth.repository.RefreshTokenRepository;
import com.sixteez.dongnehankan.auth.service.KakaoClient.KakaoProfile;
import com.sixteez.dongnehankan.auth.service.TokenProvider.SignupClaims;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.user.entity.User;
import com.sixteez.dongnehankan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final int MIN_NICKNAME_LENGTH = 2;
	private static final int MAX_NICKNAME_LENGTH = 12;
	private static final String DEFAULT_NICKNAME = "동네 이웃";

	private final KakaoClient kakaoClient;

	private final TokenProvider tokenProvider;

	private final UserRepository userRepository;

	private final RefreshTokenRepository refreshTokenRepository;

	public AuthorizeUrlResponse authorizeUrl() {
		String state = tokenProvider.createRandomToken();
		return new AuthorizeUrlResponse(kakaoClient.authorizeUrl(state), state);
	}

	@Transactional
	public KakaoLoginResponse loginWithKakao(KakaoLoginRequest request) {
		KakaoProfile profile = kakaoClient.fetchProfile(request.authorizationCode(), request.redirectUri());
		return userRepository.findByKakaoId(profile.kakaoId())
			.map(user -> KakaoLoginResponse.loggedIn(issueTokens(user)))
			.orElseGet(() -> KakaoLoginResponse.needsSignup(
					tokenProvider.createSignupToken(profile.kakaoId(), profile.nickname()), profile.nickname()));
	}

	@Transactional
	public TokenResponse signup(SignupRequest request) {
		SignupClaims claims = tokenProvider.parseSignupToken(request.signupToken());
		if (!request.privacyAgreed()) {
			throw new BusinessException(ErrorCode.PRIVACY_AGREEMENT_REQUIRED);
		}
		User user = userRepository.findByKakaoId(claims.kakaoId())
			.orElseGet(() -> userRepository.save(new User(claims.kakaoId(),
					nicknameFor(request.nickname(), claims.nickname()), LocalDateTime.now())));
		return issueTokens(user);
	}

	@Transactional
	public TokenResponse refresh(String refreshToken) {
		RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenProvider.hash(refreshToken))
			.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
		refreshTokenRepository.delete(stored);
		refreshTokenRepository.flush();
		if (stored.isExpired()) {
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
		}
		User user = userRepository.findById(stored.getUserId())
			.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
		TokenResponse tokens = issueTokens(user);
		return new TokenResponse(tokens.accessToken(), tokens.refreshToken(), null);
	}

	@Transactional
	public void logout(Long userId, String refreshToken) {
		refreshTokenRepository.deleteByTokenHashAndUserId(tokenProvider.hash(refreshToken), userId);
	}

	private TokenResponse issueTokens(User user) {
		String refreshToken = tokenProvider.createRandomToken();
		LocalDateTime expiresAt = LocalDateTime.ofInstant(tokenProvider.refreshTokenExpiry(), ZoneId.systemDefault());
		refreshTokenRepository.save(new RefreshToken(user.getId(), tokenProvider.hash(refreshToken), expiresAt));
		boolean onboardingCompleted = userRepository.countCompletedOnboarding(user.getId()) > 0;
		return new TokenResponse(tokenProvider.createAccessToken(user.getId(), user.isAdmin()), refreshToken,
				new AuthUser(user.getId(), user.getNickname(), user.isAdmin(), onboardingCompleted));
	}

	private String nicknameFor(String requested, String kakaoNickname) {
		if (requested != null) {
			return requested;
		}
		String nickname = kakaoNickname == null ? "" : kakaoNickname.strip();
		if (nickname.length() < MIN_NICKNAME_LENGTH) {
			return DEFAULT_NICKNAME;
		}
		return nickname.length() > MAX_NICKNAME_LENGTH ? nickname.substring(0, MAX_NICKNAME_LENGTH) : nickname;
	}
}
