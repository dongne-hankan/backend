package com.sixteez.dongnehankan.my.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.sixteez.dongnehankan.course.entity.Course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class VisitRecord {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "record_id")
	private Long id;

	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "course_id")
	private Course course;

	@Enumerated(EnumType.STRING)
	private Feeling feeling;

	private String memo;

	private LocalDate visitedOn;

	private String photoUrl;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	public VisitRecord(Long userId, Course course, Feeling feeling, String memo, LocalDate visitedOn) {
		this.userId = userId;
		this.course = course;
		this.feeling = feeling;
		this.memo = memo;
		this.visitedOn = visitedOn;
	}

	public void update(Feeling feeling, String memo, LocalDate visitedOn) {
		if (feeling != null) {
			this.feeling = feeling;
		}
		if (memo != null) {
			this.memo = memo.isBlank() ? null : memo;
		}
		if (visitedOn != null) {
			this.visitedOn = visitedOn;
		}
	}
}
