package com.sixteez.dongnehankan.user.service;

import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.user.dto.MyInfoResponse;
import com.sixteez.dongnehankan.user.entity.User;
import com.sixteez.dongnehankan.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public MyInfoResponse getMyInfo(Long userId) {
		return toResponse(find(userId));
	}

	@Transactional
	public MyInfoResponse changeNickname(Long userId, String nickname) {
		User user = find(userId);
		user.changeNickname(nickname);
		return toResponse(user);
	}

	// TODO: 카카오 Admin 키를 받으면 연결 끊기(unlink)도 함께 호출한다.
	@Transactional
	public void withdraw(Long userId) {
		User user = find(userId);
		if (user.isAdmin()) {
			throw new BusinessException(ErrorCode.ADMIN_CANNOT_WITHDRAW);
		}
		userRepository.delete(user);
	}

	private User find(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.AUTH_REQUIRED));
	}

	private MyInfoResponse toResponse(User user) {
		Long userId = user.getId();
		return new MyInfoResponse(userId, user.getNickname(), user.getCreatedAt().atZone(SEOUL).toOffsetDateTime(),
				userRepository.countSavedCourses(userId), userRepository.countRecords(userId), user.isAdmin(),
				userRepository.countCompletedOnboarding(userId) > 0);
	}
}
