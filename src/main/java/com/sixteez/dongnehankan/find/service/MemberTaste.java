package com.sixteez.dongnehankan.find.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.my.entity.Feeling;
import com.sixteez.dongnehankan.my.entity.VisitRecord;

/**
 * 회원의 기록으로 만든 추천 점수다. 규칙은 기능명세서 FIND-02를 따른다. 코스마다 가장 최근 기록만 쓴다.
 */
final class MemberTaste {

	private static final MemberTaste GUEST = new MemberTaste(Map.of(), Map.of(), Set.of());

	private static final int SAVED_BONUS = 1;

	private static final int VISITED_PENALTY = 3;

	private final Map<Long, Feeling> latestFeelings;

	private final Map<Long, Integer> tagScores;

	private final Set<Long> savedCourseIds;

	private MemberTaste(Map<Long, Feeling> latestFeelings, Map<Long, Integer> tagScores, Set<Long> savedCourseIds) {
		this.latestFeelings = latestFeelings;
		this.tagScores = tagScores;
		this.savedCourseIds = savedCourseIds;
	}

	static MemberTaste guest() {
		return GUEST;
	}

	/** recordsLatestFirst는 최근 기록이 앞에 오도록 정렬되어 있어야 한다. */
	static MemberTaste of(List<VisitRecord> recordsLatestFirst, Set<Long> savedCourseIds) {
		Map<Long, VisitRecord> latest = new LinkedHashMap<>();
		recordsLatestFirst.forEach(record -> latest.putIfAbsent(record.getCourse().getId(), record));

		Map<Long, Feeling> latestFeelings = new HashMap<>();
		Map<Long, Integer> tagScores = new HashMap<>();
		latest.values().forEach(record -> {
			latestFeelings.put(record.getCourse().getId(), record.getFeeling());
			record.getCourse()
				.getTags()
				.forEach(tag -> tagScores.merge(tag.getId(), record.getFeeling().getScore(), Integer::sum));
		});
		return new MemberTaste(latestFeelings, tagScores, savedCourseIds);
	}

	boolean rejected(Course course) {
		return latestFeelings.get(course.getId()) == Feeling.NOT_FOR_ME;
	}

	int score(Course course) {
		int score = course.getTags().stream().mapToInt(tag -> tagScores.getOrDefault(tag.getId(), 0)).sum();
		if (savedCourseIds.contains(course.getId())) {
			score += SAVED_BONUS;
		}
		Feeling feeling = latestFeelings.get(course.getId());
		if (feeling != null && feeling != Feeling.WANT_AGAIN) {
			score -= VISITED_PENALTY;
		}
		return score;
	}

	Optional<String> favoriteTagName(Collection<Course> courses) {
		return courses.stream()
			.flatMap(course -> course.getTags().stream())
			.filter(tag -> tagScores.getOrDefault(tag.getId(), 0) > 0)
			.max(Comparator.comparingInt(tag -> tagScores.get(tag.getId())))
			.map(ExperienceTag::getName);
	}
}
