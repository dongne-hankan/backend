package com.sixteez.dongnehankan.find.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.repository.ExperienceTagRepository;
import com.sixteez.dongnehankan.find.dto.AiRecommendationResponse;
import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.Condition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

	private final RecommendationService recommendationService;

	private final ExperienceTagRepository experienceTagRepository;

	private final AiReasonWriter aiReasonWriter;

	public AiRecommendationResponse recommend(Long regionId, Long tagId, DurationOption duration,
			List<Long> excludeCourseIds, Long userId) {
		RecommendationResponse recommendation = recommendationService.recommend(regionId, tagId, duration,
				excludeCourseIds, userId);
		Condition applied = appliedCondition(recommendation);
		String tagName = experienceTagRepository.findById(applied.tagId()).map(ExperienceTag::getName).orElse("");
		Map<Long, String> reasons = aiReasonWriter.write(tagName, applied.duration(), recommendation.courses());
		return AiRecommendationResponse.of(recommendation, reasons);
	}

	private Condition appliedCondition(RecommendationResponse recommendation) {
		if (recommendation.fallback() != null && recommendation.fallback().appliedCondition() != null) {
			return recommendation.fallback().appliedCondition();
		}
		return recommendation.condition();
	}
}
