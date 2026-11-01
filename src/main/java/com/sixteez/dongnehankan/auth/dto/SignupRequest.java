package com.sixteez.dongnehankan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignupRequest(@NotBlank String signupToken, @NotNull Boolean privacyAgreed,
		@Size(min = 2, max = 12, message = "닉네임은 2~12자로 입력해 주세요.") String nickname) {
}
