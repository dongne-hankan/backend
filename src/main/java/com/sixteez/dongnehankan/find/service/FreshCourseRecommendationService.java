package com.sixteez.dongnehankan.find.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.PlaceItem;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.RegionItem;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.StopItem;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.TagItem;
import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Place;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.repository.ExperienceTagRepository;
import com.sixteez.dongnehankan.course.repository.PlaceRepository;
import com.sixteez.dongnehankan.course.repository.RegionRepository;
import com.sixteez.dongnehankan.course.service.GeneratedCourseDrafts;
import com.sixteez.dongnehankan.find.dto.DurationOption;
import com.sixteez.dongnehankan.find.dto.FindOptionsResponse;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.Condition;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.Fallback;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.FallbackType;
import com.sixteez.dongnehankan.find.dto.RecommendationResponse.Personalization;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.KakaoPlace;
import com.sixteez.dongnehankan.find.service.NearbyCourseBuilder.PlannedStop;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FreshCourseRecommendationService {

	private static final int MAX_CANDIDATES = 15;

	private final RegionRepository regionRepository;
	private final ExperienceTagRepository tagRepository;
	private final PlaceRepository placeRepository;
	private final NearbyAiPlanner planner;
	private final NearbyCourseBuilder courseBuilder;
	private final GeneratedCourseDrafts drafts;
	private final Environment environment;

	public FindOptionsResponse getOptions() {
		return FindOptionsResponse.of(regionRepository.findAll(Sort.by("id")),
				tagRepository.findAllByOrderBySortOrder());
	}

	public RecommendationResponse recommend(Long regionId, Long tagId, DurationOption duration,
			List<Long> excludeCourseIds) {
		if (regionId == null || tagId == null || duration == null) {
			throw new BusinessException(ErrorCode.CONDITION_REQUIRED);
		}
		Region region = regionRepository.findById(regionId)
			.orElseThrow(() -> new BusinessException(ErrorCode.REGION_NOT_FOUND));
		ExperienceTag tag = tagRepository.findById(tagId)
			.orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
		Condition condition = new Condition(regionId, tagId, duration);
		Map<String, Place> places = placeRepository.findByVisibleTrueAndAddressContaining(region.getName()).stream()
			.collect(Collectors.toMap(place -> place.getId().toString(), Function.identity()));
		List<KakaoPlace> candidates = places.values().stream()
			.map(place -> plannerCandidate(region, place))
			.sorted(Comparator.comparingInt(KakaoPlace::distanceMeters).thenComparing(KakaoPlace::id))
			.limit(MAX_CANDIDATES)
			.toList();
		if (candidates.isEmpty()) {
			return unavailable(condition, "이 지역에 추천할 장소가 아직 없어요. 다른 지역을 골라 주세요.");
		}
		List<List<String>> seenRoutes = excludeCourseIds.stream()
			.filter(id -> id < 0)
			.map(drafts::find)
			.flatMap(Optional::stream)
			.filter(draft -> draft.region().regionId().equals(regionId) && draft.tag().tagId().equals(tagId))
			.map(draft -> draft.stops().stream().map(stop -> stop.place().placeId().toString()).toList())
			.distinct()
			.limit(12)
			.toList();
		return planner.planForExperience(candidates, duration, tag.getPhrase(), seenRoutes)
			.map(course -> {
				String label = environment.acceptsProfiles(Profiles.of("local", "test"))
						? "AI 추천 (로컬 장소 예시) · " : "AI 추천 · ";
				var draft = drafts.create(course.name(), label + course.reason(),
						new RegionItem(region.getId(), region.getName(), region.getProvince()),
						new TagItem(tag.getId(), tag.getName(), tag.getPhrase(), tag.getIcon()),
						courseBuilder.totalMinutes(course.stops()), stops(course.stops(), places));
				return new RecommendationResponse(condition, List.of(draft.summary()), candidates.size() > 1, Fallback.none(),
						Personalization.none());
			})
			.orElseGet(() -> unavailable(condition, "지금은 AI 추천을 만들지 못했어요. 잠시 후 다시 검색해 주세요."));
	}

	private KakaoPlace plannerCandidate(Region region, Place place) {
		double lat = place.getLat().doubleValue();
		double lng = place.getLng().doubleValue();
		int distance = 0;
		if (region.getCenterLat() != null && region.getCenterLng() != null) {
			distance = (int) Math.round(NearbyCourseBuilder.distanceMeters(region.getCenterLat().doubleValue(),
					region.getCenterLng().doubleValue(), lat, lng));
		}
		return new KakaoPlace(place.getId().toString(), place.getName(), place.getAddress(), lat, lng,
				place.getSourceUrl(), distance);
	}

	private List<StopItem> stops(List<PlannedStop> planned, Map<String, Place> places) {
		List<StopItem> items = new ArrayList<>();
		for (int i = 0; i < planned.size(); i++) {
			PlannedStop stop = planned.get(i);
			Place place = places.get(stop.place().id());
			Integer move = i + 1 < planned.size()
					? courseBuilder.walkMinutes(stop.place(), planned.get(i + 1).place()) : null;
			items.add(new StopItem(i + 1, new PlaceItem(place.getId(), place.getName(), place.getAddress(),
					place.getLat(), place.getLng(), null, place.isTimeSensitive(), place.getSourceUrl()),
					stop.stayMinutes(), move, null, null));
		}
		return items;
	}

	private RecommendationResponse unavailable(Condition condition, String message) {
		return new RecommendationResponse(condition, List.of(), false,
				new Fallback(FallbackType.EMPTY, message, null), Personalization.none());
	}
}
