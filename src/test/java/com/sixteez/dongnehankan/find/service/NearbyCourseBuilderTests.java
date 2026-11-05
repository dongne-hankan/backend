package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;
import com.sixteez.dongnehankan.find.service.NearbyCourseBuilder.PlannedStop;

class NearbyCourseBuilderTests {

	private static final double STEP_200M = 0.0018;

	private final NearbyCourseBuilder builder = new NearbyCourseBuilder();

	private static KakaoPlace place(String id, double latOffset, int distanceFromUser) {
		return new KakaoPlace(id, "장소" + id, "주소" + id, 35.8 + latOffset, 128.6, "http://place.map.kakao.com/" + id,
				distanceFromUser);
	}

	@Test
	void walkMinutesAddDetourAndRoundUp() {
		assertThat(builder.walkMinutes(place("1", 0, 0), place("2", STEP_200M, 0))).isEqualTo(4);
	}

	@Test
	void walkTakesAtLeastOneMinute() {
		assertThat(builder.walkMinutes(place("1", 0, 0), place("2", 0, 0))).isEqualTo(1);
	}

	@Test
	void thirtyMinuteCourseFitsStayAndWalk() {
		List<KakaoPlace> places = List.of(place("1", 0, 100), place("2", STEP_200M, 300), place("3", 2 * STEP_200M, 500));

		List<PlannedStop> stops = builder.basicStops(places, DurationOption.MIN_30);

		assertThat(stops).extracting(stop -> stop.place().id()).containsExactly("1", "2");
		assertThat(stops).allMatch(stop -> stop.stayMinutes() == 10);
		assertThat(builder.totalMinutes(stops)).isEqualTo(24);
	}

	@Test
	void oneHourCourseStaysFifteenMinutesEach() {
		List<KakaoPlace> places = List.of(place("1", 0, 10), place("2", 0.0001, 20), place("3", 0.0002, 30),
				place("4", 0.0003, 40), place("5", 0.0004, 50));

		List<PlannedStop> stops = builder.basicStops(places, DurationOption.HOUR_1);

		assertThat(stops).hasSize(3);
		assertThat(stops).allMatch(stop -> stop.stayMinutes() == 15);
		assertThat(builder.totalMinutes(stops)).isLessThanOrEqualTo(60);
	}

	@Test
	void picksNearestPlaceFromCurrentStop() {
		List<KakaoPlace> places = List.of(place("1", 0, 100), place("2", -STEP_200M, 200), place("3", 0.0005, 250));

		List<PlannedStop> stops = builder.basicStops(places, DurationOption.MIN_30);

		assertThat(stops).extracting(stop -> stop.place().id()).startsWith("1", "3");
	}

	@Test
	void excludesWalkToFirstStop() {
		List<PlannedStop> stops = builder.basicStops(List.of(place("1", 0, 1400)), DurationOption.MIN_30);

		assertThat(builder.totalMinutes(stops)).isEqualTo(10);
	}

	@Test
	void emptyPlacesMakeEmptyCourse() {
		assertThat(builder.basicStops(List.of(), DurationOption.MIN_30)).isEmpty();
	}
}
