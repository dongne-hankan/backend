package com.sixteez.dongnehankan.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import com.sixteez.dongnehankan.course.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

	@Query("select distinct r from Course c join c.region r where c.visible = true order by r.id")
	List<Region> findAllWithVisibleCourse();

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select r from Region r where r.id = :id")
	Optional<Region> findByIdForUpdate(Long id);
}
