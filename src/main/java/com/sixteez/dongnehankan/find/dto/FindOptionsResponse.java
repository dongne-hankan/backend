package com.sixteez.dongnehankan.find.dto;

import java.util.Arrays;
import java.util.List;

import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Province;
import com.sixteez.dongnehankan.course.entity.Region;

public record FindOptionsResponse(List<RegionItem> regions, List<TagItem> tags, List<DurationItem> durations) {

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

	public record DurationItem(DurationOption code, String label, int minutes) {
		static DurationItem from(DurationOption duration) {
			return new DurationItem(duration, duration.getLabel(), duration.getMinutes());
		}
	}

	public static FindOptionsResponse of(List<Region> regions, List<ExperienceTag> tags) {
		return new FindOptionsResponse(regions.stream().map(RegionItem::from).toList(),
				tags.stream().map(TagItem::from).toList(),
				Arrays.stream(DurationOption.values()).map(DurationItem::from).toList());
	}
}
