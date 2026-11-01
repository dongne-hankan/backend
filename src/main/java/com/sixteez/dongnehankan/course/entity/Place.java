package com.sixteez.dongnehankan.course.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "place_id")
	private Long id;

	private String placeKey;

	private String name;

	private String address;

	private BigDecimal lat;

	private BigDecimal lng;

	private LocalDate checkedOn;

	private boolean timeSensitive;

	private String sourceUrl;

	private boolean visible;

	@OneToMany(mappedBy = "place")
	@OrderBy("sortOrder")
	private List<PlacePhoto> photos = new ArrayList<>();

	public String thumbnailUrl() {
		return photos.isEmpty() ? null : photos.getFirst().getImageUrl();
	}
}
