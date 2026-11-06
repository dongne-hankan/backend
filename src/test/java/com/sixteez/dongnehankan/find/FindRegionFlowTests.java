package com.sixteez.dongnehankan.find;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.sixteez.dongnehankan.find.dto.FindRegionResponse;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.AdministrativeArea;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FindRegionFlowTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private KakaoLocalClient kakaoLocalClient;

	@Test
	void returnsPilotRegionForInsideLocation() throws Exception {
		given(kakaoLocalClient.findArea(35.8581, 128.6306))
			.willReturn(Optional.of(new AdministrativeArea("대구광역시", "수성구")));

		mockMvc.perform(get("/api/find/region").param("lat", "35.8581").param("lng", "128.6306"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.region.regionId").value(1))
			.andExpect(jsonPath("$.data.region.name").value("수성구"))
			.andExpect(jsonPath("$.data.region.province").value("DAEGU"))
			.andExpect(jsonPath("$.data.message").doesNotExist());
	}

	@Test
	void findsGyeongbukRegion() throws Exception {
		given(kakaoLocalClient.findArea(35.8562, 129.2247))
			.willReturn(Optional.of(new AdministrativeArea("경상북도", "경주시")));

		mockMvc.perform(get("/api/find/region").param("lat", "35.8562").param("lng", "129.2247"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.region.regionId").value(3));
	}

	@Test
	void choosesNoRegionOutsidePilot() throws Exception {
		given(kakaoLocalClient.findArea(37.4979, 127.0276))
			.willReturn(Optional.of(new AdministrativeArea("서울특별시", "강남구")));

		mockMvc.perform(get("/api/find/region").param("lat", "37.4979").param("lng", "127.0276"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.region").doesNotExist())
			.andExpect(jsonPath("$.data.message").value(FindRegionResponse.OUTSIDE_MESSAGE));
	}

	@Test
	void requiresMatchingProvince() throws Exception {
		given(kakaoLocalClient.findArea(37.5, 127.0))
			.willReturn(Optional.of(new AdministrativeArea("서울특별시", "수성구")));

		mockMvc.perform(get("/api/find/region").param("lat", "37.5").param("lng", "127.0"))
			.andExpect(jsonPath("$.data.region").doesNotExist());
	}

	@Test
	void skipsKakaoOutsideKorea() throws Exception {
		mockMvc.perform(get("/api/find/region").param("lat", "40.7128").param("lng", "-74.006"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.region").doesNotExist());

		verify(kakaoLocalClient, never()).findArea(anyDouble(), anyDouble());
	}

	@Test
	void returnsBadGatewayWhenKakaoFails() throws Exception {
		given(kakaoLocalClient.findArea(35.8581, 128.6306))
			.willThrow(new BusinessException(ErrorCode.KAKAO_SERVER_ERROR));

		mockMvc.perform(get("/api/find/region").param("lat", "35.8581").param("lng", "128.6306"))
			.andExpect(status().isBadGateway())
			.andExpect(jsonPath("$.code").value("KAKAO_SERVER_ERROR"));
	}

	@Test
	void requiresBothCoordinates() throws Exception {
		mockMvc.perform(get("/api/find/region").param("lat", "35.8581")).andExpect(status().isBadRequest());
	}
}
