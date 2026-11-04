package com.sixteez.dongnehankan.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(@NotBlank @Email(message = "이메일 형식을 확인해 주세요.") String email) {
}
