package com.sixteez.dongnehankan.find.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.repository.ExperienceTagRepository;
import com.sixteez.dongnehankan.course.repository.RegionRepository;

import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.dto.NearbyRequest;
import com.sixteez.dongnehankan.find.dto.NearbyResponse;
import com.sixteez.dongnehankan.find.dto.NearbyResponse.Center;
import com.sixteez.dongnehankan.find.dto.NearbyResponse.CourseItem;
import com.sixteez.dongnehankan.find.dto.NearbyResponse.StopItem;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;
import com.sixteez.dongnehankan.find.service.NearbyAiPlanner.AiCourse;
import com.sixteez.dongnehankan.find.service.NearbyCourseBuilder.PlannedStop;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NearbyRecommendationService {

	private static final int MAX_CANDIDATES = 15;

	// 대구·경북을 넉넉히 감싸는 범위. 이 밖이면 카카오를 부르지 않는다.
	private static final double MIN_LAT = 35.4;

	private static final double MAX_LAT = 37.7;

	private static final double MIN_LNG = 127.6;

	private static final double MAX_LNG = 132.0;

	private static final List<String> SERVICE_PROVINCES = List.of("대구", "경상북도", "경북");

	private final KakaoLocalClient kakaoLocalClient;

	private final NearbyAiPlanner aiPlanner;

	private final NearbyCourseBuilder courseBuilder;

	private final RegionRepository regionRepository;

	private final ExperienceTagRepository experienceTagRepository;

	private record Origin(Center center, String label, String basicName) {
	}

	public NearbyResponse recommend(NearbyRequest request) {
		String tagPhrase = request.tagId() == null ? null : experienceTagRepository.findById(request.tagId())
			.map(ExperienceTag::getPhrase)
			.orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
		DurationOption duration = request.duration();
		Origin origin;
		if (request.hasCoordinates()) {
			Center center = new Center(request.lat(), request.lng());
			if (!inServiceArea(center)) {
				return NearbyResponse.outside(center);
			}
			origin = new Origin(center, "지금 위치", "내 주변 " + duration.getLabel() + " 산책");
		}
		else {
			origin = regionOrigin(request.regionId(), duration);
		}
		Center center = origin.center();
		List<KakaoPlace> places = kakaoLocalClient.searchNearby(center.lat(), center.lng(), radiusMeters(duration));
		if (places.isEmpty()) {
			return NearbyResponse.empty(center);
		}
		List<KakaoPlace> candidates = places.subList(0, Math.min(MAX_CANDIDATES, places.size()));
		Optional<AiCourse> aiCourse = aiPlanner.plan(candidates, duration, mood(tagPhrase, request.mood()));
		CourseItem course = aiCourse.map(this::toCourseItem)
			.orElseGet(() -> basicCourse(candidates, duration, origin));
		return new NearbyResponse(center, List.of(course), aiCourse.isPresent(), null);
	}

	private boolean inServiceArea(Center center) {
		if (center.lat() < MIN_LAT || center.lat() > MAX_LAT || center.lng() < MIN_LNG || center.lng() > MAX_LNG) {
			return false;
		}
		return kakaoLocalClient.findArea(center.lat(), center.lng())
			.map(area -> SERVICE_PROVINCES.stream().anyMatch(area.province()::startsWith))
			.orElse(false);
	}

	private Origin regionOrigin(Long regionId, DurationOption duration) {
		Region region = regionRepository.findById(regionId)
			.filter(found -> found.getCenterLat() != null && found.getCenterLng() != null)
			.orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
		Center center = new Center(region.getCenterLat().doubleValue(), region.getCenterLng().doubleValue());
		return new Origin(center, "지역 중심", region.getName() + " " + duration.getLabel() + " 산책");
	}

	private static String mood(String tagPhrase, String mood) {
		if (tagPhrase == null) {
			return mood;
		}
		return StringUtils.hasText(mood) ? tagPhrase + ", " + mood : tagPhrase;
	}

	private int radiusMeters(DurationOption duration) {
		return duration == DurationOption.MIN_30 ? 1000 : 2000;
	}

	private CourseItem basicCourse(List<KakaoPlace> candidates, DurationOption duration, Origin origin) {
		List<PlannedStop> stops = courseBuilder.basicStops(candidates, duration)
			.stream()
			.map(stop -> new PlannedStop(stop.place(), stop.stayMinutes(),
					origin.label() + "에서 약 " + stop.place().distanceMeters() + "m 떨어진 곳이에요."))
			.toList();
		String first = stops.get(0).place().name();
		return courseItem(origin.basicName(), first + "에서 시작하는 가까운 산책이에요.",
				origin.label() + "에서 가까운 곳을 걷기 좋은 순서로 이었어요.", stops);
	}

	private CourseItem toCourseItem(AiCourse course) {
		return courseItem(course.name(), course.intro(), course.reason(), course.stops());
	}

	private CourseItem courseItem(String name, String intro, String reason, List<PlannedStop> stops) {
		List<StopItem> items = new ArrayList<>();
		for (int i = 0; i < stops.size(); i++) {
			PlannedStop stop = stops.get(i);
			KakaoPlace place = stop.place();
			Integer moveToNext = i + 1 < stops.size() ? courseBuilder.walkMinutes(place, stops.get(i + 1).place())
					: null;
			items.add(new StopItem(i + 1, place.name(), place.address(), place.lat(), place.lng(), stop.stayMinutes(),
					moveToNext, stop.reason(), place.id(), place.placeUrl()));
		}
		// 코스 저장 방식이 정해지기 전까지 courseId는 null이다.
		return new CourseItem(null, name, intro, reason, courseBuilder.totalMinutes(stops), items);
	}
}
