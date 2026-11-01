package com.sixteez.dongnehankan.my.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.my.entity.Feeling;
import com.sixteez.dongnehankan.my.entity.VisitRecord;

public record RecordResponse(Long recordId, CourseItem course, Feeling feeling, String memo, LocalDate visitedOn,
		String photoUrl, OffsetDateTime createdAt) {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	public record CourseItem(Long courseId, String name, String thumbnailUrl, boolean visible) {
		static CourseItem from(Course course) {
			return new CourseItem(course.getId(), course.getName(), course.getThumbnailUrl(), course.isVisible());
		}
	}

	public static RecordResponse from(VisitRecord record) {
		return new RecordResponse(record.getId(), CourseItem.from(record.getCourse()), record.getFeeling(),
				record.getMemo(), record.getVisitedOn(), record.getPhotoUrl(),
				record.getCreatedAt().atZone(SEOUL).toOffsetDateTime());
	}
}
