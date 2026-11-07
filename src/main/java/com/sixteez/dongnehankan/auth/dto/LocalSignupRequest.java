package com.sixteez.dongnehankan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LocalSignupRequest(
		@NotBlank @Pattern(regexp = LoginIdRule.PATTERN, message = LoginIdRule.MESSAGE) String loginId,
		@NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.") String password,
		@NotBlank @Size(min = 2, max = 12, message = "닉네임은 2~12자로 입력해 주세요.") String nickname,
		@NotNull Boolean privacyAgreed) {
}
