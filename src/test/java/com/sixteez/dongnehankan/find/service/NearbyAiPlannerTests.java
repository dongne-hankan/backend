package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;
import com.sixteez.dongnehankan.find.service.NearbyAiPlanner.AiCourse;

import tools.jackson.databind.json.JsonMapper;

class NearbyAiPlannerTests {

	private static final List<KakaoPlace> CANDIDATES = List.of(
			new KakaoPlace("11", "수성못", "대구 수성구 두산동", 35.8285, 128.6175, "http://place.map.kakao.com/11", 300),
			new KakaoPlace("22", "범어공원", "대구 수성구 범어동", 35.8290, 128.6180, "http://place.map.kakao.com/22", 400),
			new KakaoPlace("33", "들안길", "대구 수성구 상동", 35.8295, 128.6185, "http://place.map.kakao.com/33", 500));

	private GeminiClient geminiClient;

	private NearbyAiPlanner planner;

	@BeforeEach
	void setUp() {
		geminiClient = mock(GeminiClient.class);
		planner = new NearbyAiPlanner(geminiClient, new NearbyCourseBuilder(), JsonMapper.builder().build());
	}

	private void geminiAnswers(String json) {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.of(json));
	}

	@Test
	void buildsCourseInGeminiOrder() {
		geminiAnswers("""
				{"name":"수성못 한 바퀴","intro":"물가를 천천히 걸어요","reason":"조용히 걷기 좋아요",
				 "stops":[{"placeId":"22","reason":"나무 그늘"},{"placeId":"11","reason":"호수 산책"}]}
				""");

		AiCourse course = planner.plan(CANDIDATES, DurationOption.MIN_30, "조용히 걷고 싶어요").orElseThrow();

		assertThat(course.name()).isEqualTo("수성못 한 바퀴");
		assertThat(course.intro()).isEqualTo("물가를 천천히 걸어요");
		assertThat(course.reason()).isEqualTo("조용히 걷기 좋아요");
		assertThat(course.stops()).extracting(stop -> stop.place().name()).containsExactly("범어공원", "수성못");
		assertThat(course.stops()).extracting(NearbyCourseBuilder.PlannedStop::reason)
			.containsExactly("나무 그늘", "호수 산책");
		assertThat(course.stops()).allMatch(stop -> stop.stayMinutes() == 10);
	}

	@Test
	void dropsUnknownAndDuplicatePlaces() {
		geminiAnswers("""
				{"name":"코스","intro":"소개","reason":"이유",
				 "stops":[{"placeId":"99","reason":"없는 곳"},{"placeId":"11","reason":"a"},{"placeId":"11","reason":"b"}]}
				""");

		AiCourse course = planner.plan(CANDIDATES, DurationOption.MIN_30, null).orElseThrow();

		assertThat(course.stops()).extracting(stop -> stop.place().id()).containsExactly("11");
	}

	@Test
	void trimsLastStopsWhenOverTime() {
		geminiAnswers("""
				{"name":"코스","intro":"소개","reason":"이유",
				 "stops":[{"placeId":"11","reason":"a"},{"placeId":"22","reason":"b"},{"placeId":"33","reason":"c"}]}
				""");

		AiCourse course = planner.plan(CANDIDATES, DurationOption.MIN_30, null).orElseThrow();

		assertThat(course.stops()).extracting(stop -> stop.place().id()).containsExactly("11", "22");
	}

	@Test
	void returnsEmptyWhenNoStopsRemain() {
		geminiAnswers("""
				{"name":"코스","intro":"소개","reason":"이유","stops":[{"placeId":"99","reason":"없는 곳"}]}
				""");

		assertThat(planner.plan(CANDIDATES, DurationOption.MIN_30, null)).isEmpty();
	}

	@Test
	void returnsEmptyOnBrokenJson() {
		geminiAnswers("코스를 만들 수 없어요");

		assertThat(planner.plan(CANDIDATES, DurationOption.MIN_30, null)).isEmpty();
	}

	@Test
	void returnsEmptyWithoutCourseName() {
		geminiAnswers("""
				{"intro":"소개","reason":"이유","stops":[{"placeId":"11","reason":"a"}]}
				""");

		assertThat(planner.plan(CANDIDATES, DurationOption.MIN_30, null)).isEmpty();
	}

	@Test
	void returnsEmptyWhenGeminiFails() {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		assertThat(planner.plan(CANDIDATES, DurationOption.MIN_30, null)).isEmpty();
	}

	@Test
	void promptIncludesCandidatesMoodAndDuration() {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		planner.plan(CANDIDATES, DurationOption.HOUR_1, "조용히 걷고 싶어요");

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(geminiClient).generateJson(prompt.capture());
		assertThat(prompt.getValue()).contains("11", "수성못", "22", "범어공원", "33", "들안길", "조용히 걷고 싶어요", "60분");
	}
}
