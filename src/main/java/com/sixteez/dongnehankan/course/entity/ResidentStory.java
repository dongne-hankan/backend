package com.sixteez.dongnehankan.course.entity;

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
public class ResidentStory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "story_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id")
	private Place place;

	private String residentIntro;

	private String residentName;

	private String residentPhotoUrl;

	@Column(columnDefinition = "TEXT")
	private String reason;

	@Column(columnDefinition = "TEXT")
	private String howToEnjoy;

	@Enumerated(EnumType.STRING)
	private ConsentScope consentScope;

	private boolean visible;

	public String publicName() {
		return consentScope == ConsentScope.INTRO_ONLY ? null : residentName;
	}

	public String publicPhotoUrl() {
		return consentScope == ConsentScope.WITH_PHOTO ? residentPhotoUrl : null;
	}
}
