package com.sixteez.dongnehankan.auth.dto;

public record AuthUser(Long userId, String nickname, boolean isAdmin, boolean onboardingCompleted) {
}
