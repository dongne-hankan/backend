package com.sixteez.dongnehankan.find.dto;

import java.util.List;
import java.util.Map;

public record AiRecommendationResponse(RecommendationResponse recommendation, List<CourseReason> reasons,
		boolean aiGenerated) {

	public record CourseReason(Long courseId, String reason) {
	}

	public static AiRecommendationResponse of(RecommendationResponse recommendation, Map<Long, String> reasons) {
		List<CourseReason> items = reasons.entrySet()
			.stream()
			.map(entry -> new CourseReason(entry.getKey(), entry.getValue()))
			.toList();
		return new AiRecommendationResponse(recommendation, items, !items.isEmpty());
	}
}
