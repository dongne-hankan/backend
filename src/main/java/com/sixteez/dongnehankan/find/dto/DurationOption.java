package com.sixteez.dongnehankan.find.dto;

import java.util.Optional;

public enum DurationOption {

	MIN_30("30분", 30), HOUR_1("1시간", 60);

	private final String label;
	private final int minutes;

	DurationOption(String label, int minutes) {
		this.label = label;
		this.minutes = minutes;
	}

	public String getLabel() {
		return label;
	}

	public int getMinutes() {
		return minutes;
	}

	public Optional<DurationOption> longer() {
		return ordinal() + 1 < values().length ? Optional.of(values()[ordinal() + 1]) : Optional.empty();
	}
}
