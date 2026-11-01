package com.sixteez.dongnehankan.global.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.auth.service.TokenProvider.AccessClaims;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 공개 API는 토큰이 없거나 만료되어도, 탈퇴한 회원의 토큰이어도 비회원으로 통과시킨다. 회원 API의 오류 응답은 SecurityConfig가 만든다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	static final String ERROR_ATTRIBUTE = "authError";

	private static final String BEARER_PREFIX = "Bearer ";

	private final TokenProvider tokenProvider;

	private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		bearerToken(request).ifPresent(token -> authenticate(token, request));
		chain.doFilter(request, response);
	}

	private Optional<String> bearerToken(HttpServletRequest request) {
		return Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
			.filter(header -> header.startsWith(BEARER_PREFIX))
			.map(header -> header.substring(BEARER_PREFIX.length()));
	}

	private void authenticate(String token, HttpServletRequest request) {
		try {
			AccessClaims claims = tokenProvider.parseAccessToken(token);
			if (!userRepository.existsById(claims.userId())) {
				request.setAttribute(ERROR_ATTRIBUTE, ErrorCode.AUTH_REQUIRED);
				return;
			}
			String role = claims.admin() ? "ROLE_ADMIN" : "ROLE_USER";
			SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(claims.userId(), null,
						List.of(new SimpleGrantedAuthority(role))));
		}
		catch (BusinessException e) {
			request.setAttribute(ERROR_ATTRIBUTE, e.getErrorCode());
		}
	}
}
