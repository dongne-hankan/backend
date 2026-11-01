package com.sixteez.dongnehankan.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class TokenProvider {

	private static final String TYPE_CLAIM = "type";
	private static final String ACCESS_TYPE = "access";
	private static final String SIGNUP_TYPE = "signup";
	private static final Duration SIGNUP_TOKEN_TTL = Duration.ofMinutes(10);

	private final SecureRandom secureRandom = new SecureRandom();

	private final SecretKey key;

	private final Duration accessTokenTtl;

	private final Duration refreshTokenTtl;

	public TokenProvider(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-token-minutes}") long accessTokenMinutes,
			@Value("${app.jwt.refresh-token-days}") long refreshTokenDays) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.accessTokenTtl = Duration.ofMinutes(accessTokenMinutes);
		this.refreshTokenTtl = Duration.ofDays(refreshTokenDays);
	}

	public record AccessClaims(Long userId, boolean admin) {
	}

	public record SignupClaims(String kakaoId, String nickname) {
	}

	public String createAccessToken(Long userId, boolean admin) {
		return Jwts.builder()
			.subject(userId.toString())
			.claim(TYPE_CLAIM, ACCESS_TYPE)
			.claim("admin", admin)
			.expiration(Date.from(Instant.now().plus(accessTokenTtl)))
			.signWith(key)
			.compact();
	}

	public AccessClaims parseAccessToken(String token) {
		try {
			Claims claims = parse(token, ACCESS_TYPE);
			return new AccessClaims(Long.valueOf(claims.getSubject()), claims.get("admin", Boolean.class));
		}
		catch (ExpiredJwtException e) {
			throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
		}
		catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.AUTH_REQUIRED);
		}
	}

	public String createSignupToken(String kakaoId, String nickname) {
		return Jwts.builder()
			.subject(kakaoId)
			.claim(TYPE_CLAIM, SIGNUP_TYPE)
			.claim("nickname", nickname)
			.expiration(Date.from(Instant.now().plus(SIGNUP_TOKEN_TTL)))
			.signWith(key)
			.compact();
	}

	public SignupClaims parseSignupToken(String token) {
		try {
			Claims claims = parse(token, SIGNUP_TYPE);
			return new SignupClaims(claims.getSubject(), claims.get("nickname", String.class));
		}
		catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.SIGNUP_TOKEN_EXPIRED);
		}
	}

	public String createRandomToken() {
		byte[] bytes = new byte[32];
		secureRandom.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public Instant refreshTokenExpiry() {
		return Instant.now().plus(refreshTokenTtl);
	}

	private Claims parse(String token, String type) {
		Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
		if (!type.equals(claims.get(TYPE_CLAIM, String.class))) {
			throw new JwtException("Unexpected token type");
		}
		return claims;
	}
}
