package com.sixteez.dongnehankan;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
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

import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.AdministrativeArea;
import com.sixteez.dongnehankan.user.entity.User;
import com.sixteez.dongnehankan.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WalkFlowTests {

	private static final AtomicInteger KAKAO_IDS = new AtomicInteger(9000);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@MockitoBean
	private KakaoLocalClient kakaoLocalClient;

	private String token;

	@BeforeEach
	void createUser() {
		User user = userRepository
			.save(new User(String.valueOf(KAKAO_IDS.incrementAndGet()), "성민", LocalDateTime.now()));
		token = tokenProvider.createAccessToken(user.getId(), false);
	}

	@Test
	void walkInAPilotRegionFillsTheNeighborhood() throws Exception {
		givenArea("대구광역시", "수성구");

		walk(1, "WANT_AGAIN").andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.record.course.name").value("수성못 물가 산책"))
			.andExpect(jsonPath("$.data.record.course.visible").value(false))
			.andExpect(jsonPath("$.data.neighborhood.newlyFilled").value(true));

		authed(get("/api/my/neighborhood")).andExpect(jsonPath("$.data.grid.filledCellCount").value(1))
			.andExpect(jsonPath("$.data.summary.recordCount").value(1));
		authed(get("/api/my/records")).andExpect(jsonPath("$.data.items", hasSize(1)));
	}

	@Test
	void walkCourseIsNotShownInRecommendations() throws Exception {
		givenArea("대구광역시", "수성구");
		walk(1, "WANT_AGAIN").andExpect(status().isCreated());

		mockMvc.perform(get("/api/find/recommendations?regionId=1&tagId=1&duration=HOUR_1"))
			.andExpect(jsonPath("$.data.courses[?(@.name == '수성못 물가 산책')]").isEmpty());
	}

	@Test
	void walkInANewDistrictAddsTheRegionToTheGrid() throws Exception {
		givenArea("대구광역시", "중구");

		walk(2, "GOOD").andExpect(status().isCreated());

		authed(get("/api/my/neighborhood"))
			.andExpect(jsonPath("$.data.grid.regions", hasSize(1)))
			.andExpect(jsonPath("$.data.grid.regions[0].name").value("중구"))
			.andExpect(jsonPath("$.data.grid.visitedRegionCount").value(1))
			.andExpect(jsonPath("$.data.grid.filledCellCount").value(1));
	}

	@Test
	void walkOutsideDaeguAndGyeongbukIsRejected() throws Exception {
		givenArea("서울특별시", "중구");

		walk(1, "GOOD").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("OUT_OF_SERVICE_AREA"));
	}

	@Test
	void walkNeedsAFeelingAndAValidPlaceLink() throws Exception {
		givenArea("대구광역시", "수성구");

		authed(post("/api/my/walks").contentType(MediaType.APPLICATION_JSON).content(body(1, null)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("FEELING_REQUIRED"));

		String badLink = body(1, "GOOD").replace("http://place.map.kakao.com/123", "https://example.com/x");
		authed(post("/api/my/walks").contentType(MediaType.APPLICATION_JSON).content(badLink))
			.andExpect(status().isBadRequest());

		authed(get("/api/my/records")).andExpect(jsonPath("$.data.items", hasSize(0)));
	}

	@Test
	void guestsCannotRecordWalks() throws Exception {
		mockMvc.perform(post("/api/my/walks").contentType(MediaType.APPLICATION_JSON).content(body(1, "GOOD")))
			.andExpect(status().isUnauthorized());
	}

	private void givenArea(String province, String district) {
		given(kakaoLocalClient.findArea(anyDouble(), anyDouble()))
			.willReturn(Optional.of(new AdministrativeArea(province, district)));
	}

	private ResultActions walk(long tagId, String feeling) throws Exception {
		return authed(post("/api/my/walks").contentType(MediaType.APPLICATION_JSON).content(body(tagId, feeling)));
	}

	private String body(long tagId, String feeling) {
		String feelingField = feeling == null ? "" : ",\"feeling\":\"" + feeling + "\"";
		return """
				{"course":{"name":"수성못 물가 산책","intro":"물가를 따라 걷는 30분","durationMinutes":30,"tagId":%d,
				"center":{"lat":35.8272,"lng":128.6175},
				"stops":[{"kakaoPlaceId":"123","kakaoPlaceUrl":"http://place.map.kakao.com/123","stayMinutes":20,"moveMinutesToNext":5},
				{"kakaoPlaceId":"456","kakaoPlaceUrl":null,"stayMinutes":5,"moveMinutesToNext":null}]}%s,"memo":"좋았어요"}
				""".formatted(tagId, feelingField);
	}

	private ResultActions authed(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
			throws Exception {
		return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}
}
