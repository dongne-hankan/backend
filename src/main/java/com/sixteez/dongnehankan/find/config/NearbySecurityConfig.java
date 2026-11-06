package com.sixteez.dongnehankan.find.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.global.config.JwtAuthenticationFilter;
import com.sixteez.dongnehankan.user.repository.UserRepository;

@Configuration
public class NearbySecurityConfig {

	// 공용 체인은 GET만 열어 두어서, 비회원도 POST로 부를 수 있게 이 경로만 연다.
	@Bean
	@Order(1)
	SecurityFilterChain nearbySecurityFilterChain(HttpSecurity http, TokenProvider tokenProvider,
			UserRepository userRepository) throws Exception {
		return http.securityMatcher("/api/find/nearby")
			.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
			.addFilterBefore(new JwtAuthenticationFilter(tokenProvider, userRepository),
					UsernamePasswordAuthenticationFilter.class)
			.build();
	}
}
