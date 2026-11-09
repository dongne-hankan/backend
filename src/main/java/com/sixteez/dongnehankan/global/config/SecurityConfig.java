package com.sixteez.dongnehankan.global.config;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.global.exception.ErrorResponse;
import com.sixteez.dongnehankan.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

	private final ObjectMapper objectMapper;

	private final TokenProvider tokenProvider;

	private final UserRepository userRepository;

	public SecurityConfig(ObjectMapper objectMapper, TokenProvider tokenProvider, UserRepository userRepository) {
		this.objectMapper = objectMapper;
		this.tokenProvider = tokenProvider;
		this.userRepository = userRepository;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/liveness")
				.permitAll()
				.requestMatchers(HttpMethod.POST, "/api/auth/logout")
				.authenticated()
				.requestMatchers("/api/auth/**")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/find/**", "/api/courses/**", "/api/places/**")
				.permitAll()
				.requestMatchers("/api/admin/**")
				.hasRole("ADMIN")
				.anyRequest()
				.authenticated())
			.addFilterBefore(new JwtAuthenticationFilter(tokenProvider, userRepository),
					UsernamePasswordAuthenticationFilter.class)
			.exceptionHandling(handler -> handler
				.authenticationEntryPoint((request, response, e) -> writeError(response, authError(request)))
				.accessDeniedHandler((request, response, e) -> writeError(response, ErrorCode.FORBIDDEN)))
			.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", configuration);
		return source;
	}

	private ErrorCode authError(HttpServletRequest request) {
		return request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE) instanceof ErrorCode errorCode ? errorCode
				: ErrorCode.AUTH_REQUIRED;
	}

	private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		HttpStatus status = errorCode.getStatus();
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode));
	}
}
