package com.sixteez.dongnehankan.find.dto;

import java.util.List;

import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;

public record RecommendationResponse(Condition condition, List<CourseSummaryResponse> courses, boolean hasMore,
		Fallback fallback, Personalization personalization) {

	public record Condition(Long regionId, Long tagId, DurationOption duration) {
	}

	public enum FallbackType {
		NONE, LONGER_DURATION, OTHER_TAG, EMPTY
	}

	public record Fallback(FallbackType type, String message, Condition appliedCondition) {
		public static Fallback none() {
			return new Fallback(FallbackType.NONE, null, null);
		}
	}

	public record Personalization(boolean applied, String message) {
		public static Personalization none() {
			return new Personalization(false, null);
		}
	}
}
