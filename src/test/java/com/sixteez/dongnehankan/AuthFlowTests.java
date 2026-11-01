package com.sixteez.dongnehankan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.sixteez.dongnehankan.auth.service.KakaoClient;
import com.sixteez.dongnehankan.auth.service.KakaoClient.KakaoProfile;
import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.global.exception.BusinessException;
import com.sixteez.dongnehankan.global.exception.ErrorCode;
import com.sixteez.dongnehankan.user.repository.UserRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTests {

	private static final AtomicInteger KAKAO_IDS = new AtomicInteger(1000);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Value("${app.jwt.secret}")
	private String secret;

	@MockitoBean
	private KakaoClient kakaoClient;

	@Test
	void newUserSignsUpThenLogsInAsExistingMember() throws Exception {
		String kakaoId = nextKakaoId();
		givenKakaoProfile(kakaoId, "성민");

		String signupToken = readJson(login(), "$.data.signupToken");

		MvcResult signup = signup(signupToken, true, null).andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.user.nickname").value("성민"))
			.andExpect(jsonPath("$.data.user.isAdmin").value(false))
			.andExpect(jsonPath("$.data.user.onboardingCompleted").value(false))
			.andReturn();
		String accessToken = readJson(signup, "$.data.accessToken");

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.nickname").value("성민"))
			.andExpect(jsonPath("$.data.savedCourseCount").value(0))
			.andExpect(jsonPath("$.data.joinedAt", containsString("+09:00")));

		mockMvc.perform(kakaoLogin())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.signupRequired").value(false))
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.signupToken").doesNotExist());
	}

	@Test
	void signupRequiresPrivacyAgreement() throws Exception {
		signup(tokenProvider.createSignupToken(nextKakaoId(), "성민"), false, null).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PRIVACY_AGREEMENT_REQUIRED"));
	}

	@Test
	void longKakaoNicknameIsShortenedToFitTheColumn() throws Exception {
		signup(tokenProvider.createSignupToken(nextKakaoId(), "아주아주아주아주아주아주긴닉네임"), true, null)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.user.nickname").value("아주아주아주아주아주아주"));
	}

	@Test
	void invalidSignupTokenIsRejected() throws Exception {
		signup("not-a-token", true, null).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("SIGNUP_TOKEN_EXPIRED"));
	}

	@Test
	void signupTokenCannotBeUsedAsAccessToken() throws Exception {
		String signupToken = tokenProvider.createSignupToken(nextKakaoId(), "성민");
		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + signupToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
	}

	@Test
	void expiredAccessTokenGetsTheRefreshSignal() throws Exception {
		String expired = Jwts.builder()
			.subject("1")
			.claim("type", "access")
			.claim("admin", false)
			.expiration(Date.from(Instant.now().minusSeconds(60)))
			.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
			.compact();

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_TOKEN_EXPIRED"));

		mockMvc.perform(get("/api/find/options").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
			.andExpect(status().isOk());
	}

	@Test
	void refreshRotatesTheTokenAndLogoutInvalidatesIt() throws Exception {
		MvcResult signup = signup(tokenProvider.createSignupToken(nextKakaoId(), "성민"), true, null).andReturn();
		String firstRefresh = readJson(signup, "$.data.refreshToken");

		MvcResult refreshed = refresh(firstRefresh).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.user").doesNotExist())
			.andReturn();
		String secondRefresh = readJson(refreshed, "$.data.refreshToken");
		String accessToken = readJson(refreshed, "$.data.accessToken");

		refresh(firstRefresh).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));

		mockMvc
			.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"refreshToken\":\"" + secondRefresh + "\"}"))
			.andExpect(status().isNoContent());

		refresh(secondRefresh).andExpect(status().isUnauthorized());
	}

	@Test
	void nicknameCanBeChangedWithinTheLimit() throws Exception {
		String accessToken = accessTokenForNewUser();

		mockMvc
			.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"동네초보\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.nickname").value("동네초보"));

		mockMvc
			.perform(patch("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"nickname\":\"가\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
			.andExpect(jsonPath("$.errors[0].field").value("nickname"));
	}

	@Test
	void withdrawalDeletesTheUserAndTokens() throws Exception {
		String kakaoId = nextKakaoId();
		MvcResult signup = signup(tokenProvider.createSignupToken(kakaoId, "성민"), true, null).andReturn();
		String accessToken = readJson(signup, "$.data.accessToken");
		String refreshToken = readJson(signup, "$.data.refreshToken");

		mockMvc.perform(delete("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isNoContent());

		assertThat(userRepository.findByKakaoId(kakaoId)).isEmpty();
		refresh(refreshToken).andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/my/saved-courses").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
		mockMvc.perform(get("/api/find/options").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
			.andExpect(status().isOk());
	}

	@Test
	void invalidKakaoCodeIsReported() throws Exception {
		given(kakaoClient.fetchProfile(any(), any()))
			.willThrow(new BusinessException(ErrorCode.KAKAO_CODE_INVALID));

		mockMvc.perform(kakaoLogin())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("KAKAO_CODE_INVALID"));
	}

	private void givenKakaoProfile(String kakaoId, String nickname) {
		given(kakaoClient.fetchProfile(any(), any())).willReturn(new KakaoProfile(kakaoId, nickname));
	}

	private String nextKakaoId() {
		return String.valueOf(KAKAO_IDS.incrementAndGet());
	}

	private String accessTokenForNewUser() throws Exception {
		return readJson(signup(tokenProvider.createSignupToken(nextKakaoId(), "성민"), true, null).andReturn(),
				"$.data.accessToken");
	}

	private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder kakaoLogin() {
		return post("/api/auth/kakao").contentType(MediaType.APPLICATION_JSON)
			.content("{\"authorizationCode\":\"code\",\"redirectUri\":\"http://localhost:5173/oauth/kakao\"}");
	}

	private MvcResult login() throws Exception {
		return mockMvc.perform(kakaoLogin())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.signupRequired").value(true))
			.andReturn();
	}

	private org.springframework.test.web.servlet.ResultActions signup(String signupToken, boolean agreed,
			String nickname) throws Exception {
		String nicknameField = nickname == null ? "" : ",\"nickname\":\"" + nickname + "\"";
		return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
			.content("{\"signupToken\":\"" + signupToken + "\",\"privacyAgreed\":" + agreed + nicknameField + "}"));
	}

	private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/auth/token/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refreshToken + "\"}"));
	}

	private String readJson(MvcResult result, String path) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
	}
}
