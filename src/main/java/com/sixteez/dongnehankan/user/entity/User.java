package com.sixteez.dongnehankan.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long id;

	private String kakaoId;

	private String loginId;

	private String passwordHash;

	private String nickname;

	private boolean isAdmin;

	private LocalDateTime privacyAgreedAt;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@UpdateTimestamp
	private LocalDateTime updatedAt;

	public User(String kakaoId, String nickname, LocalDateTime privacyAgreedAt) {
		this.kakaoId = kakaoId;
		this.nickname = nickname;
		this.privacyAgreedAt = privacyAgreedAt;
	}

	public static User withLoginId(String loginId, String passwordHash, String nickname,
			LocalDateTime privacyAgreedAt) {
		User user = new User(null, nickname, privacyAgreedAt);
		user.loginId = loginId;
		user.passwordHash = passwordHash;
		return user;
	}

	public boolean hasPassword() {
		return passwordHash != null;
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}
}
