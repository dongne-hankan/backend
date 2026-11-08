package com.sixteez.dongnehankan.find;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.course.repository.CourseRepository;
import com.sixteez.dongnehankan.find.service.GeminiClient;
import com.sixteez.dongnehankan.user.entity.User;
import com.sixteez.dongnehankan.user.repository.UserRepository;

@SpringBootTest(properties = {
		"FIND_FRESH_AI_ENABLED=true",
		"spring.datasource.url=jdbc:h2:mem:fresh_courses;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FreshCourseFlowTests {

	private static final String FIND = "/api/find/recommendations?regionId=1&tagId=1&duration=MIN_30";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private CourseRepository courses;
	@Autowired
	private UserRepository users;
	@Autowired
	private TokenProvider tokens;
	@MockitoBean
	private GeminiClient gemini;

	@Test
	void generatesAgainAndPersistsOnlyTheCourseTheMemberSaves() throws Exception {
		given(gemini.generateJson(anyString())).willReturn(course("새 물가 산책")).willReturn(course("두 번째 새 코스"));
		long before = courses.count();

		long first = id(mockMvc.perform(get(FIND)).andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
				.andExpect(jsonPath("$.data.courses[0].name").value("새 물가 산책")), "$.data.courses[0].courseId");
		long second = id(mockMvc.perform(get(FIND)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.courses[0].name").value("두 번째 새 코스")), "$.data.courses[0].courseId");
		assertThat(first).isNegative().isNotEqualTo(second);
		assertThat(courses.count()).isEqualTo(before);
		verify(gemini, times(2)).generateJson(anyString());

		String owner = newToken("88001");
		String other = newToken("88002");
		mockMvc.perform(get("/api/courses/{id}", first).header(HttpHeaders.AUTHORIZATION, owner))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.stops[0].place.placeId").value(101))
			.andExpect(jsonPath("$.data.me.saved").value(false));
		long saved = id(save(first, owner).andExpect(status().isCreated()), "$.data.courseId");
		assertThat(saved).isPositive();
		assertThat(courses.count()).isEqualTo(before + 1);
		save(first, owner).andExpect(status().isOk());
		assertThat(courses.count()).isEqualTo(before + 1);
		mockMvc.perform(get("/api/courses/{id}", first).header(HttpHeaders.AUTHORIZATION, owner))
			.andExpect(jsonPath("$.data.me.saved").value(true))
			.andExpect(jsonPath("$.data.stops[0].place.placeId").value(101));
		mockMvc.perform(get("/api/my/saved-courses").header(HttpHeaders.AUTHORIZATION, owner))
			.andExpect(jsonPath("$.data.items[0].course.name").value("새 물가 산책"));
		mockMvc.perform(get("/api/my/saved-courses").header(HttpHeaders.AUTHORIZATION, other))
			.andExpect(jsonPath("$.data.items").isEmpty());
		mockMvc.perform(post("/api/my/records").header(HttpHeaders.AUTHORIZATION, owner)
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"courseId":%d,"feeling":"GOOD"}
						""".formatted(first)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.record.course.courseId").value(saved));
		mockMvc.perform(delete("/api/my/saved-courses/{id}", first).header(HttpHeaders.AUTHORIZATION, owner))
			.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/my/saved-courses").header(HttpHeaders.AUTHORIZATION, owner))
			.andExpect(jsonPath("$.data.items").isEmpty());
	}

	@Test
	void showsRetryMessageInsteadOfReusingOldCoursesWhenAiFails() throws Exception {
		given(gemini.generateJson(anyString())).willReturn(Optional.empty());
		mockMvc.perform(get(FIND))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courses").isEmpty())
			.andExpect(jsonPath("$.data.fallback.message").value("지금은 AI 추천을 만들지 못했어요. 잠시 후 다시 검색해 주세요."));
	}


	@Test
	void doesNotReturnAnAlreadySeenRouteOnNextSearch() throws Exception {
		given(gemini.generateJson(anyString())).willReturn(course("처음 코스"));
		long first = id(mockMvc.perform(get(FIND)).andExpect(status().isOk()), "$.data.courses[0].courseId");
		mockMvc.perform(get(FIND + "&excludeCourseIds=" + first))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courses").isEmpty());
		verify(gemini).generateJson(contains("\"seenRoutes\":[[\"101\"]]"));
	}

	private static Optional<String> course(String name) {
		return Optional.of("""
				{"name":"%s","intro":"산책 코스예요","reason":"잠깐 걷기 좋아요",
				"stops":[{"placeId":"101","reason":"첫 장소예요"}]}
				""".formatted(name));
	}

	private String newToken(String kakaoId) {
		User user = users.save(new User(kakaoId, "테스트", LocalDateTime.now()));
		return "Bearer " + tokens.createAccessToken(user.getId(), false);
	}

	private ResultActions save(long courseId, String token) throws Exception {
		return mockMvc.perform(post("/api/my/saved-courses").header(HttpHeaders.AUTHORIZATION, token)
			.contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":" + courseId + "}"));
	}

	private long id(ResultActions result, String path) throws Exception {
		Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
		return id.longValue();
	}
}
