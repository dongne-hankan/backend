package com.sixteez.dongnehankan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;
import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.my.repository.VisitRecordRepository;
import com.sixteez.dongnehankan.user.entity.User;
import com.sixteez.dongnehankan.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MyFlowTests {

	private static final AtomicInteger KAKAO_IDS = new AtomicInteger(5000);

	private static final String HOUR_WALK_IN_SUSEONG = "/api/find/recommendations?regionId=1&tagId=1&duration=HOUR_1";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private VisitRecordRepository visitRecordRepository;

	private Long userId;

	private String token;

	@BeforeEach
	void createUser() {
		User user = newUser();
		userId = user.getId();
		token = tokenProvider.createAccessToken(userId, false);
	}

	@Test
	void savingACourseIsIdempotentAndCanBeCancelled() throws Exception {
		save(1).andExpect(status().isCreated()).andExpect(jsonPath("$.data.saved").value(true));
		save(1).andExpect(status().isOk());

		authed(get("/api/my/saved-courses")).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.items", hasSize(1)))
			.andExpect(jsonPath("$.data.items[0].visited").value(false))
			.andExpect(jsonPath("$.data.items[0].course.visible").value(true))
			.andExpect(jsonPath("$.data.totalElements").value(1));

		authed(get("/api/courses/1")).andExpect(jsonPath("$.data.me.saved").value(true));

		authed(delete("/api/my/saved-courses/1")).andExpect(status().isNoContent());
		authed(delete("/api/my/saved-courses/1")).andExpect(status().isNoContent());
		authed(get("/api/my/saved-courses")).andExpect(jsonPath("$.data.items", hasSize(0)));
	}

	@Test
	void hiddenOrUnknownCoursesCannotBeSaved() throws Exception {
		save(8).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("COURSE_HIDDEN"));
		save(9999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
	}

	@Test
	void recordFillsCellsOnlyTheFirstTime() throws Exception {
		record("{\"courseId\":1,\"feeling\":\"WANT_AGAIN\",\"memo\":\"좋았어요\"}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.record.feeling").value("WANT_AGAIN"))
			.andExpect(jsonPath("$.data.record.course.visible").value(true))
			.andExpect(jsonPath("$.data.neighborhood.filledCellCount").value(2))
			.andExpect(jsonPath("$.data.neighborhood.totalCellCount").value(9))
			.andExpect(jsonPath("$.data.neighborhood.newlyFilled").value(true));

		record("{\"courseId\":1,\"feeling\":\"GOOD\"}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.neighborhood.newlyFilled").value(false));

		authed(get("/api/my/neighborhood")).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.grid.cells", hasSize(9)))
			.andExpect(jsonPath("$.data.grid.filledCellCount").value(2))
			.andExpect(jsonPath("$.data.summary.recordCount").value(2))
			.andExpect(jsonPath("$.data.summary.feelingCounts.WANT_AGAIN").value(1))
			.andExpect(jsonPath("$.data.summary.feelingCounts.NOT_FOR_ME").value(0))
			.andExpect(jsonPath("$.data.summary.topTagMessage").isNotEmpty());

		authed(get("/api/my/records")).andExpect(jsonPath("$.data.items", hasSize(2)));
		authed(get("/api/courses/1")).andExpect(jsonPath("$.data.me.records", hasSize(2)));
	}

	@Test
	void emptyNeighborhoodHasNoTopTag() throws Exception {
		authed(get("/api/my/neighborhood")).andExpect(jsonPath("$.data.grid.filledCellCount").value(0))
			.andExpect(jsonPath("$.data.summary.topTag").isEmpty());
	}

	@Test
	void recordInputIsValidated() throws Exception {
		String tomorrow = LocalDate.now(ZoneId.of("Asia/Seoul")).plusDays(1).toString();
		record("{\"courseId\":1}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FEELING_REQUIRED"));
		record("{\"courseId\":1,\"feeling\":\"GOOD\",\"visitedOn\":\"" + tomorrow + "\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		record("{\"courseId\":1,\"feeling\":\"GOOD\",\"memo\":\"" + "가".repeat(51) + "\"}")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("memo"));
		record("{\"courseId\":1,\"feeling\":\"BAD\"}").andExpect(status().isBadRequest());
		record("{\"courseId\":9999,\"feeling\":\"GOOD\"}").andExpect(status().isNotFound());
	}

	@Test
	void recordCanBeChangedAndDeletedOnlyByItsOwner() throws Exception {
		int recordId = readRecordId(record("{\"courseId\":1,\"feeling\":\"GOOD\",\"memo\":\"처음\"}").andReturn());

		authed(jsonPatch("/api/my/records/" + recordId, "{\"feeling\":\"WANT_AGAIN\"}")).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.feeling").value("WANT_AGAIN"))
			.andExpect(jsonPath("$.data.memo").value("처음"));

		String otherToken = tokenProvider.createAccessToken(newUser().getId(), false);
		mockMvc
			.perform(jsonPatch("/api/my/records/" + recordId, "{\"memo\":\"남의 기록\"}")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("RECORD_FORBIDDEN"));

		authed(delete("/api/my/records/9999")).andExpect(status().isNotFound());
		authed(delete("/api/my/records/" + recordId)).andExpect(status().isNoContent());
		assertThat(visitRecordRepository.findAllWithCourse(userId)).isEmpty();
	}

	@Test
	void guestRecommendationStaysUnpersonalized() throws Exception {
		mockMvc.perform(get(HOUR_WALK_IN_SUSEONG))
			.andExpect(jsonPath("$.data.courses[0].courseId").value(3))
			.andExpect(jsonPath("$.data.courses[0].visible").doesNotExist())
			.andExpect(jsonPath("$.data.personalization.applied").value(false));
	}

	@Test
	void wantAgainRaisesACourseAboveTheDefaultOrder() throws Exception {
		record("{\"courseId\":1,\"feeling\":\"WANT_AGAIN\"}");

		authed(get(HOUR_WALK_IN_SUSEONG)).andExpect(jsonPath("$.data.courses[0].courseId").value(1))
			.andExpect(jsonPath("$.data.personalization.applied").value(true))
			.andExpect(jsonPath("$.data.personalization.message").value("'천천히 걷기'를 좋아하셔서 먼저 보여드려요"));
	}

	@Test
	void visitedCoursesDropBelowOthersUnlessWantedAgain() throws Exception {
		record("{\"courseId\":3,\"feeling\":\"GOOD\"}");

		authed(get(HOUR_WALK_IN_SUSEONG)).andExpect(jsonPath("$.data.courses[0].courseId").value(1))
			.andExpect(jsonPath("$.data.courses[1].courseId").value(3));
	}

	@Test
	void notForMeRemovesTheCourseAndLatestRecordWins() throws Exception {
		int recordId = readRecordId(record("{\"courseId\":1,\"feeling\":\"NOT_FOR_ME\"}").andReturn());

		authed(get(HOUR_WALK_IN_SUSEONG)).andExpect(jsonPath("$.data.courses", hasSize(1)))
			.andExpect(jsonPath("$.data.courses[0].courseId").value(3))
			.andExpect(jsonPath("$.data.personalization.applied").value(false));

		authed(jsonPatch("/api/my/records/" + recordId, "{\"feeling\":\"GOOD\"}")).andExpect(status().isOk());
		authed(get(HOUR_WALK_IN_SUSEONG)).andExpect(jsonPath("$.data.courses", hasSize(2)));
	}

	@Test
	void savedCourseGetsABonus() throws Exception {
		save(1);
		authed(get(HOUR_WALK_IN_SUSEONG)).andExpect(jsonPath("$.data.courses[0].courseId").value(1));
	}

	private User newUser() {
		return userRepository.save(new User(String.valueOf(KAKAO_IDS.incrementAndGet()), "성민", LocalDateTime.now()));
	}

	private ResultActions save(int courseId) throws Exception {
		return authed(jsonPost("/api/my/saved-courses", "{\"courseId\":" + courseId + "}"));
	}

	private ResultActions record(String body) throws Exception {
		return authed(jsonPost("/api/my/records", body));
	}

	private MockHttpServletRequestBuilder jsonPost(String url, String body) {
		return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private MockHttpServletRequestBuilder jsonPatch(String url, String body) {
		return patch(url).contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private ResultActions authed(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

	private int readRecordId(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8),
				"$.data.record.recordId");
	}
}
