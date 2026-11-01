package com.sixteez.dongnehankan.course.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.course.dto.CourseDetailResponse;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.Me;
import com.sixteez.dongnehankan.course.dto.CourseDetailResponse.RecordItem;
import com.sixteez.dongnehankan.course.dto.PlaceDetailResponse;
import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.entity.Place;
import com.sixteez.dongnehankan.course.repository.CourseRepository;
import com.sixteez.dongnehankan.course.repository.PlaceRepository;
import com.sixteez.dongnehankan.course.repository.ResidentStoryRepository;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.my.repository.SavedCourseRepository;
import com.sixteez.dongnehankan.my.repository.VisitRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

	private final CourseRepository courseRepository;

	private final PlaceRepository placeRepository;

	private final ResidentStoryRepository residentStoryRepository;

	private final SavedCourseRepository savedCourseRepository;

	private final VisitRecordRepository visitRecordRepository;

	public CourseDetailResponse getCourse(Long courseId, Long userId) {
		Course course = courseRepository.findVisibleById(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
		return CourseDetailResponse.of(course, userId == null ? null : me(courseId, userId));
	}

	private Me me(Long courseId, Long userId) {
		boolean saved = savedCourseRepository.findByUserIdAndCourseId(userId, courseId).isPresent();
		return new Me(saved, visitRecordRepository
			.findByUserIdAndCourseIdOrderByVisitedOnDescCreatedAtDescIdDesc(userId, courseId)
			.stream()
			.map(RecordItem::from)
			.toList());
	}

	public PlaceDetailResponse getPlace(Long placeId) {
		Place place = placeRepository.findByIdAndVisibleTrue(placeId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));
		return PlaceDetailResponse.of(place, residentStoryRepository.findByPlaceIdAndVisibleTrueOrderById(placeId));
	}
}
