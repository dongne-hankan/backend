package com.sixteez.dongnehankan.my.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sixteez.dongnehankan.global.response.ApiResponse;
import com.sixteez.dongnehankan.global.response.PageResponse;
import com.sixteez.dongnehankan.my.dto.CreateRecordRequest;
import com.sixteez.dongnehankan.my.dto.NeighborhoodResponse;
import com.sixteez.dongnehankan.my.dto.RecordCreatedResponse;
import com.sixteez.dongnehankan.my.dto.RecordResponse;
import com.sixteez.dongnehankan.my.dto.SaveCourseRequest;
import com.sixteez.dongnehankan.my.dto.SaveCourseResponse;
import com.sixteez.dongnehankan.my.dto.SavedCourseItem;
import com.sixteez.dongnehankan.my.dto.UpdateRecordRequest;
import com.sixteez.dongnehankan.my.dto.WalkRecordRequest;
import com.sixteez.dongnehankan.my.service.MyService;
import com.sixteez.dongnehankan.my.service.WalkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyController {

	private final MyService myService;

	private final WalkService walkService;

	@PostMapping("/saved-courses")
	public ResponseEntity<ApiResponse<SaveCourseResponse>> saveCourse(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody SaveCourseRequest request) {
		MyService.SaveResult result = myService.saveCourse(userId, request.courseId());
		return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
			.body(ApiResponse.of(result.response()));
	}

	@GetMapping("/saved-courses")
	public ApiResponse<PageResponse<SavedCourseItem>> getSavedCourses(@AuthenticationPrincipal Long userId,
			@PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.of(myService.getSavedCourses(userId, pageable));
	}

	@DeleteMapping("/saved-courses/{courseId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsaveCourse(@AuthenticationPrincipal Long userId, @PathVariable Long courseId) {
		myService.unsaveCourse(userId, courseId);
	}

	@PostMapping("/records")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RecordCreatedResponse> createRecord(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody CreateRecordRequest request) {
		return ApiResponse.of(myService.createRecord(userId, request));
	}

	@PostMapping("/walks")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RecordCreatedResponse> recordWalk(@AuthenticationPrincipal Long userId,
			@Valid @RequestBody WalkRecordRequest request) {
		return ApiResponse.of(walkService.recordWalk(userId, request));
	}

	@GetMapping("/records")
	public ApiResponse<PageResponse<RecordResponse>> getRecords(@AuthenticationPrincipal Long userId,
			@PageableDefault(size = 20) Pageable pageable) {
		return ApiResponse.of(myService.getRecords(userId, pageable));
	}

	@PatchMapping("/records/{recordId}")
	public ApiResponse<RecordResponse> updateRecord(@AuthenticationPrincipal Long userId, @PathVariable Long recordId,
			@Valid @RequestBody UpdateRecordRequest request) {
		return ApiResponse.of(myService.updateRecord(userId, recordId, request));
	}

	@DeleteMapping("/records/{recordId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteRecord(@AuthenticationPrincipal Long userId, @PathVariable Long recordId) {
		myService.deleteRecord(userId, recordId);
	}

	@GetMapping("/neighborhood")
	public ApiResponse<NeighborhoodResponse> getNeighborhood(@AuthenticationPrincipal Long userId) {
		return ApiResponse.of(myService.getNeighborhood(userId));
	}
}
