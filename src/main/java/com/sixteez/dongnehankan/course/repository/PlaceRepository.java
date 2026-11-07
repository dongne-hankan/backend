package com.sixteez.dongnehankan.course.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sixteez.dongnehankan.course.entity.Place;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	Optional<Place> findByIdAndVisibleTrue(Long id);

	List<Place> findByVisibleTrueAndAddressContaining(String regionName);
}
