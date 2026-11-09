package com.sixteez.dongnehankan;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DongneHankanApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void memberApiRequiresLogin() throws Exception {
		mockMvc.perform(get("/api/my/saved-courses"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void livenessIsPublicForUptimeMonitors() throws Exception {
		mockMvc.perform(get("/actuator/health/liveness"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void unknownPublicPathReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/find/unknown"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void findOptionsReturnsConfirmedChoices() throws Exception {
		mockMvc.perform(get("/api/find/options"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.regions", hasSize(3)))
			.andExpect(jsonPath("$.data.tags", hasSize(3)))
			.andExpect(jsonPath("$.data.durations", hasSize(2)))
			.andExpect(jsonPath("$.data.durations[1].code").value("HOUR_1"));
	}

	@Test
	void recommendsOnlyCoursesFittingTheCondition() throws Exception {
		mockMvc.perform(get("/api/find/recommendations?regionId=1&tagId=1&duration=MIN_30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courses", hasSize(1)))
			.andExpect(jsonPath("$.data.courses[0].courseId").value(1))
			.andExpect(jsonPath("$.data.courses[0].stopCount").value(2))
			.andExpect(jsonPath("$.data.fallback.type").value("NONE"))
			.andExpect(jsonPath("$.data.hasMore").value(false));
	}

	@Test
	void excludedCoursesAreNotShownAgain() throws Exception {
		mockMvc.perform(get("/api/find/recommendations?regionId=1&tagId=1&duration=HOUR_1&excludeCourseIds=1,3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courses", hasSize(0)))
			.andExpect(jsonPath("$.data.fallback.type").value("NONE"));
	}

	@Test
	void fallsBackToLongerDuration() throws Exception {
		mockMvc.perform(get("/api/find/recommendations?regionId=2&tagId=3&duration=MIN_30"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courses[0].courseId").value(5))
			.andExpect(jsonPath("$.data.fallback.type").value("LONGER_DURATION"))
			.andExpect(jsonPath("$.data.fallback.appliedCondition.duration").value("HOUR_1"));
	}

	@Test
	void fallsBackToOtherTagInTheSameRegion() throws Exception {
		mockMvc.perform(get("/api/find/recommendations?regionId=3&tagId=3&duration=HOUR_1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.fallback.type").value("OTHER_TAG"))
			.andExpect(jsonPath("$.data.courses[0].region.regionId").value(3));
	}

	@Test
	void missingConditionIsRejected() throws Exception {
		mockMvc.perform(get("/api/find/recommendations?regionId=1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CONDITION_REQUIRED"));
	}

	@Test
	void removedDurationIsRejected() throws Exception {
		mockMvc.perform(get("/api/find/recommendations?regionId=1&tagId=1&duration=HALF_DAY"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void courseDetailIncludesStopsAndStory() throws Exception {
		mockMvc.perform(get("/api/courses/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.stops", hasSize(2)))
			.andExpect(jsonPath("$.data.stops[0].order").value(1))
			.andExpect(jsonPath("$.data.stops[0].moveMinutesToNext").value(5))
			.andExpect(jsonPath("$.data.stops[0].place.sourceUrl").isNotEmpty())
			.andExpect(jsonPath("$.data.stops[0].highlightStory.residentIntro").isNotEmpty())
			.andExpect(jsonPath("$.data.stops[1].moveMinutesToNext").isEmpty())
			.andExpect(jsonPath("$.data.me").isEmpty());
	}

	@Test
	void placeDetailHidesResidentNameWithoutConsent() throws Exception {
		mockMvc.perform(get("/api/places/101"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.stories", hasSize(1)))
			.andExpect(jsonPath("$.data.stories[0].residentName").isEmpty());
	}

	@Test
	void unknownCourseReturnsNotFound() throws Exception {
		mockMvc.perform(get("/api/courses/9999"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
	}
}
