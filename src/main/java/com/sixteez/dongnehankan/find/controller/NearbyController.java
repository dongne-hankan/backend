package com.sixteez.dongnehankan.find.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sixteez.dongnehankan.find.dto.NearbyRequest;
import com.sixteez.dongnehankan.find.dto.NearbyResponse;
import com.sixteez.dongnehankan.find.service.NearbyRecommendationService;
import com.sixteez.dongnehankan.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/find")
@RequiredArgsConstructor
public class NearbyController {

	private final NearbyRecommendationService nearbyRecommendationService;

	@PostMapping("/nearby")
	public ApiResponse<NearbyResponse> recommendNearby(@Valid @RequestBody NearbyRequest request) {
		return ApiResponse.of(nearbyRecommendationService.recommend(request));
	}
}
