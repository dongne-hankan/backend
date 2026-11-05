package com.sixteez.dongnehankan.find.dto;

import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.find.dto.FindOptionsResponse.RegionItem;

public record FindRegionResponse(RegionItem region, String message) {

	public static final String OUTSIDE_MESSAGE = "아직 이 위치는 추천 지역이 아니에요. 지역을 직접 골라 주세요.";

	public static FindRegionResponse inside(Region region) {
		return new FindRegionResponse(RegionItem.from(region), null);
	}

	public static FindRegionResponse outside() {
		return new FindRegionResponse(null, OUTSIDE_MESSAGE);
	}
}
