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
import com.sixteez.dongnehankan.auth.dto.EmailAvailabilityResponse;
import com.sixteez.dongnehankan.auth.dto.EmailSignupRequest;
import com.sixteez.dongnehankan.auth.dto.KakaoLoginRequest;
import com.sixteez.dongnehankan.auth.dto.KakaoLoginResponse;
import com.sixteez.dongnehankan.auth.dto.LoginRequest;
import com.sixteez.dongnehankan.auth.dto.PasswordResetConfirmRequest;
import com.sixteez.dongnehankan.auth.dto.PasswordResetRequest;
import com.sixteez.dongnehankan.auth.dto.RefreshTokenRequest;
import com.sixteez.dongnehankan.auth.dto.SignupRequest;
import com.sixteez.dongnehankan.auth.dto.TokenResponse;
import com.sixteez.dongnehankan.auth.service.AuthService;
import com.sixteez.dongnehankan.global.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@GetMapping("/email-availability")
	public ApiResponse<EmailAvailabilityResponse> emailAvailability(@RequestParam @NotBlank @Email String email) {
		return ApiResponse.of(authService.emailAvailability(email));
	}

	@PostMapping("/email/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<TokenResponse> signupWithEmail(@Valid @RequestBody EmailSignupRequest request) {
		return ApiResponse.of(authService.signupWithEmail(request));
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

	@PostMapping("/password-reset/request")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		authService.requestPasswordReset(request.email());
	}

	@PostMapping("/password-reset/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
		authService.confirmPasswordReset(request);
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
