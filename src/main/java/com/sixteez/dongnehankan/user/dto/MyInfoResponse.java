package com.sixteez.dongnehankan.user.dto;

import java.time.OffsetDateTime;

public record MyInfoResponse(Long userId, String nickname, OffsetDateTime joinedAt, long savedCourseCount,
		long recordCount, boolean isAdmin, boolean onboardingCompleted) {
}
