package com.sixteez.dongnehankan.course.entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "course_id")
	private Long id;

	private String courseKey;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "region_id")
	private Region region;

	private String name;

	private String intro;

	private String transport;

	private String thumbnailUrl;

	private int durationMinutes;

	private boolean visible;

	private String source = SOURCE_CURATED;

	@ManyToMany
	@JoinTable(name = "course_tag", joinColumns = @JoinColumn(name = "course_id"),
			inverseJoinColumns = @JoinColumn(name = "tag_id"))
	@OrderBy("sortOrder")
	private Set<ExperienceTag> tags = new LinkedHashSet<>();

	@OneToMany(mappedBy = "course", cascade = CascadeType.PERSIST)
	@OrderBy("stopOrder")
	private List<CourseStop> stops = new ArrayList<>();

	public static final String SOURCE_CURATED = "CURATED";

	public static final String SOURCE_WALK = "WALK";

	public record WalkStopPlan(String kakaoPlaceId, String kakaoPlaceUrl, int stayMinutes, Integer moveMinutesToNext) {
	}

	public record StopPlan(Place place, int stayMinutes, Integer moveMinutesToNext) {
	}

	public static Course generated(String key, Region region, ExperienceTag tag, String name, String intro,
			int durationMinutes, List<StopPlan> stops) {
		Course course = new Course();
		course.courseKey = key;
		course.region = region;
		course.name = name;
		course.intro = intro;
		course.transport = "도보";
		course.durationMinutes = durationMinutes;
		course.visible = true;
		course.tags.add(tag);
		for (StopPlan stop : stops) {
			course.stops.add(new CourseStop(course, stop.place(), course.stops.size() + 1,
					stop.stayMinutes(), stop.moveMinutesToNext()));
		}
		return course;
	}

	/**
	 * 회원이 다녀온 지도 추천 코스다. 카카오 장소는 약관상 ID와 링크만 남기고, 추천·상세에는 나오지 않는다.
	 */
	public static Course walk(String key, Region region, ExperienceTag tag, String name, String intro,
			int durationMinutes, List<WalkStopPlan> stops) {
		Course course = new Course();
		course.courseKey = key;
		course.region = region;
		course.name = name;
		course.intro = intro;
		course.transport = "도보";
		course.durationMinutes = durationMinutes;
		course.visible = false;
		course.source = SOURCE_WALK;
		course.tags.add(tag);
		for (WalkStopPlan stop : stops) {
			course.stops.add(CourseStop.kakao(course, stop.kakaoPlaceId(), stop.kakaoPlaceUrl(),
					course.stops.size() + 1, stop.stayMinutes(), stop.moveMinutesToNext()));
		}
		return course;
	}

	public boolean hasTag(Long tagId) {
		return tags.stream().anyMatch(tag -> tag.getId().equals(tagId));
	}
}
