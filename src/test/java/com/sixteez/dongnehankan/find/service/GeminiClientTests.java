package com.sixteez.dongnehankan.find.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiClientTests {

	private static final String BASE_URL = "https://gemini.test";

	private static final String URL = BASE_URL + "/v1beta/models/test-model:generateContent";

	private RestClient.Builder builder;

	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
	}

	@Test
	void returnsGeneratedJson() {
		GeminiClient client = new GeminiClient(builder, "test-key", "test-model", BASE_URL);
		server.expect(requestTo(URL))
			.andExpect(method(HttpMethod.POST))
			.andExpect(header("x-goog-api-key", "test-key"))
			.andExpect(jsonPath("$.contents[0].parts[0].text").value("코스를 골라줘"))
			.andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
			.andRespond(withSuccess("""
					{"candidates":[{"content":{"parts":[{"text":"{\\"courses\\":[]}"}]}}]}
					""", MediaType.APPLICATION_JSON));

		assertThat(client.generateJson("코스를 골라줘")).contains("{\"courses\":[]}");
		server.verify();
	}

	@Test
	void skipsCallWithoutApiKey() {
		GeminiClient client = new GeminiClient(builder, "", "test-model", BASE_URL);

		assertThat(client.generateJson("코스를 골라줘")).isEmpty();
		server.verify();
	}

	@Test
	void returnsEmptyOnServerError() {
		GeminiClient client = new GeminiClient(builder, "test-key", "test-model", BASE_URL);
		server.expect(requestTo(URL)).andRespond(withServerError());

		assertThat(client.generateJson("코스를 골라줘")).isEmpty();
	}

	@Test
	void returnsEmptyWithoutCandidates() {
		GeminiClient client = new GeminiClient(builder, "test-key", "test-model", BASE_URL);
		server.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

		assertThat(client.generateJson("코스를 골라줘")).isEmpty();
	}
}
