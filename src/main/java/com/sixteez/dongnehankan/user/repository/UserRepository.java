package com.sixteez.dongnehankan.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sixteez.dongnehankan.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByKakaoId(String kakaoId);

	Optional<User> findByLoginId(String loginId);

	boolean existsByLoginId(String loginId);

	@Query(value = "select count(*) from saved_course where user_id = :userId", nativeQuery = true)
	long countSavedCourses(Long userId);

	@Query(value = "select count(*) from visit_record where user_id = :userId", nativeQuery = true)
	long countRecords(Long userId);

	@Query(value = "select count(*) from user_preference where user_id = :userId and onboarding_completed = 1",
			nativeQuery = true)
	long countCompletedOnboarding(Long userId);
}
