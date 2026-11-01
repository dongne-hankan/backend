package com.sixteez.dongnehankan.my.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sixteez.dongnehankan.my.entity.SavedCourse;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

	Optional<SavedCourse> findByUserIdAndCourseId(Long userId, Long courseId);

	Page<SavedCourse> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

	long countByUserId(Long userId);

	void deleteByUserIdAndCourseId(Long userId, Long courseId);

	@Query("select s.course.id from SavedCourse s where s.userId = :userId")
	Set<Long> findCourseIdsByUserId(Long userId);
}
