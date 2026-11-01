package com.sixteez.dongnehankan.course.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sixteez.dongnehankan.course.entity.ResidentStory;

public interface ResidentStoryRepository extends JpaRepository<ResidentStory, Long> {

	List<ResidentStory> findByPlaceIdAndVisibleTrueOrderById(Long placeId);
}
