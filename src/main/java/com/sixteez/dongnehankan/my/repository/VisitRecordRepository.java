package com.sixteez.dongnehankan.my.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sixteez.dongnehankan.my.entity.VisitRecord;

public interface VisitRecordRepository extends JpaRepository<VisitRecord, Long> {

	Page<VisitRecord> findByUserIdOrderByVisitedOnDescCreatedAtDescIdDesc(Long userId, Pageable pageable);

	@Query("""
			select r from VisitRecord r join fetch r.course c join fetch c.region
			where r.userId = :userId
			order by r.visitedOn desc, r.createdAt desc, r.id desc
			""")
	List<VisitRecord> findAllWithCourse(Long userId);

	List<VisitRecord> findByUserIdAndCourseIdOrderByVisitedOnDescCreatedAtDescIdDesc(Long userId, Long courseId);

	@Query("select distinct r.course.id from VisitRecord r where r.userId = :userId and r.course.id in :courseIds")
	List<Long> findVisitedCourseIds(Long userId, Collection<Long> courseIds);
}
