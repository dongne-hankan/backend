package com.sixteez.dongnehankan.find.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sixteez.dongnehankan.find.dto.AiRecommendationResponse;
import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.service.AiRecommendationService;
import com.sixteez.dongnehankan.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/find")
@RequiredArgsConstructor
public class AiRecommendationController {

	private final AiRecommendationService aiRecommendationService;

	@GetMapping("/ai-recommendations")
	public ApiResponse<AiRecommendationResponse> recommend(@RequestParam(required = false) Long regionId,
			@RequestParam(required = false) Long tagId, @RequestParam(required = false) DurationOption duration,
			@RequestParam(defaultValue = "") List<Long> excludeCourseIds, @AuthenticationPrincipal Long userId) {
		return ApiResponse.of(aiRecommendationService.recommend(regionId, tagId, duration, excludeCourseIds, userId));
	}
}
