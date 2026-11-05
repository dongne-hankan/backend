package com.sixteez.dongnehankan.find.service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GeminiClient {

	private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

	private final RestClient restClient;

	private final String apiKey;

	private final String model;

	@Autowired
	public GeminiClient(@Value("${GEMINI_API_KEY:}") String apiKey,
			@Value("${GEMINI_MODEL:gemini-3.1-flash-lite}") String model,
			@Value("${GEMINI_BASE_URL:https://generativelanguage.googleapis.com}") String baseUrl) {
		this(RestClient.builder().requestFactory(requestFactory()), apiKey, model, baseUrl);
	}

	GeminiClient(RestClient.Builder builder, String apiKey, String model, String baseUrl) {
		this.restClient = builder.baseUrl(baseUrl).build();
		this.apiKey = apiKey;
		this.model = model;
	}

	private static SimpleClientHttpRequestFactory requestFactory() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(3));
		requestFactory.setReadTimeout(Duration.ofSeconds(10));
		return requestFactory;
	}

	private record Part(String text) {
	}

	private record Content(List<Part> parts) {
	}

	private record Candidate(Content content) {
	}

	private record GenerateResponse(List<Candidate> candidates) {
	}

	public Optional<String> generateJson(String prompt) {
		if (!StringUtils.hasText(apiKey)) {
			return Optional.empty();
		}
		Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
				"generationConfig", Map.of("responseMimeType", "application/json", "temperature", 0.7));
		try {
			GenerateResponse response = restClient.post()
				.uri("/v1beta/models/{model}:generateContent", model)
				.header("x-goog-api-key", apiKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(GenerateResponse.class);
			return firstText(response);
		}
		catch (RestClientException e) {
			log.warn("Gemini 호출 실패: {}", e.getMessage());
			return Optional.empty();
		}
	}

	private Optional<String> firstText(GenerateResponse response) {
		if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
			return Optional.empty();
		}
		Content content = response.candidates().get(0).content();
		if (content == null || content.parts() == null || content.parts().isEmpty()) {
			return Optional.empty();
		}
		return Optional.ofNullable(content.parts().get(0).text()).filter(StringUtils::hasText);
	}
}
