package com.sixteez.dongnehankan.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.sixteez.dongnehankan.course.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

	@Query("select distinct c from Course c join fetch c.region where c.visible = true and c.courseKey not like 'ai-draft-%'")
	List<Course> findAllVisible();

	@Query("select c from Course c join fetch c.region where c.id = :id and c.visible = true")
	Optional<Course> findVisibleById(Long id);

	Optional<Course> findByCourseKeyAndVisibleTrue(String courseKey);

	boolean existsByCourseKey(String courseKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Course c where c.courseKey = :key")
	Optional<Course> findByKeyForUpdate(String key);
}
