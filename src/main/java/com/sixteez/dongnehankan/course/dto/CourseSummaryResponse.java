package com.sixteez.dongnehankan.course.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.entity.CourseStop;
import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.entity.ResidentStory;

public record CourseSummaryResponse(Long courseId, String name, String intro, String thumbnailUrl, RegionItem region,
		List<TagItem> tags, int durationMinutes, int stopCount, String highlightQuote,
		@JsonInclude(JsonInclude.Include.NON_NULL) Boolean visible) {

	public record RegionItem(Long regionId, String name) {
		static RegionItem from(Region region) {
			return new RegionItem(region.getId(), region.getName());
		}
	}

	public record TagItem(Long tagId, String name) {
		static TagItem from(ExperienceTag tag) {
			return new TagItem(tag.getId(), tag.getName());
		}
	}

	public static CourseSummaryResponse from(Course course) {
		return of(course, null);
	}

	public static CourseSummaryResponse withVisibility(Course course) {
		return of(course, course.isVisible());
	}

	private static CourseSummaryResponse of(Course course, Boolean visible) {
		return new CourseSummaryResponse(course.getId(), course.getName(), course.getIntro(),
				course.getThumbnailUrl(), RegionItem.from(course.getRegion()),
				course.getTags().stream().map(TagItem::from).toList(), course.getDurationMinutes(),
				course.getStops().size(), highlightQuote(course), visible);
	}

	private static String highlightQuote(Course course) {
		return course.getStops()
			.stream()
			.map(CourseStop::getHighlightStory)
			.filter(story -> story != null && story.isVisible())
			.map(ResidentStory::getReason)
			.findFirst()
			.orElse(null);
	}
}
