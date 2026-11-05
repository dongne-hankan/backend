package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;

import tools.jackson.databind.json.JsonMapper;

class NearbyAiValidationTests {

	private final GeminiClient client = mock(GeminiClient.class);

	private final NearbyAiPlanner planner = new NearbyAiPlanner(client, new NearbyCourseBuilder(),
			JsonMapper.builder().build());

	private final List<KakaoPlace> candidates = List.of(new KakaoPlace("11", "공원", "대구 수성구", 35.8,
			128.6, null, 100));

	@Test
	void skipsAiWhenThereAreNoVerifiedCandidates() {
		assertThat(planner.plan(List.of(), DurationOption.MIN_30, null)).isEmpty();
		verifyNoInteractions(client);
	}

	@Test
	void fallsBackWhenCourseExplanationIsMissing() {
		given(client.generateJson(anyString())).willReturn(Optional.of("""
				{"name":"산책","intro":"소개","reason":" ","stops":[{"placeId":"11","reason":"이유"}]}
				"""));
		assertThat(planner.plan(candidates, DurationOption.MIN_30, null)).isEmpty();
	}

	@Test
	void fallsBackWhenAllStopExplanationsAreMissing() {
		given(client.generateJson(anyString())).willReturn(Optional.of("""
				{"name":"산책","intro":"소개","reason":"이유","stops":[{"placeId":"11"}]}
				"""));
		assertThat(planner.plan(candidates, DurationOption.MIN_30, null)).isEmpty();
	}
}
