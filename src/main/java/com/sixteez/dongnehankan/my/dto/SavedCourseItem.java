package com.sixteez.dongnehankan.my.dto;

import java.time.OffsetDateTime;

import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;

public record SavedCourseItem(OffsetDateTime savedAt, boolean visited, CourseSummaryResponse course) {
}
