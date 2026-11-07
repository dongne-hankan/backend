package com.sixteez.dongnehankan.auth.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sixteez.dongnehankan.auth.dto.AuthorizeUrlResponse;
import com.sixteez.dongnehankan.auth.dto.KakaoLoginRequest;
import com.sixteez.dongnehankan.auth.dto.KakaoLoginResponse;
import com.sixteez.dongnehankan.auth.dto.LocalSignupRequest;
import com.sixteez.dongnehankan.auth.dto.LoginIdAvailabilityResponse;
import com.sixteez.dongnehankan.auth.dto.LoginIdRule;
import com.sixteez.dongnehankan.auth.dto.LoginRequest;
import com.sixteez.dongnehankan.auth.dto.RefreshTokenRequest;
import com.sixteez.dongnehankan.auth.dto.SignupRequest;
import com.sixteez.dongnehankan.auth.dto.TokenResponse;
import com.sixteez.dongnehankan.auth.service.AuthService;
import com.sixteez.dongnehankan.global.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/login-id-availability")
	public ApiResponse<LoginIdAvailabilityResponse> loginIdAvailability(
			@RequestParam @Pattern(regexp = LoginIdRule.PATTERN, message = LoginIdRule.MESSAGE) String loginId) {
		return ApiResponse.of(authService.loginIdAvailability(loginId));
	}

	@PostMapping("/local/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TokenResponse> signupLocal(@Valid @RequestBody LocalSignupRequest request) {
		return ApiResponse.of(authService.signupLocal(request));
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.of(authService.login(request));
	}

	@GetMapping("/kakao/authorize-url")
	public ApiResponse<AuthorizeUrlResponse> authorizeUrl() {
		return ApiResponse.of(authService.authorizeUrl());
	}

	@PostMapping("/kakao")
	public ApiResponse<KakaoLoginResponse> loginWithKakao(@Valid @RequestBody KakaoLoginRequest request) {
		return ApiResponse.of(authService.loginWithKakao(request));
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TokenResponse> signup(@Valid @RequestBody SignupRequest request) {
		return ApiResponse.of(authService.signup(request));
	}

	@PostMapping("/token/refresh")
	public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ApiResponse.of(authService.refresh(request.refreshToken()));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@AuthenticationPrincipal Long userId, @Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(userId, request.refreshToken());
	}
}
