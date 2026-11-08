package com.sixteez.dongnehankan.my.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.sixteez.dongnehankan.my.entity.Feeling;

public record NeighborhoodResponse(Grid grid, Summary summary) {

	public record RegionItem(Long regionId, String name) {
	}

	public record TagItem(Long tagId, String name) {
	}

	public record Cell(Long regionId, Long tagId, boolean filled, int recordCount, LocalDate lastVisitedOn) {
	}

	public record Grid(List<RegionItem> regions, List<TagItem> tags, List<Cell> cells, int filledCellCount,
			int totalCellCount, int visitedRegionCount) {
	}

	public record Summary(int recordCount, long savedCourseCount, Map<Feeling, Long> feelingCounts, TagItem topTag,
			String topTagMessage) {
	}
}
