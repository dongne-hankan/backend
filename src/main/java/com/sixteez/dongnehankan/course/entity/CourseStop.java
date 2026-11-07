package com.sixteez.dongnehankan.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseStop {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "stop_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id")
	private Course course;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id")
	private Place place;

	private int stopOrder;

	private int stayMinutes;

	private Integer moveMinutesToNext;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "highlight_story_id")
	private ResidentStory highlightStory;

	CourseStop(Course course, Place place, int order, int stayMinutes, Integer moveMinutesToNext) {
		this.course = course;
		this.place = place;
		this.stopOrder = order;
		this.stayMinutes = stayMinutes;
		this.moveMinutesToNext = moveMinutesToNext;
	}
}
