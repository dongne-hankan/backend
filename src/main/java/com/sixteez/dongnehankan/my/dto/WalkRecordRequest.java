package com.sixteez.dongnehankan.my.dto;

import java.time.LocalDate;
import java.util.List;

import com.sixteez.dongnehankan.my.entity.Feeling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WalkRecordRequest(@Valid @NotNull WalkCourse course, Feeling feeling,
		@Size(max = 50, message = "메모는 50자까지 쓸 수 있어요.") String memo, LocalDate visitedOn) {

	public record WalkCourse(@NotBlank @Size(max = 100) String name, @Size(max = 255) String intro,
			@Min(1) @Max(600) int durationMinutes, @NotNull Long tagId, @Valid @NotNull Center center,
			@NotEmpty @Size(max = 4) List<@Valid WalkStop> stops) {
	}

	public record Center(@NotNull @DecimalMin("-90") @DecimalMax("90") Double lat,
			@NotNull @DecimalMin("-180") @DecimalMax("180") Double lng) {
	}

	public record WalkStop(@NotBlank @Size(max = 30) String kakaoPlaceId,
			@Size(max = 500) @Pattern(regexp = "^https?://place\\.map\\.kakao\\.com/.*$") String kakaoPlaceUrl,
			@Min(0) @Max(600) int stayMinutes, @Min(0) @Max(600) Integer moveMinutesToNext) {
	}
}
