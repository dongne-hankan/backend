package com.sixteez.dongnehankan.my.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.entity.Course.WalkStopPlan;
import com.sixteez.dongnehankan.course.entity.ExperienceTag;
import com.sixteez.dongnehankan.course.entity.Province;
import com.sixteez.dongnehankan.course.entity.Region;
import com.sixteez.dongnehankan.course.repository.CourseRepository;
import com.sixteez.dongnehankan.course.repository.ExperienceTagRepository;
import com.sixteez.dongnehankan.course.repository.RegionRepository;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient;
import com.sixteez.dongnehankan.find.service.KakaoLocalClient.AdministrativeArea;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.my.dto.CreateRecordRequest;
import com.sixteez.dongnehankan.my.dto.RecordCreatedResponse;
import com.sixteez.dongnehankan.my.dto.WalkRecordRequest;
import com.sixteez.dongnehankan.my.dto.WalkRecordRequest.WalkCourse;

import lombok.RequiredArgsConstructor;

/**
 * 지도에서 추천받아 다녀온 코스를 저장하고 느낌 기록을 남긴다. 코스는 기록과 내 한 칸을 위해서만 저장한다.
 */
@Service
@RequiredArgsConstructor
public class WalkService {

	private final KakaoLocalClient kakaoLocalClient;

	private final RegionRepository regionRepository;

	private final ExperienceTagRepository experienceTagRepository;

	private final CourseRepository courseRepository;

	private final MyService myService;

	@Transactional
	public RecordCreatedResponse recordWalk(Long userId, WalkRecordRequest request) {
		if (request.feeling() == null) {
			throw new BusinessException(ErrorCode.FEELING_REQUIRED);
		}
		WalkCourse walk = request.course();
		ExperienceTag tag = experienceTagRepository.findById(walk.tagId())
			.orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND));
		Region region = regionOf(walk.center().lat(), walk.center().lng());

		Course course = courseRepository.saveAndFlush(Course.walk("walk-" + UUID.randomUUID(), region, tag,
				walk.name(), walk.intro() == null ? "" : walk.intro(), walk.durationMinutes(),
				walk.stops()
					.stream()
					.map(stop -> new WalkStopPlan(stop.kakaoPlaceId(), stop.kakaoPlaceUrl(), stop.stayMinutes(),
							stop.moveMinutesToNext()))
					.toList()));
		return myService.createRecord(userId,
				new CreateRecordRequest(course.getId(), request.feeling(), request.memo(), request.visitedOn()));
	}

	// 시범 지역 밖의 대구·경북 구·군이면 지역을 새로 만든다. 행정구역 이름은 카카오 장소 정보가 아니다.
	private Region regionOf(double lat, double lng) {
		AdministrativeArea area = kakaoLocalClient.findArea(lat, lng)
			.orElseThrow(() -> new BusinessException(ErrorCode.OUT_OF_SERVICE_AREA));
		Province province = provinceOf(area.province());
		return regionRepository.findByProvinceAndName(province, area.district())
			.orElseGet(() -> regionRepository.save(Region.discovered(province, area.district(),
					BigDecimal.valueOf(lat), BigDecimal.valueOf(lng))));
	}

	private Province provinceOf(String name) {
		if (name.startsWith("대구")) {
			return Province.DAEGU;
		}
		if (name.startsWith("경상북도") || name.startsWith("경북")) {
			return Province.GYEONGBUK;
		}
		throw new BusinessException(ErrorCode.OUT_OF_SERVICE_AREA);
	}
}
