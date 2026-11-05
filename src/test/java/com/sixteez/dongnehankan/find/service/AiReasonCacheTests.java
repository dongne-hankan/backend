package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;
import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse.RegionItem;
import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse.TagItem;
import com.sixteez.dongnehankan.find.dto.DurationOption;

import tools.jackson.databind.json.JsonMapper;

class AiReasonCacheTests {

	private CourseSummaryResponse course(String quote) {
		return new CourseSummaryResponse(1L, "동네 산책", "짧게 걷는 코스", null, new RegionItem(1L, "수성구"),
				List.of(new TagItem(1L, "천천히 걷기")), 30, 2, quote, null);
	}

	@Test
	void hiddenQuoteCannotReuseOldGeneratedReason() {
		GeminiClient client = mock(GeminiClient.class);
		given(client.generateJson(anyString()))
			.willReturn(Optional.of("{\"reasons\":[{\"courseId\":1,\"reason\":\"이전 인용 기반 설명\"}]}"),
					Optional.of("{\"reasons\":[{\"courseId\":1,\"reason\":\"현재 정보 기반 설명\"}]}"));
		AiReasonWriter writer = new AiReasonWriter(client, JsonMapper.builder().build(), 1000);

		writer.write("천천히 걷기", DurationOption.MIN_30, List.of(course("철회된 인용")));
		assertThat(writer.write("천천히 걷기", DurationOption.MIN_30, List.of(course(null))))
			.containsEntry(1L, "현재 정보 기반 설명");
		ArgumentCaptor<String> prompts = ArgumentCaptor.forClass(String.class);
		verify(client, times(2)).generateJson(prompts.capture());
		assertThat(prompts.getAllValues().get(1)).doesNotContain("철회된 인용");
	}

	@Test
	void expiresReasonsAfterThirtyMinutes() {
		GeminiClient client = mock(GeminiClient.class);
		given(client.generateJson(anyString()))
			.willReturn(Optional.of("{\"reasons\":[{\"courseId\":1,\"reason\":\"추천 설명\"}]}"));
		Clock clock = mock(Clock.class);
		given(clock.millis()).willReturn(0L);
		AiReasonWriter writer = new AiReasonWriter(client, JsonMapper.builder().build(), 1000, clock);

		writer.write("천천히 걷기", DurationOption.MIN_30, List.of(course(null)));
		writer.write("천천히 걷기", DurationOption.MIN_30, List.of(course(null)));
		verify(client, times(1)).generateJson(anyString());
		given(clock.millis()).willReturn(Duration.ofMinutes(30).toMillis());
		writer.write("천천히 걷기", DurationOption.MIN_30, List.of(course(null)));
		verify(client, times(2)).generateJson(anyString());
	}
}
