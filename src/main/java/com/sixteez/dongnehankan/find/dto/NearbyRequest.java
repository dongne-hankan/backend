package com.sixteez.dongnehankan.find.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NearbyRequest(@DecimalMin("-90") @DecimalMax("90") Double lat,
		@DecimalMin("-180") @DecimalMax("180") Double lng, Long regionId, Long tagId,
		@NotNull DurationOption duration, @Size(max = 100) String mood) {

	public boolean hasCoordinates() {
		return lat != null && lng != null;
	}

	// 좌표 한 쌍이나 regionId 중 하나는 있어야 한다.
	@AssertTrue
	boolean isLocated() {
		return hasCoordinates() || (lat == null && lng == null && regionId != null);
	}
}
