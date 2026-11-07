package com.sixteez.dongnehankan.find;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.sixteez.dongnehankan.find.dto.NearbyResponse;
import com.sixteez.dongnehankan.find.service.GeminiClient;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.AdministrativeArea;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NearbyFlowTests {

	private static final List<KakaoPlace> PLACES = List.of(
			new KakaoPlace("11", "수성못", "대구 수성구 두산동", 35.8285, 128.6175, "http://place.map.kakao.com/11", 300),
			new KakaoPlace("22", "범어공원", "대구 수성구 범어동", 35.8290, 128.6180, "http://place.map.kakao.com/22", 400));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KakaoLocalClient kakaoLocalClient;

	@MockitoBean
	private GeminiClient geminiClient;

	@BeforeEach
	void setUp() {
		given(kakaoLocalClient.searchNearby(anyDouble(), anyDouble(), anyInt())).willReturn(PLACES);
		given(kakaoLocalClient.findArea(anyDouble(), anyDouble()))
			.willReturn(Optional.of(new AdministrativeArea("대구광역시", "중구")));
	}

	private ResultActions requestNearby(String body) throws Exception {
		return mockMvc.perform(post("/api/find/nearby").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void guestGetsGeminiCourse() throws Exception {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.of("""
				{"name":"수성못 한 바퀴","intro":"물가를 걸어요","reason":"조용히 걷기 좋아요",
				 "stops":[{"placeId":"22","reason":"나무 그늘"},{"placeId":"11","reason":"호수 산책"}]}
				"""));

		requestNearby("""
				{"lat":35.8714,"lng":128.6014,"duration":"MIN_30","mood":"조용히 걷고 싶어요"}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.center.lat").value(35.8714))
			.andExpect(jsonPath("$.data.center.lng").value(128.6014))
			.andExpect(jsonPath("$.data.aiGenerated").value(true))
			.andExpect(jsonPath("$.data.message").doesNotExist())
			.andExpect(jsonPath("$.data.courses", hasSize(1)))
			.andExpect(jsonPath("$.data.courses[0].name").value("수성못 한 바퀴"))
			.andExpect(jsonPath("$.data.courses[0].intro").value("물가를 걸어요"))
			.andExpect(jsonPath("$.data.courses[0].reason").value("조용히 걷기 좋아요"))
			.andExpect(jsonPath("$.data.courses[0].durationMinutes").value(22))
			.andExpect(jsonPath("$.data.courses[0].stops", hasSize(2)))
			.andExpect(jsonPath("$.data.courses[0].stops[0].order").value(1))
			.andExpect(jsonPath("$.data.courses[0].stops[0].name").value("범어공원"))
			.andExpect(jsonPath("$.data.courses[0].stops[0].address").value("대구 수성구 범어동"))
			.andExpect(jsonPath("$.data.courses[0].stops[0].lat").value(35.8290))
			.andExpect(jsonPath("$.data.courses[0].stops[0].lng").value(128.6180))
			.andExpect(jsonPath("$.data.courses[0].stops[0].stayMinutes").value(10))
			.andExpect(jsonPath("$.data.courses[0].stops[0].moveMinutesToNext").value(2))
			.andExpect(jsonPath("$.data.courses[0].stops[0].reason").value("나무 그늘"))
			.andExpect(jsonPath("$.data.courses[0].stops[0].kakaoPlaceUrl").value("http://place.map.kakao.com/22"))
			.andExpect(jsonPath("$.data.courses[0].stops[1].order").value(2))
			.andExpect(jsonPath("$.data.courses[0].stops[1].moveMinutesToNext").doesNotExist());
	}

	@Test
	void fallsBackToBasicCourseWhenGeminiFails() throws Exception {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		requestNearby("""
				{"lat":35.8714,"lng":128.6014,"duration":"MIN_30"}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.aiGenerated").value(false))
			.andExpect(jsonPath("$.data.courses", hasSize(1)))
			.andExpect(jsonPath("$.data.courses[0].name").value("내 주변 30분 산책"))
			.andExpect(jsonPath("$.data.courses[0].stops[0].name").value("수성못"))
			.andExpect(jsonPath("$.data.courses[0].stops[0].reason").value("지금 위치에서 약 300m 떨어진 곳이에요."));
	}

	@Test
	void returnsMessageWhenNoPlacesNearby() throws Exception {
		given(kakaoLocalClient.searchNearby(anyDouble(), anyDouble(), anyInt())).willReturn(List.of());

		requestNearby("""
				{"lat":35.8714,"lng":128.6014,"duration":"HOUR_1"}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courses", hasSize(0)))
			.andExpect(jsonPath("$.data.aiGenerated").value(false))
			.andExpect(jsonPath("$.data.message").value(NearbyResponse.EMPTY_MESSAGE));
	}

	@Test
	void rejectsMoodOver100Characters() throws Exception {
		String mood = "가".repeat(101);

		requestNearby("""
				{"lat":35.8714,"lng":128.6014,"duration":"MIN_30","mood":"%s"}
				""".formatted(mood)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void requiresDuration() throws Exception {
		requestNearby("""
				{"lat":35.8714,"lng":128.6014}
				""").andExpect(status().isBadRequest());
	}

	@Test
	void rejectsOutOfRangeCoordinates() throws Exception {
		requestNearby("""
				{"lat":135.0,"lng":128.6014,"duration":"MIN_30"}
				""").andExpect(status().isBadRequest());
	}

	@Test
	void refusesCoordinatesOutsideDaeguGyeongbuk() throws Exception {
		given(kakaoLocalClient.findArea(anyDouble(), anyDouble()))
			.willReturn(Optional.of(new AdministrativeArea("경상남도", "창원시 성산구")));

		requestNearby("""
				{"lat":35.2280,"lng":128.6811,"duration":"MIN_30"}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.courses", hasSize(0)))
			.andExpect(jsonPath("$.data.aiGenerated").value(false))
			.andExpect(jsonPath("$.data.message").value(NearbyResponse.OUTSIDE_MESSAGE));
		then(kakaoLocalClient).should(never()).searchNearby(anyDouble(), anyDouble(), anyInt());
	}

	@Test
	void refusesFarCoordinatesWithoutCallingKakao() throws Exception {
		requestNearby("""
				{"lat":37.5665,"lng":126.9780,"duration":"MIN_30"}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.message").value(NearbyResponse.OUTSIDE_MESSAGE));
		then(kakaoLocalClient).should(never()).findArea(anyDouble(), anyDouble());
	}

	@Test
	void refusesCoordinatesWithoutArea() throws Exception {
		given(kakaoLocalClient.findArea(anyDouble(), anyDouble())).willReturn(Optional.empty());

		requestNearby("""
				{"lat":36.0,"lng":129.9,"duration":"MIN_30"}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.message").value(NearbyResponse.OUTSIDE_MESSAGE));
	}

	@Test
	void recommendsFromRegionCenter() throws Exception {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		requestNearby("""
				{"regionId":1,"duration":"MIN_30"}
				""").andExpect(status().isOk())
			.andExpect(jsonPath("$.data.center.lat").value(35.8581))
			.andExpect(jsonPath("$.data.center.lng").value(128.6306))
			.andExpect(jsonPath("$.data.courses", hasSize(1)))
			.andExpect(jsonPath("$.data.courses[0].name").value("수성구 30분 산책"))
			.andExpect(jsonPath("$.data.courses[0].stops[0].reason").value("지역 중심에서 약 300m 떨어진 곳이에요."));
		then(kakaoLocalClient).should().searchNearby(eq(35.8581), eq(128.6306), anyInt());
		then(kakaoLocalClient).should(never()).findArea(anyDouble(), anyDouble());
	}

	@Test
	void passesTagToGemini() throws Exception {
		given(geminiClient.generateJson(anyString())).willReturn(Optional.empty());

		requestNearby("""
				{"regionId":2,"tagId":1,"duration":"HOUR_1","mood":"강가"}
				""").andExpect(status().isOk());

		ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
		then(geminiClient).should().generateJson(prompt.capture());
		org.assertj.core.api.Assertions.assertThat(prompt.getValue()).contains("천천히 걷고 싶어요", "강가");
	}

	@Test
	void rejectsUnknownRegion() throws Exception {
		requestNearby("""
				{"regionId":999,"duration":"MIN_30"}
				""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("REGION_NOT_FOUND"));
	}

	@Test
	void rejectsUnknownTag() throws Exception {
		requestNearby("""
				{"regionId":1,"tagId":999,"duration":"MIN_30"}
				""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("TAG_NOT_FOUND"));
	}

	@Test
	void requiresCoordinatesOrRegion() throws Exception {
		requestNearby("""
				{"duration":"MIN_30"}
				""").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		requestNearby("""
				{"lat":35.8714,"duration":"MIN_30"}
				""").andExpect(status().isBadRequest());
	}
}
