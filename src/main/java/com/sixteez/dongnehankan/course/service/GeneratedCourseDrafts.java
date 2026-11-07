package com.sixteez.dongnehankan.course.service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.course.dto.CourseDetailResponse;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.Me;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.RegionItem;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.StopItem;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.TagItem;
import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;
import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.entity.Course.StopPlan;
import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Place;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.repository.CourseRepository;
import com.sixteez.dongnehankan.course.repository.ExperienceTagRepository;
import com.sixteez.dongnehankan.course.repository.PlaceRepository;
import com.sixteez.dongnehankan.course.repository.RegionRepository;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeneratedCourseDrafts {

	private static final int MAX_DRAFTS = 512;
	private static final Duration DRAFT_TTL = Duration.ofMinutes(30);
	// JavaScript에서도 손실 없이 전달하고 DB에서 발급하는 양수 ID와 구분한다.
	private static final long MIN_DRAFT_ID = -(1L << 52);

	private final LinkedHashMap<Long, Draft> drafts = new LinkedHashMap<>();
	private final CourseRepository courseRepository;
	private final PlaceRepository placeRepository;
	private final RegionRepository regionRepository;
	private final ExperienceTagRepository tagRepository;

	public record Draft(long id, Instant expiresAt, String name, String intro, RegionItem region, TagItem tag,
			int durationMinutes, List<StopItem> stops) {

		public CourseSummaryResponse summary() {
			return new CourseSummaryResponse(id, name, intro, null,
					new CourseSummaryResponse.RegionItem(region.regionId(), region.name()),
					List.of(new CourseSummaryResponse.TagItem(tag.tagId(), tag.name())),
					durationMinutes, stops.size(), null, null);
		}

		public CourseDetailResponse detail(Me me) {
			return new CourseDetailResponse(id, name, intro, null, region, List.of(tag),
					durationMinutes, "도보", stops, me);
		}
	}

	public synchronized Draft create(String name, String intro, RegionItem region, TagItem tag, int durationMinutes,
			List<StopItem> stops) {
		Instant now = Instant.now();
		drafts.values().removeIf(draft -> !draft.expiresAt().isAfter(now));
		long id;
		do {
			id = ThreadLocalRandom.current().nextLong(MIN_DRAFT_ID, 0);
		}
		while (drafts.containsKey(id) || courseRepository.existsByCourseKey(key(id)));
		Draft draft = new Draft(id, now.plus(DRAFT_TTL), name, intro, region, tag, durationMinutes, List.copyOf(stops));
		drafts.put(id, draft);
		if (drafts.size() > MAX_DRAFTS) {
			drafts.pollFirstEntry();
		}
		return draft;
	}

	public synchronized Optional<Draft> find(long id) {
		Draft draft = drafts.get(id);
		if (draft != null && !draft.expiresAt().isAfter(Instant.now())) {
			drafts.remove(id);
			return Optional.empty();
		}
		return Optional.ofNullable(draft);
	}

	public Optional<Course> findVisibleCourse(long id) {
		return id < 0 ? courseRepository.findByCourseKeyAndVisibleTrue(key(id)) : courseRepository.findVisibleById(id);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public Course persist(long id) {
		Draft draft = find(id).orElse(null);
		if (draft == null) {
			return courseRepository.findByKeyForUpdate(key(id))
				.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
		}
		// 동일한 임시 코스의 동시 담기 요청을 트랜잭션이 끝날 때까지 직렬화한다.
		Region region = regionRepository.findByIdForUpdate(draft.region().regionId())
			.orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
		return courseRepository.findByKeyForUpdate(key(id)).orElseGet(() -> insert(draft, region));
	}

	private Course insert(Draft draft, Region region) {
		ExperienceTag tag = tagRepository.findById(draft.tag().tagId())
			.orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
		List<StopPlan> itinerary = draft.stops().stream().map(stop -> {
			Place place = placeRepository.findByIdAndVisibleTrue(stop.place().placeId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
			return new StopPlan(place, stop.stayMinutes(), stop.moveMinutesToNext());
		}).toList();
		return courseRepository.save(Course.generated(key(draft.id()), region, tag, draft.name(), draft.intro(),
				draft.durationMinutes(), itinerary));
	}

	private static String key(long id) {
		return "ai-draft" + id;
	}
}
