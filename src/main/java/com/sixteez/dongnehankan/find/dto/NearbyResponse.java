package com.sixteez.dongnehankan.find.dto;

import java.util.List;

public record NearbyResponse(Center center, List<CourseItem> courses, boolean aiGenerated, String message) {

	public static final String EMPTY_MESSAGE = "근처에서 걸을 만한 장소를 찾지 못했어요. 지역을 골라 추천을 받아 보세요.";

	public static final String OUTSIDE_MESSAGE = "대구·경북 안에서만 산책 코스를 추천해요. 지역을 골라 추천을 받아 보세요.";

	public record Center(double lat, double lng) {
	}

	public record CourseItem(Long courseId, String name, String intro, String reason, int durationMinutes,
			List<StopItem> stops) {
	}

	public record StopItem(int order, String name, String address, double lat, double lng, int stayMinutes,
			Integer moveMinutesToNext, String reason, String kakaoPlaceUrl) {
	}

	public static NearbyResponse empty(Center center) {
		return new NearbyResponse(center, List.of(), false, EMPTY_MESSAGE);
	}

	public static NearbyResponse outside(Center center) {
		return new NearbyResponse(center, List.of(), false, OUTSIDE_MESSAGE);
	}
}
