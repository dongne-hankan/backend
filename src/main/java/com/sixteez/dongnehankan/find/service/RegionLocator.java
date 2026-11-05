package com.sixteez.dongnehankan.find.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.course.entity.Province;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.repository.RegionRepository;
import com.sixteez.dongnehankan.find.dto.FindRegionResponse;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.AdministrativeArea;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionLocator {

	private static final double MIN_LAT = 33.0;

	private static final double MAX_LAT = 39.0;

	private static final double MIN_LNG = 124.0;

	private static final double MAX_LNG = 132.0;

	private static final Map<Province, List<String>> PROVINCE_NAMES = Map.of(Province.DAEGU, List.of("대구"),
			Province.GYEONGBUK, List.of("경상북도", "경북"));

	private final KakaoLocalClient kakaoLocalClient;

	private final RegionRepository regionRepository;

	public FindRegionResponse locate(double lat, double lng) {
		if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LNG || lng > MAX_LNG) {
			return FindRegionResponse.outside();
		}
		return kakaoLocalClient.findArea(lat, lng)
			.flatMap(area -> regionRepository.findAllWithVisibleCourse()
				.stream()
				.filter(region -> matches(region, area))
				.findFirst())
			.map(FindRegionResponse::inside)
			.orElseGet(FindRegionResponse::outside);
	}

	private boolean matches(Region region, AdministrativeArea area) {
		boolean sameProvince = PROVINCE_NAMES.getOrDefault(region.getProvince(), List.of())
			.stream()
			.anyMatch(area.province()::startsWith);
		return sameProvince && region.getName().equals(area.district());
	}
}
