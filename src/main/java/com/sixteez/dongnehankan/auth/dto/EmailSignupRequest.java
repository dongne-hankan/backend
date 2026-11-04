package com.sixteez.dongnehankan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailSignupRequest(
		@NotBlank @Email(message = "이메일 형식을 확인해 주세요.") @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.") String password,
		@NotBlank @Size(min = 2, max = 12, message = "닉네임은 2~12자로 입력해 주세요.") String nickname,
		@NotNull Boolean privacyAgreed) {
}
