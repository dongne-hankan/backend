package com.sixteez.dongnehankan.find.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;

@Component
public class KakaoLocalClient {

	private static final Logger log = LoggerFactory.getLogger(KakaoLocalClient.class);

	// 공원·산책로는 카테고리 코드가 없어 키워드로 찾는다. 음식점, 카페, 편의점, 주차장은 넣지 않는다.
	private static final Set<String> EXCLUDED_GROUPS = Set.of("FD6", "CE7", "CS2", "PK6");

	private static final List<String> EXCLUDED_CATEGORY_WORDS = List.of("음식점", "카페", "편의점", "주차장");

	private final RestClient restClient;

	private final String restApiKey;

	// REST API 키를 따로 넣지 않으면 같은 앱의 KAKAO_CLIENT_ID를 쓴다.
	@Autowired
	public KakaoLocalClient(@Value("${KAKAO_REST_API_KEY:}") String restApiKey,
			@Value("${KAKAO_CLIENT_ID:}") String clientId,
			@Value("${KAKAO_LOCAL_BASE_URL:https://dapi.kakao.com}") String baseUrl) {
		this(RestClient.builder().requestFactory(requestFactory()), firstNonBlank(restApiKey, clientId), baseUrl);
	}

	static String firstNonBlank(String first, String second) {
		return StringUtils.hasText(first) ? first : second;
	}

	KakaoLocalClient(RestClient.Builder builder, String restApiKey, String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
		this.restApiKey = restApiKey;
	}

	private static SimpleClientHttpRequestFactory requestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(3));
		requestFactory.setReadTimeout(Duration.ofSeconds(5));
		return requestFactory;
	}

	public record AdministrativeArea(String province, String district) {
	}

	private record RegionCodeResponse(List<RegionDocument> documents) {
	}

	private record RegionDocument(@JsonProperty("region_type") String regionType,
			@JsonProperty("region_1depth_name") String depth1, @JsonProperty("region_2depth_name") String depth2) {
	}

	public record KakaoPlace(String id, String name, String address, double lat, double lng, String placeUrl,
			int distanceMeters) {
	}

	private record PlaceSearchResponse(List<PlaceDocument> documents) {
	}

	private record PlaceDocument(String id, @JsonProperty("place_name") String placeName,
			@JsonProperty("address_name") String addressName, @JsonProperty("road_address_name") String roadAddressName,
			String x, String y, @JsonProperty("place_url") String placeUrl, String distance,
			@JsonProperty("category_group_code") String groupCode, @JsonProperty("category_name") String categoryName) {

		boolean walkable() {
			if (StringUtils.hasText(groupCode) && EXCLUDED_GROUPS.contains(groupCode)) {
				return false;
			}
			return categoryName == null || EXCLUDED_CATEGORY_WORDS.stream().noneMatch(categoryName::contains);
		}

		KakaoPlace toPlace() {
			String address = StringUtils.hasText(roadAddressName) ? roadAddressName : addressName;
			int meters = StringUtils.hasText(distance) ? Integer.parseInt(distance) : Integer.MAX_VALUE;
			return new KakaoPlace(id, placeName, address, Double.parseDouble(y), Double.parseDouble(x), placeUrl,
					meters);
		}
	}

	public List<KakaoPlace> searchNearby(double lat, double lng, int radiusMeters) {
		requireKey();
		try {
			List<PlaceDocument> attractions = search("/v2/local/search/category.json", "category_group_code", "AT4",
					lat, lng, radiusMeters);
			List<PlaceDocument> culture = search("/v2/local/search/category.json", "category_group_code", "CT1",
					lat, lng, radiusMeters);
			List<PlaceDocument> parks = search("/v2/local/search/keyword.json", "query", "공원", lat, lng,
					radiusMeters);
			List<PlaceDocument> trails = search("/v2/local/search/keyword.json", "query", "산책로", lat, lng,
					radiusMeters);
			Map<String, KakaoPlace> unique = new LinkedHashMap<>();
			Stream.of(attractions, culture, parks, trails)
				.flatMap(List::stream)
				.filter(PlaceDocument::walkable)
				.map(PlaceDocument::toPlace)
				.forEach(place -> unique.putIfAbsent(place.id(), place));
			return unique.values().stream().sorted(Comparator.comparingInt(KakaoPlace::distanceMeters)).toList();
		}
		catch (RestClientException | IllegalArgumentException e) {
			log.warn("카카오 장소 검색 실패: {}", e.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
		}
	}

	private List<PlaceDocument> search(String path, String filterName, String filterValue, double lat, double lng,
			int radiusMeters) {
		PlaceSearchResponse response = restClient.get()
			.uri(uri -> uri.path(path)
				.queryParam(filterName, filterValue)
				.queryParam("x", lng)
				.queryParam("y", lat)
				.queryParam("radius", radiusMeters)
				.queryParam("sort", "distance")
				.build())
			.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
			.retrieve()
			.body(PlaceSearchResponse.class);
		return response == null || response.documents() == null ? List.of() : response.documents();
	}

	private void requireKey() {
		if (!StringUtils.hasText(restApiKey)) {
			log.warn("KAKAO_REST_API_KEY가 비어 있어 카카오 로컬 API를 부를 수 없습니다.");
			throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
		}
	}

	public Optional<AdministrativeArea> findArea(double lat, double lng) {
		requireKey();
		try {
			RegionCodeResponse response = restClient.get()
				.uri(uri -> uri.path("/v2/local/geo/coord2regioncode.json")
					.queryParam("x", lng)
					.queryParam("y", lat)
					.build())
				.header(HttpHeaders.AUTHORIZATION, "KakaoAK " + restApiKey)
				.retrieve()
				.body(RegionCodeResponse.class);
			if (response == null || response.documents() == null) {
				return Optional.empty();
			}
			return response.documents()
				.stream()
				.sorted((a, b) -> Boolean.compare(!"B".equals(a.regionType()), !"B".equals(b.regionType())))
				.filter(document -> StringUtils.hasText(document.depth1()) && StringUtils.hasText(document.depth2()))
				.findFirst()
				.map(document -> new AdministrativeArea(document.depth1(), document.depth2()));
		}
		catch (RestClientException e) {
			log.warn("카카오 로컬 API 호출 실패: {}", e.getClass().getSimpleName());
			throw new BusinessException(ErrorCode.KAKAO_SERVER_ERROR);
		}
	}
}
