package com.sixteez.dongnehankan.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sixteez.dongnehankan.course.entity.Region;

public interface RegionRepository extends JpaRepository<Region, Long> {

	@Query("select distinct r from Course c join c.region r where c.visible = true order by r.id")
	List<Region> findAllWithVisibleCourse();
}
