package com.sixteez.dongnehankan.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
		@NotBlank(message = "닉네임을 입력해 주세요.") @Size(min = 2, max = 12, message = "닉네임은 2~12자로 입력해 주세요.") String nickname) {
}
