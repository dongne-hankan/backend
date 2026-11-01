package com.sixteez.dongnehankan.course.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sixteez.dongnehankan.course.dto.CourseDetailResponse;
import com.sixteez.dongnehankan.course.dto.PlaceDetailResponse;
import com.sixteez.dongnehankan.course.service.CourseService;
import com.sixteez.dongnehankan.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CourseController {

	private final CourseService courseService;

	@GetMapping("/courses/{courseId}")
	public ApiResponse<CourseDetailResponse> getCourse(@PathVariable Long courseId,
			@AuthenticationPrincipal Long userId) {
		return ApiResponse.of(courseService.getCourse(courseId, userId));
	}

	@GetMapping("/places/{placeId}")
	public ApiResponse<PlaceDetailResponse> getPlace(@PathVariable Long placeId) {
		return ApiResponse.of(courseService.getPlace(placeId));
	}
}
