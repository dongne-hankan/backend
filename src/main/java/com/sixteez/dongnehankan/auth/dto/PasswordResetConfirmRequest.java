package com.sixteez.dongnehankan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(@NotBlank String token,
		@NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자로 입력해 주세요.") String newPassword) {
}
