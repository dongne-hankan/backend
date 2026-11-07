package com.sixteez.dongnehankan.course.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "region_id")
	private Long id;

	private String regionKey;

	private String name;

	@Enumerated(EnumType.STRING)
	private Province province;

	private BigDecimal centerLat;

	private BigDecimal centerLng;

	public static Region discovered(Province province, String name, BigDecimal centerLat, BigDecimal centerLng) {
		Region region = new Region();
		region.regionKey = province.name().toLowerCase() + "-" + name;
		region.name = name;
		region.province = province;
		region.centerLat = centerLat;
		region.centerLng = centerLng;
		return region;
	}
}
