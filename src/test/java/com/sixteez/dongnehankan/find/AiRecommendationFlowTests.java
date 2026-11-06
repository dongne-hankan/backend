package com.sixteez.dongnehankan.find;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sixteez.dongnehankan.find.service.GeminiClient;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiRecommendationFlowTests {

	private static final String HOUR_WALK_IN_SUSEONG = "/api/find/ai-recommendations?regionId=1&tagId=1&duration=HOUR_1";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GeminiClient geminiClient;

	@Test
	void addsGeminiReasonsToRegionRecommendation() throws Exception {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.of("""
				{"reasons":[{"courseId":1,"reason":"첫 번째 이유"},{"courseId":2,"reason":"두 번째 이유"},
				            {"courseId":3,"reason":"세 번째 이유"}]}
				"""));

		mockMvc.perform(get(HOUR_WALK_IN_SUSEONG))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.recommendation.courses[0].courseId").value(3))
			.andExpect(jsonPath("$.data.recommendation.personalization.applied").value(false))
			.andExpect(jsonPath("$.data.reasons[0].courseId").value(3))
			.andExpect(jsonPath("$.data.reasons[0].reason").value("세 번째 이유"))
			.andExpect(jsonPath("$.data.aiGenerated").value(true));
	}

	@Test
	void keepsRecommendationWhenGeminiFails() throws Exception {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		mockMvc.perform(get("/api/find/ai-recommendations?regionId=1&tagId=1&duration=MIN_30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.recommendation.courses").isNotEmpty())
			.andExpect(jsonPath("$.data.reasons", hasSize(0)))
			.andExpect(jsonPath("$.data.aiGenerated").value(false));
	}

	@Test
	void keepsRecommendationErrors() throws Exception {
		mockMvc.perform(get("/api/find/ai-recommendations?regionId=1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CONDITION_REQUIRED"));
	}

	@Test
	void passesExcludedCoursesThrough() throws Exception {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		mockMvc.perform(get(HOUR_WALK_IN_SUSEONG + "&excludeCourseIds=3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.recommendation.courses[?(@.courseId == 3)]").isEmpty());
	}
}
