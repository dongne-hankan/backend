package com.sixteez.dongnehankan.my.dto;

import jakarta.validation.constraints.NotNull;

public record SaveCourseRequest(@NotNull Long courseId) {
}
