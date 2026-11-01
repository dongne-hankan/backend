package com.sixteez.dongnehankan.my.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.repository.ExperienceTagRepository;
import com.sixteez.dongnehankan.course.repository.RegionRepository;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse.Cell;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse.Grid;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse.RegionItem;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse.Summary;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse.TagItem;
import com.sixteez.dongnehankan.my.entity.Feeling;
import com.sixteez.dongnehankan.my.entity.VisitRecord;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
class NeighborhoodCalculator {

	private final RegionRepository regionRepository;

	private final ExperienceTagRepository experienceTagRepository;

	Grid grid(List<VisitRecord> records) {
		List<Region> regions = regionRepository.findAll(Sort.by("id"));
		List<ExperienceTag> tags = experienceTagRepository.findAllByOrderBySortOrder();

		List<Cell> cells = new ArrayList<>();
		for (Region region : regions) {
			for (ExperienceTag tag : tags) {
				cells.add(cell(region, tag, records));
			}
		}
		int filled = (int) cells.stream().filter(Cell::filled).count();
		return new Grid(regions.stream().map(region -> new RegionItem(region.getId(), region.getName())).toList(),
				tags.stream().map(tag -> new TagItem(tag.getId(), tag.getName())).toList(), cells, filled,
				cells.size());
	}

	Summary summary(List<VisitRecord> records, long savedCourseCount) {
		Map<Feeling, Long> feelingCounts = new EnumMap<>(Feeling.class);
		for (Feeling feeling : Feeling.values()) {
			feelingCounts.put(feeling, records.stream().filter(record -> record.getFeeling() == feeling).count());
		}
		Optional<TagItem> topTag = topTag(records);
		return new Summary(records.size(), savedCourseCount, feelingCounts, topTag.orElse(null),
				topTag.map(tag -> tag.name() + "를 가장 좋아해요").orElse(null));
	}

	NeighborhoodResponse response(List<VisitRecord> records, long savedCourseCount) {
		return new NeighborhoodResponse(grid(records), summary(records, savedCourseCount));
	}

	private Cell cell(Region region, ExperienceTag tag, List<VisitRecord> records) {
		List<VisitRecord> matched = records.stream()
			.filter(record -> record.getCourse().getRegion().getId().equals(region.getId()))
			.filter(record -> record.getCourse().hasTag(tag.getId()))
			.toList();
		LocalDate lastVisitedOn = matched.stream()
			.map(VisitRecord::getVisitedOn)
			.max(Comparator.naturalOrder())
			.orElse(null);
		return new Cell(region.getId(), tag.getId(), !matched.isEmpty(), matched.size(), lastVisitedOn);
	}

	// records는 최근순이므로 먼저 나온 태그가 동점일 때 앞선다.
	private Optional<TagItem> topTag(List<VisitRecord> records) {
		Map<Long, Integer> counts = new LinkedHashMap<>();
		Map<Long, TagItem> tags = new HashMap<>();
		records.stream().filter(record -> record.getFeeling() != Feeling.NOT_FOR_ME).forEach(record -> {
			for (ExperienceTag tag : record.getCourse().getTags()) {
				counts.merge(tag.getId(), 1, Integer::sum);
				tags.putIfAbsent(tag.getId(), new TagItem(tag.getId(), tag.getName()));
			}
		});
		return counts.entrySet()
			.stream()
			.reduce((best, next) -> next.getValue() > best.getValue() ? next : best)
			.map(entry -> tags.get(entry.getKey()));
	}
}
