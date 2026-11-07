package com.sixteez.dongnehankan.my.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;
import com.sixteez.dongnehankan.course.entity.Course;
import com.sixteez.dongnehankan.course.repository.CourseRepository;
import com.sixteez.dongnehankan.course.service.GeneratedCourseDrafts;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.global.response.PageResponse;
import com.sixteez.dongnehankan.my.dto.CreateRecordRequest;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse.Grid;
import com.sixteez.dongnehankan.my.dto.RecordCreatedResponse;
import com.sixteez.dongnehankan.my.dto.RecordCreatedResponse.NeighborhoodProgress;
import com.sixteez.dongnehankan.my.dto.RecordResponse;
import com.sixteez.dongnehankan.my.dto.SaveCourseResponse;
import com.sixteez.dongnehankan.my.dto.SavedCourseItem;
import com.sixteez.dongnehankan.my.dto.UpdateRecordRequest;
import com.sixteez.dongnehankan.my.entity.SavedCourse;
import com.sixteez.dongnehankan.my.entity.VisitRecord;
import com.sixteez.dongnehankan.my.repository.SavedCourseRepository;
import com.sixteez.dongnehankan.my.repository.VisitRecordRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	public record SaveResult(boolean created, SaveCourseResponse response) {
	}

	private final CourseRepository courseRepository;
	private final GeneratedCourseDrafts generatedCourseDrafts;

	private final SavedCourseRepository savedCourseRepository;

	private final VisitRecordRepository visitRecordRepository;

	private final NeighborhoodCalculator neighborhoodCalculator;

	@Transactional
	public SaveResult saveCourse(Long userId, Long courseId) {
		Course course = findCourse(courseId);
		var existing = savedCourseRepository.findByUserIdAndCourseId(userId, course.getId());
		if (existing.isPresent()) {
			return new SaveResult(false, saveResponse(existing.get()));
		}
		if (!course.isVisible()) {
			throw new BusinessException(ErrorCode.COURSE_HIDDEN);
		}
		return new SaveResult(true, saveResponse(savedCourseRepository.save(new SavedCourse(userId, course))));
	}

	@Transactional
	public void unsaveCourse(Long userId, Long courseId) {
		if (courseId > 0) {
			savedCourseRepository.deleteByUserIdAndCourseId(userId, courseId);
		}
		else {
			generatedCourseDrafts.findVisibleCourse(courseId)
				.ifPresent(course -> savedCourseRepository.deleteByUserIdAndCourseId(userId, course.getId()));
		}
	}

	@Transactional(readOnly = true)
	public PageResponse<SavedCourseItem> getSavedCourses(Long userId, Pageable pageable) {
		Page<SavedCourse> saved = savedCourseRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, unsorted(pageable));
		List<Long> courseIds = saved.map(item -> item.getCourse().getId()).getContent();
		Set<Long> visited = Set.copyOf(visitRecordRepository.findVisitedCourseIds(userId, courseIds));
		return PageResponse.of(saved.map(item -> new SavedCourseItem(toOffset(item.getCreatedAt()),
				visited.contains(item.getCourse().getId()), CourseSummaryResponse.withVisibility(item.getCourse()))));
	}

	@Transactional
	public RecordCreatedResponse createRecord(Long userId, CreateRecordRequest request) {
		if (request.feeling() == null) {
			throw new BusinessException(ErrorCode.FEELING_REQUIRED);
		}
		Course course = findCourse(request.courseId());
		validateVisitedOn(request.visitedOn());
		int filledBefore = neighborhoodCalculator.grid(visitRecordRepository.findAllWithCourse(userId))
			.filledCellCount();

		LocalDate visitedOn = request.visitedOn() == null ? LocalDate.now(SEOUL) : request.visitedOn();
		String memo = request.memo() == null || request.memo().isBlank() ? null : request.memo();
		VisitRecord record = visitRecordRepository
			.saveAndFlush(new VisitRecord(userId, course, request.feeling(), memo, visitedOn));

		Grid grid = neighborhoodCalculator.grid(visitRecordRepository.findAllWithCourse(userId));
		return new RecordCreatedResponse(RecordResponse.from(record), new NeighborhoodProgress(grid.filledCellCount(),
				grid.totalCellCount(), grid.filledCellCount() > filledBefore));
	}

	@Transactional(readOnly = true)
	public PageResponse<RecordResponse> getRecords(Long userId, Pageable pageable) {
		return PageResponse.of(visitRecordRepository
			.findByUserIdOrderByVisitedOnDescCreatedAtDescIdDesc(userId, unsorted(pageable))
			.map(RecordResponse::from));
	}

	@Transactional
	public RecordResponse updateRecord(Long userId, Long recordId, UpdateRecordRequest request) {
		validateVisitedOn(request.visitedOn());
		VisitRecord record = findOwnRecord(userId, recordId);
		record.update(request.feeling(), request.memo(), request.visitedOn());
		return RecordResponse.from(record);
	}

	@Transactional
	public void deleteRecord(Long userId, Long recordId) {
		visitRecordRepository.delete(findOwnRecord(userId, recordId));
	}

	@Transactional(readOnly = true)
	public NeighborhoodResponse getNeighborhood(Long userId) {
		return neighborhoodCalculator.response(visitRecordRepository.findAllWithCourse(userId),
				savedCourseRepository.countByUserId(userId));
	}

	private Course findCourse(Long courseId) {
		if (courseId < 0) {
			return generatedCourseDrafts.persist(courseId);
		}
		return courseRepository.findById(courseId)
			.orElseThrow(() -> new BusinessException(ErrorCode.COURSE_NOT_FOUND));
	}

	private VisitRecord findOwnRecord(Long userId, Long recordId) {
		VisitRecord record = visitRecordRepository.findById(recordId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RECORD_NOT_FOUND));
		if (!record.getUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.RECORD_FORBIDDEN);
		}
		return record;
	}

	private void validateVisitedOn(LocalDate visitedOn) {
		if (visitedOn != null && visitedOn.isAfter(LocalDate.now(SEOUL))) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
	}

	private SaveCourseResponse saveResponse(SavedCourse saved) {
		return new SaveCourseResponse(saved.getCourse().getId(), true, toOffset(saved.getCreatedAt()));
	}

	private OffsetDateTime toOffset(LocalDateTime dateTime) {
		return dateTime.atZone(SEOUL).toOffsetDateTime();
	}

	private Pageable unsorted(Pageable pageable) {
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
	}
}
