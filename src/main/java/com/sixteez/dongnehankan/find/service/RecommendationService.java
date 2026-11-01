package com.sixteez.dongnehankan.find.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;
import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.repository.CourseRepository;
import com.sixteez.dongnehankan.course.repository.ExperienceTagRepository;
import com.sixteez.dongnehankan.course.repository.RegionRepository;
import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.dto.FindOptionsResponse;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.Condition;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.Fallback;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.FallbackType;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.Personalization;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.my.repository.SavedCourseRepository;
import com.sixteez.dongnehankan.my.repository.VisitRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

	private static final int PAGE_SIZE = 3;

	private final CourseRepository courseRepository;

	private final RegionRepository regionRepository;

	private final ExperienceTagRepository experienceTagRepository;

	private final VisitRecordRepository visitRecordRepository;

	private final SavedCourseRepository savedCourseRepository;

	public FindOptionsResponse getOptions() {
		return FindOptionsResponse.of(regionRepository.findAllWithVisibleCourse(),
				experienceTagRepository.findAllByOrderBySortOrder());
	}

	public RecommendationResponse recommend(Long regionId, Long tagId, DurationOption duration,
			List<Long> excludeCourseIds, Long userId) {
		if (regionId == null || tagId == null || duration == null) {
			throw new BusinessException(ErrorCode.CONDITION_REQUIRED);
		}
		if (!regionRepository.existsById(regionId)) {
			throw new BusinessException(ErrorCode.REGION_NOT_FOUND);
		}
		if (!experienceTagRepository.existsById(tagId)) {
			throw new BusinessException(ErrorCode.TAG_NOT_FOUND);
		}

		Condition condition = new Condition(regionId, tagId, duration);
		MemberTaste taste = tasteOf(userId);
		List<Course> visible = courseRepository.findAllVisible()
			.stream()
			.filter(course -> !taste.rejected(course))
			.toList();
		List<Course> matched = filter(visible, regionId, tagId, duration);
		if (!matched.isEmpty()) {
			return response(condition, matched, duration, excludeCourseIds, Fallback.none(), taste);
		}
		return fallback(condition, visible, excludeCourseIds, taste);
	}

	private MemberTaste tasteOf(Long userId) {
		if (userId == null) {
			return MemberTaste.guest();
		}
		return MemberTaste.of(visitRecordRepository.findAllWithCourse(userId),
				savedCourseRepository.findCourseIdsByUserId(userId));
	}

	private RecommendationResponse fallback(Condition condition, List<Course> visible, List<Long> excludeCourseIds,
			MemberTaste taste) {
		Long regionId = condition.regionId();
		DurationOption duration = condition.duration();

		Optional<DurationOption> longer = duration.longer();
		if (longer.isPresent()) {
			List<Course> candidates = filter(visible, regionId, condition.tagId(), longer.get());
			if (!candidates.isEmpty()) {
				Fallback fallback = new Fallback(FallbackType.LONGER_DURATION,
						longer.get().getLabel() + "이면 갈 수 있는 코스가 있어요",
						new Condition(regionId, condition.tagId(), longer.get()));
				return response(condition, candidates, longer.get(), excludeCourseIds, fallback, taste);
			}
		}

		List<Course> otherTag = visible.stream()
			.filter(course -> course.getRegion().getId().equals(regionId))
			.filter(fitsIn(duration))
			.toList();
		if (!otherTag.isEmpty()) {
			Long appliedTagId = otherTag.getFirst().getTags().iterator().next().getId();
			Fallback fallback = new Fallback(FallbackType.OTHER_TAG, "이 동네의 다른 경험은 어떠세요?",
					new Condition(regionId, appliedTagId, duration));
			return response(condition, otherTag, duration, excludeCourseIds, fallback, taste);
		}

		return new RecommendationResponse(condition, List.of(), false,
				new Fallback(FallbackType.EMPTY, "조건을 바꿔 볼까요?", null), Personalization.none());
	}

	private List<Course> filter(List<Course> courses, Long regionId, Long tagId, DurationOption duration) {
		return courses.stream()
			.filter(course -> course.getRegion().getId().equals(regionId))
			.filter(course -> course.hasTag(tagId))
			.filter(fitsIn(duration))
			.toList();
	}

	private Predicate<Course> fitsIn(DurationOption duration) {
		return course -> course.getDurationMinutes() <= duration.getMinutes();
	}

	private RecommendationResponse response(Condition condition, List<Course> candidates, DurationOption duration,
			List<Long> excludeCourseIds, Fallback fallback, MemberTaste taste) {
		List<Course> remaining = new ArrayList<>(
				candidates.stream().filter(course -> !excludeCourseIds.contains(course.getId())).toList());
		Collections.shuffle(remaining);
		remaining.sort(Comparator.<Course>comparingInt(course -> -taste.score(course))
			.thenComparingInt(course -> duration.getMinutes() - course.getDurationMinutes()));

		List<Course> page = remaining.stream().limit(PAGE_SIZE).toList();
		return new RecommendationResponse(condition, page.stream().map(CourseSummaryResponse::from).toList(),
				remaining.size() > PAGE_SIZE, fallback, personalization(taste, page));
	}

	private Personalization personalization(MemberTaste taste, List<Course> courses) {
		return taste.favoriteTagName(courses)
			.map(name -> new Personalization(true, "'" + name + "'를 좋아하셔서 먼저 보여드려요"))
			.orElseGet(Personalization::none);
	}
}
