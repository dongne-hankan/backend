package com.sixteez.dongnehankan.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(@NotBlank String authorizationCode, @NotBlank String redirectUri, String state) {
}
