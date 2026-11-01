package com.sixteez.dongnehankan.course.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.sixteez.dongnehankan.course.entity.Place;
import com.sixteez.dongnehankan.course.entity.PlacePhoto;
import com.sixteez.dongnehankan.course.entity.ResidentStory;

public record PlaceDetailResponse(Long placeId, String name, String address, BigDecimal lat, BigDecimal lng,
		List<String> photoUrls, LocalDate checkedOn, boolean timeSensitive, String sourceUrl, List<StoryItem> stories) {

	public record StoryItem(Long storyId, String residentIntro, String residentName, String residentPhotoUrl,
			String reason, String howToEnjoy) {
		static StoryItem from(ResidentStory story) {
			return new StoryItem(story.getId(), story.getResidentIntro(), story.publicName(), story.publicPhotoUrl(),
					story.getReason(), story.getHowToEnjoy());
		}
	}

	public static PlaceDetailResponse of(Place place, List<ResidentStory> stories) {
		return new PlaceDetailResponse(place.getId(), place.getName(), place.getAddress(), place.getLat(),
				place.getLng(), place.getPhotos().stream().map(PlacePhoto::getImageUrl).toList(), place.getCheckedOn(),
				place.isTimeSensitive(), place.getSourceUrl(), stories.stream().map(StoryItem::from).toList());
	}
}
