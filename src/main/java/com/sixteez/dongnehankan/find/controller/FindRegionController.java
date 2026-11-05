package com.sixteez.dongnehankan.find.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sixteez.dongnehankan.find.dto.FindRegionResponse;
import com.sixteez.dongnehankan.find.service.RegionLocator;
import com.sixteez.dongnehankan.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/find")
@RequiredArgsConstructor
public class FindRegionController {

	private final RegionLocator regionLocator;

	@GetMapping("/region")
	public ApiResponse<FindRegionResponse> findRegion(@RequestParam double lat, @RequestParam double lng) {
		return ApiResponse.of(regionLocator.locate(lat, lng));
	}
}
