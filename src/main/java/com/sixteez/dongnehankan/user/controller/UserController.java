package com.sixteez.dongnehankan.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sixteez.dongnehankan.global.response.ApiResponse;
import com.sixteez.dongnehankan.user.dto.MyInfoResponse;
import com.sixteez.dongnehankan.user.dto.UpdateNicknameRequest;
import com.sixteez.dongnehankan.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public ApiResponse<MyInfoResponse> getMyInfo(@AuthenticationPrincipal Long userId) {
		return ApiResponse.of(userService.getMyInfo(userId));
	}

	@PatchMapping
	public ApiResponse<MyInfoResponse> changeNickname(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody UpdateNicknameRequest request) {
		return ApiResponse.of(userService.changeNickname(userId, request.nickname()));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void withdraw(@AuthenticationPrincipal Long userId) {
		userService.withdraw(userId);
	}
}
