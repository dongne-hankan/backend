package com.sixteez.dongnehankan.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sixteez.dongnehankan.course.entity.ExperienceTag;

public interface ExperienceTagRepository extends JpaRepository<ExperienceTag, Long> {

	List<ExperienceTag> findAllByOrderBySortOrder();
}
