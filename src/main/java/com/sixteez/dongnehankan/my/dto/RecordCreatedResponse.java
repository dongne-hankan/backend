package com.sixteez.dongnehankan.my.dto;

public record RecordCreatedResponse(RecordResponse record, NeighborhoodProgress neighborhood) {

	public record NeighborhoodProgress(int filledCellCount, int totalCellCount, boolean newlyFilled,
			int visitedRegionCount, boolean newRegion) {
	}
}
