package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.sixteez.dongnehankan.find.service.KakaoLocalClient.AdministrativeArea;
import com.sixteez.dongnehankan.global.exception.BusinessException;
class KakaoLocalClientTests {

	private static final String BASE_URL = "https://kakao.test";

	private RestClient.Builder builder;

	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
	}

	@Test
	void convertsCoordinatesToLegalDongArea() {
		KakaoLocalClient client = new KakaoLocalClient(builder, "test-key", BASE_URL);
		server.expect(requestTo(startsWith(BASE_URL + "/v2/local/geo/coord2regioncode.json")))
			.andExpect(method(HttpMethod.GET))
			.andExpect(queryParam("x", "128.6306"))
			.andExpect(queryParam("y", "35.8581"))
			.andExpect(header("Authorization", "KakaoAK test-key"))
			.andRespond(withSuccess("""
					{"documents":[
					  {"region_type":"H","region_1depth_name":"대구광역시","region_2depth_name":"수성구"},
					  {"region_type":"B","region_1depth_name":"대구광역시","region_2depth_name":"수성구"}
					]}
					""", MediaType.APPLICATION_JSON));

		assertThat(client.findArea(35.8581, 128.6306)).contains(new AdministrativeArea("대구광역시", "수성구"));
		server.verify();
	}

	@Test
	void returnsEmptyWhenNoArea() {
		KakaoLocalClient client = new KakaoLocalClient(builder, "test-key", BASE_URL);
		server.expect(requestTo(startsWith(BASE_URL)))
			.andRespond(withSuccess("{\"documents\":[]}", MediaType.APPLICATION_JSON));

		assertThat(client.findArea(35.8581, 128.6306)).isEmpty();
	}

	@Test
	void throwsOnKakaoError() {
		KakaoLocalClient client = new KakaoLocalClient(builder, "test-key", BASE_URL);
		server.expect(requestTo(startsWith(BASE_URL))).andRespond(withServerError());

		assertThatThrownBy(() -> client.findArea(35.8581, 128.6306)).isInstanceOf(BusinessException.class);
	}

	@Test
	void mergesAttractionsAndParksByDistance() {
		KakaoLocalClient client = new KakaoLocalClient(builder, "test-key", BASE_URL);
		server.expect(requestTo(startsWith(BASE_URL + "/v2/local/search/category.json")))
			.andExpect(queryParam("category_group_code", "AT4"))
			.andExpect(queryParam("x", "128.6306"))
			.andExpect(queryParam("y", "35.8581"))
			.andExpect(queryParam("radius", "1500"))
			.andExpect(queryParam("sort", "distance"))
			.andExpect(header("Authorization", "KakaoAK test-key"))
			.andRespond(withSuccess("""
					{"documents":[
					  {"id":"1","place_name":"수성못","address_name":"대구 수성구 두산동","road_address_name":"",
					   "x":"128.6175","y":"35.8285","place_url":"http://place.map.kakao.com/1","distance":"300"},
					  {"id":"2","place_name":"들안길","address_name":"대구 수성구 상동","road_address_name":"대구 수성구 들안로 1",
					   "x":"128.6200","y":"35.8300","place_url":"http://place.map.kakao.com/2","distance":"900"}
					]}
					""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(startsWith(BASE_URL + "/v2/local/search/keyword.json")))
			.andExpect(queryParam("radius", "1500"))
			.andRespond(withSuccess("""
					{"documents":[
					  {"id":"1","place_name":"수성못","address_name":"대구 수성구 두산동","road_address_name":"",
					   "x":"128.6175","y":"35.8285","place_url":"http://place.map.kakao.com/1","distance":"300"},
					  {"id":"3","place_name":"범어공원","address_name":"대구 수성구 범어동","road_address_name":"",
					   "x":"128.6300","y":"35.8600","place_url":"http://place.map.kakao.com/3","distance":"500"}
					]}
					""", MediaType.APPLICATION_JSON));

		var places = client.searchNearby(35.8581, 128.6306, 1500);

		assertThat(places).extracting(KakaoLocalClient.KakaoPlace::name).containsExactly("수성못", "범어공원", "들안길");
		assertThat(places.get(2).address()).isEqualTo("대구 수성구 들안로 1");
		assertThat(places.get(0).address()).isEqualTo("대구 수성구 두산동");
		assertThat(places.get(0).lat()).isEqualTo(35.8285);
		assertThat(places.get(0).lng()).isEqualTo(128.6175);
		assertThat(places.get(0).placeUrl()).isEqualTo("http://place.map.kakao.com/1");
		server.verify();
	}

	@Test
	void throwsWhenPlaceSearchFails() {
		KakaoLocalClient client = new KakaoLocalClient(builder, "test-key", BASE_URL);
		server.expect(requestTo(startsWith(BASE_URL))).andRespond(withServerError());

		assertThatThrownBy(() -> client.searchNearby(35.8581, 128.6306, 1500)).isInstanceOf(BusinessException.class);
	}

	@Test
	void fallsBackToClientIdWhenRestKeyIsBlank() {
		assertThat(KakaoLocalClient.firstNonBlank("", "client-id")).isEqualTo("client-id");
		assertThat(KakaoLocalClient.firstNonBlank(null, "client-id")).isEqualTo("client-id");
		assertThat(KakaoLocalClient.firstNonBlank("rest-key", "client-id")).isEqualTo("rest-key");
	}

	@Test
	void throwsWithoutCallingWhenKeyIsMissing() {
		KakaoLocalClient client = new KakaoLocalClient(builder, "", BASE_URL);

		assertThatThrownBy(() -> client.findArea(35.8581, 128.6306)).isInstanceOf(BusinessException.class);
		server.verify();
	}
}
