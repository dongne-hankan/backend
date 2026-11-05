package com.sixteez.dongnehankan.find.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;

@Component
public class NearbyCourseBuilder {

	public static final int MAX_STOPS = 4;

	private static final double EARTH_RADIUS_METERS = 6_371_000;

	// 직선거리를 실제 걷는 길이로 보정
	private static final double DETOUR_FACTOR = 1.3;

	// 시속 4km
	private static final double WALK_METERS_PER_MINUTE = 67;

	public record PlannedStop(KakaoPlace place, int stayMinutes, String reason) {
	}

	public int stayMinutes(DurationOption duration) {
		return duration == DurationOption.MIN_30 ? 10 : 15;
	}

	public int walkMinutes(KakaoPlace from, KakaoPlace to) {
		double meters = distanceMeters(from.lat(), from.lng(), to.lat(), to.lng()) * DETOUR_FACTOR;
		return Math.max(1, (int) Math.ceil(meters / WALK_METERS_PER_MINUTE));
	}

	// 첫 장소까지 가는 시간은 넣지 않는다.
	public int totalMinutes(List<PlannedStop> stops) {
		int total = 0;
		for (int i = 0; i < stops.size(); i++) {
			total += stops.get(i).stayMinutes();
			if (i > 0) {
				total += walkMinutes(stops.get(i - 1).place(), stops.get(i).place());
			}
		}
		return total;
	}

	public List<PlannedStop> basicStops(List<KakaoPlace> places, DurationOption duration) {
		if (places.isEmpty()) {
			return List.of();
		}
		int stay = stayMinutes(duration);
		List<KakaoPlace> remaining = new ArrayList<>(places);
		KakaoPlace current = remaining.remove(0);
		List<PlannedStop> stops = new ArrayList<>(List.of(basicStop(current, stay)));
		int total = stay;
		while (stops.size() < MAX_STOPS && !remaining.isEmpty()) {
			KakaoPlace from = current;
			KakaoPlace next = remaining.stream().min(Comparator.comparingInt(place -> walkMinutes(from, place))).get();
			int nextTotal = total + walkMinutes(current, next) + stay;
			if (nextTotal > duration.getMinutes()) {
				break;
			}
			remaining.remove(next);
			stops.add(basicStop(next, stay));
			total = nextTotal;
			current = next;
		}
		return stops;
	}

	private PlannedStop basicStop(KakaoPlace place, int stay) {
		return new PlannedStop(place, stay, "지금 위치에서 약 " + place.distanceMeters() + "m 떨어진 곳이에요.");
	}

	private static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}
}
