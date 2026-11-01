package com.sixteez.dongnehankan.my.dto;

import java.time.LocalDate;

import com.sixteez.dongnehankan.my.entity.Feeling;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRecordRequest(@NotNull Long courseId, Feeling feeling,
		@Size(max = 50, message = "메모는 50자까지 쓸 수 있어요.") String memo, LocalDate visitedOn) {
}
