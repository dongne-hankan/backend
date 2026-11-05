package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;
import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse.RegionItem;
import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse.TagItem;
import com.sixteez.dongnehankan.find.dto.DurationOption;

import tools.jackson.databind.json.JsonMapper;

class AiReasonWriterTests {

	private static final List<CourseSummaryResponse> COURSES = List.of(
			course(1L, "수성못 둘레길", "물가를 따라 걷는 길"), course(2L, "범어천 산책", "개천을 따라 걷는 길"));

	private GeminiClient geminiClient;

	private AiReasonWriter writer;

	private static CourseSummaryResponse course(Long id, String name, String intro) {
		return new CourseSummaryResponse(id, name, intro, null, new RegionItem(1L, "수성구"),
				List.of(new TagItem(1L, "천천히 걷기")), 50, 3, "주민이 아끼는 길", null);
	}

	@BeforeEach
	void setUp() {
		geminiClient = mock(GeminiClient.class);
		writer = new AiReasonWriter(geminiClient, JsonMapper.builder().build(), 200);
	}

	@Test
	void writesReasonsForReturnedCourses() {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.of("""
				{"reasons":[{"courseId":2,"reason":"개천 소리를 들으며 걷기 좋아요"},
				            {"courseId":1,"reason":"평탄한 물가 길이라 천천히 걷기 좋아요"}]}
				"""));

		Map<Long, String> reasons = writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES);

		assertThat(reasons).containsExactly(Map.entry(1L, "평탄한 물가 길이라 천천히 걷기 좋아요"),
				Map.entry(2L, "개천 소리를 들으며 걷기 좋아요"));
	}

	@Test
	void ignoresUnknownCoursesAndBlankReasons() {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.of("""
				{"reasons":[{"courseId":99,"reason":"없는 코스"},{"courseId":1,"reason":"  "},
				            {"courseId":2,"reason":"좋아요"}]}
				"""));

		assertThat(writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES)).containsOnlyKeys(2L);
	}

	@Test
	void returnsEmptyOnBrokenJson() {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.of("이유를 쓸 수 없어요"));

		assertThat(writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES)).isEmpty();
	}

	@Test
	void returnsEmptyWhenGeminiIsTooSlow() {
		given(geminiClient.generateJson(anyString())).willAnswer(invocation -> {
			Thread.sleep(2000);
			return Optional.of("{\"reasons\":[{\"courseId\":1,\"reason\":\"늦은 답\"}]}");
		});

		long start = System.nanoTime();
		Map<Long, String> reasons = writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES);
		long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

		assertThat(reasons).isEmpty();
		assertThat(elapsedMillis).isLessThan(1000);
	}

	@Test
	void slowReasonsAreCachedForTheNextRequest() throws Exception {
		given(geminiClient.generateJson(anyString())).willAnswer(invocation -> {
			Thread.sleep(400);
			return Optional.of("{\"reasons\":[{\"courseId\":1,\"reason\":\"늦게 온 이유\"},{\"courseId\":2,\"reason\":\"둘째\"}]}");
		});

		assertThat(writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES)).isEmpty();
		Thread.sleep(600);

		assertThat(writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES)).containsEntry(1L, "늦게 온 이유")
			.containsEntry(2L, "둘째");
		verify(geminiClient, times(1)).generateJson(anyString());
	}

	@Test
	void cacheIsSeparatedByCondition() {
		given(geminiClient.generateJson(anyString()))
			.willReturn(Optional.of("{\"reasons\":[{\"courseId\":1,\"reason\":\"a\"},{\"courseId\":2,\"reason\":\"b\"}]}"));

		writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES);
		writer.write("혼자 쉬기", DurationOption.HOUR_1, COURSES);

		verify(geminiClient, times(2)).generateJson(anyString());
	}

	@Test
	void skipsGeminiWithoutCourses() {
		assertThat(writer.write("천천히 걷기", DurationOption.HOUR_1, List.of())).isEmpty();

		verify(geminiClient, never()).generateJson(anyString());
	}

	@Test
	void promptIncludesConditionAndCourses() {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		writer.write("천천히 걷기", DurationOption.HOUR_1, COURSES);

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		verify(geminiClient).generateJson(prompt.capture());
		assertThat(prompt.getValue()).contains("천천히 걷기", "1시간", "수성구", "수성못 둘레길", "물가를 따라 걷는 길",
				"범어천 산책", "주민이 아끼는 길");
	}
}
