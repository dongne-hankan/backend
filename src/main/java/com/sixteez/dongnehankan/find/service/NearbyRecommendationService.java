package com.sixteez.dongnehankan.find.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.dto.NearbyRequest;
import com.sixteez.dongnehankan.find.dto.NearbyResponse;
import com.sixteez.dongnehankan.find.dto.NearbyResponse.Center;
import com.sixteez.dongnehankan.find.dto.NearbyResponse.CourseItem;
import com.sixteez.dongnehankan.find.dto.NearbyResponse.StopItem;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;
import com.sixteez.dongnehankan.find.service.NearbyAiPlanner.AiCourse;
import com.sixteez.dongnehankan.find.service.NearbyCourseBuilder.PlannedStop;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NearbyRecommendationService {

	private static final int MAX_CANDIDATES = 15;

	private final KakaoLocalClient kakaoLocalClient;

	private final NearbyAiPlanner aiPlanner;

	private final NearbyCourseBuilder courseBuilder;

	public NearbyResponse recommend(NearbyRequest request) {
		Center center = new Center(request.lat(), request.lng());
		DurationOption duration = request.duration();
		List<KakaoPlace> places = kakaoLocalClient.searchNearby(request.lat(), request.lng(), radiusMeters(duration));
		if (places.isEmpty()) {
			return NearbyResponse.empty(center);
		}
		List<KakaoPlace> candidates = places.subList(0, Math.min(MAX_CANDIDATES, places.size()));
		Optional<AiCourse> aiCourse = aiPlanner.plan(candidates, duration, request.mood());
		CourseItem course = aiCourse.map(this::toCourseItem).orElseGet(() -> basicCourse(candidates, duration));
		return new NearbyResponse(center, List.of(course), aiCourse.isPresent(), null);
	}

	private int radiusMeters(DurationOption duration) {
		return duration == DurationOption.MIN_30 ? 1000 : 2000;
	}

	private CourseItem basicCourse(List<KakaoPlace> candidates, DurationOption duration) {
		List<PlannedStop> stops = courseBuilder.basicStops(candidates, duration);
		String first = stops.get(0).place().name();
		return courseItem("내 주변 " + duration.getLabel() + " 산책", first + "에서 시작하는 가까운 산책이에요.",
				"지금 위치에서 가까운 곳을 걷기 좋은 순서로 이었어요.", stops);
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
					moveToNext, stop.reason(), place.placeUrl()));
		}
		// 코스 저장 방식이 정해지기 전까지 courseId는 null이다.
		return new CourseItem(null, name, intro, reason, courseBuilder.totalMinutes(stops), items);
	}
}
