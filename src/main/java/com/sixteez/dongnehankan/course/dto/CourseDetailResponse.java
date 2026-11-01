package com.sixteez.dongnehankan.course.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.entity.CourseStop;
import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Place;
import com.sixteez.dongnehankan.course.entity.Province;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.entity.ResidentStory;
import com.sixteez.dongnehankan.my.entity.Feeling;
import com.sixteez.dongnehankan.my.entity.VisitRecord;

public record CourseDetailResponse(Long courseId, String name, String intro, String thumbnailUrl, RegionItem region,
		List<TagItem> tags, int durationMinutes, String transport, List<StopItem> stops, Me me) {

	public record RegionItem(Long regionId, String name, Province province) {
		static RegionItem from(Region region) {
			return new RegionItem(region.getId(), region.getName(), region.getProvince());
		}
	}

	public record TagItem(Long tagId, String name, String phrase, String icon) {
		static TagItem from(ExperienceTag tag) {
			return new TagItem(tag.getId(), tag.getName(), tag.getPhrase(), tag.getIcon());
		}
	}

	public record PlaceItem(Long placeId, String name, String address, BigDecimal lat, BigDecimal lng,
			String thumbnailUrl, boolean timeSensitive, String sourceUrl) {
		static PlaceItem from(Place place) {
			return new PlaceItem(place.getId(), place.getName(), place.getAddress(), place.getLat(), place.getLng(),
					place.thumbnailUrl(), place.isTimeSensitive(), place.getSourceUrl());
		}
	}

	public record StoryItem(Long storyId, String residentIntro, String reason, String howToEnjoy) {
		static StoryItem from(ResidentStory story) {
			return new StoryItem(story.getId(), story.getResidentIntro(), story.getReason(), story.getHowToEnjoy());
		}
	}

	public record StopItem(int order, PlaceItem place, int stayMinutes, Integer moveMinutesToNext,
			String highlightQuote, StoryItem highlightStory) {
		static StopItem from(CourseStop stop) {
			ResidentStory story = stop.getHighlightStory();
			boolean showStory = story != null && story.isVisible();
			return new StopItem(stop.getStopOrder(), PlaceItem.from(stop.getPlace()), stop.getStayMinutes(),
					stop.getMoveMinutesToNext(), showStory ? story.getReason() : null,
					showStory ? StoryItem.from(story) : null);
		}
	}

	public record RecordItem(Long recordId, Feeling feeling, LocalDate visitedOn) {
		public static RecordItem from(VisitRecord record) {
			return new RecordItem(record.getId(), record.getFeeling(), record.getVisitedOn());
		}
	}

	public record Me(boolean saved, List<RecordItem> records) {
	}

	public static CourseDetailResponse of(Course course, Me me) {
		return new CourseDetailResponse(course.getId(), course.getName(), course.getIntro(),
				course.getThumbnailUrl(), RegionItem.from(course.getRegion()),
				course.getTags().stream().map(TagItem::from).toList(), course.getDurationMinutes(),
				course.getTransport(), course.getStops().stream().map(StopItem::from).toList(), me);
	}
}
