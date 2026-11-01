package com.sixteez.dongnehankan.my.dto;

import java.time.OffsetDateTime;

public record SaveCourseResponse(Long courseId, boolean saved, OffsetDateTime savedAt) {
}
