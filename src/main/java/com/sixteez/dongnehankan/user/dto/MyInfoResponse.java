package com.sixteez.dongnehankan.user.dto;

import java.time.OffsetDateTime;

public record MyInfoResponse(Long userId, String nickname, String loginId, OffsetDateTime joinedAt, long savedCourseCount,
		long recordCount, boolean isAdmin, boolean onboardingCompleted) {
}
