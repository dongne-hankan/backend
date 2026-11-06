package com.sixteez.dongnehankan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import com.sixteez.dongnehankan.auth.service.TokenProvider;
import com.sixteez.dongnehankan.user.entity.User;
import com.sixteez.dongnehankan.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalLoginFlowTests {

	private static final AtomicInteger IDS = new AtomicInteger(8000);

	private static final String PASSWORD = "password-123";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private TokenProvider tokenProvider;

	@Autowired
	private UserRepository userRepository;

	@Test
	void userSignsUpWithLoginIdThenLogsIn() throws Exception {
		String loginId = nextLoginId();
		loginIdAvailability(loginId).andExpect(jsonPath("$.data.available").value(true));

		MvcResult signup = signup(loginId, PASSWORD, "성민", true).andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.user.nickname").value("성민"))
			.andExpect(jsonPath("$.data.user.isAdmin").value(false))
			.andExpect(jsonPath("$.data.user.onboardingCompleted").value(false))
			.andReturn();

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(signup)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.nickname").value("성민"))
			.andExpect(jsonPath("$.data.loginId").value(loginId));

		loginIdAvailability(loginId.toUpperCase()).andExpect(jsonPath("$.data.available").value(false));
		assertThat(userRepository.findByLoginId(loginId).orElseThrow().getPasswordHash()).isNotEqualTo(PASSWORD);

		login(loginId.toUpperCase(), PASSWORD).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
			.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
			.andExpect(jsonPath("$.data.user.nickname").value("성민"));
	}

	@Test
	void duplicateLoginIdIsRejected() throws Exception {
		String loginId = nextLoginId();
		signup(loginId, PASSWORD, "성민", true).andExpect(status().isCreated());

		signup(loginId.toUpperCase(), PASSWORD, "다른사람", true).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
	}

	@Test
	void signupRequiresPrivacyAgreement() throws Exception {
		signup(nextLoginId(), PASSWORD, "성민", false).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PRIVACY_AGREEMENT_REQUIRED"));
	}

	@Test
	void signupValidatesLoginIdAndPassword() throws Exception {
		signup("한글아이디", PASSWORD, "성민", true).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("loginId"));
		signup(nextLoginId(), "short", "성민", true).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("password"));
		mockMvc.perform(get("/api/auth/login-id-availability").param("loginId", "ab"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void loginFailureDoesNotRevealWhichPartWasWrong() throws Exception {
		String loginId = nextLoginId();
		signup(loginId, PASSWORD, "성민", true);

		String wrongPassword = body(login(loginId, "wrong-password").andExpect(status().isUnauthorized()));
		String unknownId = body(login(nextLoginId(), PASSWORD).andExpect(status().isUnauthorized()));

		assertThat(wrongPassword).isEqualTo(unknownId).contains("LOGIN_FAILED");
	}

	@Test
	void passwordChangeRequiresCurrentPasswordAndRevokesRefreshTokens() throws Exception {
		String loginId = nextLoginId();
		MvcResult signup = signup(loginId, PASSWORD, "성민", true).andReturn();
		String refreshToken = readJson(signup, "$.data.refreshToken");

		changePassword(bearer(signup), "wrong-password", "new-password-123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("CURRENT_PASSWORD_MISMATCH"));

		changePassword(bearer(signup), PASSWORD, "new-password-123").andExpect(status().isNoContent());

		mockMvc.perform(post("/api/auth/token/refresh").contentType(MediaType.APPLICATION_JSON)
			.content("{\"refreshToken\":\"" + refreshToken + "\"}")).andExpect(status().isUnauthorized());
		login(loginId, PASSWORD).andExpect(status().isUnauthorized());
		login(loginId, "new-password-123").andExpect(status().isOk());
	}

	@Test
	void kakaoUserHasNoPasswordToChange() throws Exception {
		User kakaoUser = userRepository
			.save(new User(String.valueOf(IDS.incrementAndGet()), "카카오", LocalDateTime.now()));
		String bearer = "Bearer " + tokenProvider.createAccessToken(kakaoUser.getId(), false);

		mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer))
			.andExpect(jsonPath("$.data.loginId").doesNotExist());
		changePassword(bearer, PASSWORD, "new-password-123").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PASSWORD_NOT_SET"));
	}

	private String nextLoginId() {
		return "user" + IDS.incrementAndGet();
	}

	private ResultActions loginIdAvailability(String loginId) throws Exception {
		return mockMvc.perform(get("/api/auth/login-id-availability").param("loginId", loginId))
			.andExpect(status().isOk());
	}

	private ResultActions signup(String loginId, String password, String nickname, boolean agreed) throws Exception {
		return postJson("/api/auth/local/signup", "{\"loginId\":\"" + loginId + "\",\"password\":\"" + password
				+ "\",\"nickname\":\"" + nickname + "\",\"privacyAgreed\":" + agreed + "}");
	}

	private ResultActions login(String loginId, String password) throws Exception {
		return postJson("/api/auth/login", "{\"loginId\":\"" + loginId + "\",\"password\":\"" + password + "\"}");
	}

	private ResultActions changePassword(String bearer, String currentPassword, String newPassword) throws Exception {
		return mockMvc.perform(patch("/api/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"currentPassword\":\"" + currentPassword + "\",\"newPassword\":\"" + newPassword + "\"}"));
	}

	private ResultActions postJson(String url, String body) throws Exception {
		return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String bearer(MvcResult result) throws Exception {
		return "Bearer " + readJson(result, "$.data.accessToken");
	}

	private String body(ResultActions actions) throws Exception {
		return actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private String readJson(MvcResult result, String path) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
	}
}
