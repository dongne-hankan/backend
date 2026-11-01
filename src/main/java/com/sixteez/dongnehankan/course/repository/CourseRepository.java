package com.sixteez.dongnehankan.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sixteez.dongnehankan.course.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

	@Query("select distinct c from Course c join fetch c.region where c.visible = true")
	List<Course> findAllVisible();

	@Query("select c from Course c join fetch c.region where c.id = :id and c.visible = true")
	Optional<Course> findVisibleById(Long id);
}
