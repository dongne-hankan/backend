package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiReliabilityTests {

	private MockRestServiceServer server;

	private GeminiClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		client = new GeminiClient(builder, "test-key", "test-model", "https://gemini.test");
	}

	@Test
	void joinsAnswerPartsWithoutExposingThoughts() {
		server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
			.andRespond(withSuccess("""
					{"candidates":[{"finishReason":"STOP","content":{"parts":[
					{"text":"private reasoning","thought":true},null,
					{"text":"{\\"ok\\":"},{"text":"true}"}]}}]}
					""", MediaType.APPLICATION_JSON));

		assertThat(client.generateJson("JSON으로 답해 주세요")).contains("{\"ok\":true}");
		server.verify();
	}

	@Test
	void discardsTruncatedJsonEvenIfItLooksValid() {
		server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
			.andRespond(withSuccess("""
					{"candidates":[{"finishReason":"MAX_TOKENS","content":{"parts":[{"text":"{}"}]}}]}
					""", MediaType.APPLICATION_JSON));

		assertThat(client.generateJson("JSON으로 답해 주세요")).isEmpty();
	}

	@Test
	void sharesQuotaCooldownBetweenAiFeaturesWithoutRetrying() {
		server.expect(requestTo("https://gemini.test/v1beta/models/test-model:generateContent"))
			.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertThat(client.generateJson("지역 추천 이유")).isEmpty();
		assertThat(client.generateJson("주변 산책 코스")).isEmpty();
		server.verify();
	}

	@Test
	void skipsBlankPromptsWithoutCallingProvider() {
		assertThat(client.generateJson(" ")).isEmpty();
		server.verify();
	}
}
