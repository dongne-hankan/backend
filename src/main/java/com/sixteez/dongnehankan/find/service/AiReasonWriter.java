package com.sixteez.dongnehankan.find.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse;
import com.sixteez.dongnehankan.course.dto.CourseSummaryResponse.TagItem;
import com.sixteez.dongnehankan.find.dto.DurationOption;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class AiReasonWriter {

	private static final Logger log = LoggerFactory.getLogger(AiReasonWriter.class);

	private static final int MAX_REASON_LENGTH = 120;

	private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

	private final GeminiClient geminiClient;

	private final ObjectMapper objectMapper;

	private final long timeoutMillis;

	private final Map<String, String> cache = new ConcurrentHashMap<>();

	private final Set<String> pending = ConcurrentHashMap.newKeySet();

	// 추천은 2초 안에 나가야 해서, 늦으면 이유 없이 보낸다.
	@Autowired
	public AiReasonWriter(GeminiClient geminiClient, ObjectMapper objectMapper,
			@Value("${AI_REASON_TIMEOUT_MS:1500}") long timeoutMillis) {
		this.geminiClient = geminiClient;
		this.objectMapper = objectMapper;
		this.timeoutMillis = timeoutMillis;
	}

	private record GeminiReason(Long courseId, String reason) {
	}

	private record GeminiReasons(List<GeminiReason> reasons) {
	}

	public Map<Long, String> write(String tagName, DurationOption duration, List<CourseSummaryResponse> courses) {
		if (courses.isEmpty()) {
			return Map.of();
		}
		List<CourseSummaryResponse> missing = courses.stream()
			.filter(course -> !cache.containsKey(cacheKey(course.courseId(), tagName, duration)))
			.toList();
		if (!missing.isEmpty()) {
			generate(tagName, duration, missing);
		}
		Map<Long, String> reasons = new LinkedHashMap<>();
		for (CourseSummaryResponse course : courses) {
			String reason = cache.get(cacheKey(course.courseId(), tagName, duration));
			if (reason != null) {
				reasons.put(course.courseId(), reason);
			}
		}
		return reasons;
	}

	// 늦게 끝나도 결과는 캐시에 남아 다음 요청부터 쓴다.
	private void generate(String tagName, DurationOption duration, List<CourseSummaryResponse> missing) {
		String jobKey = tagName + "|" + duration + "|"
				+ missing.stream().map(course -> course.courseId().toString()).collect(Collectors.joining(","));
		if (!pending.add(jobKey)) {
			return;
		}
		String prompt = prompt(tagName, duration, missing);
		CompletableFuture<Void> job = CompletableFuture.supplyAsync(() -> geminiClient.generateJson(prompt), EXECUTOR)
			.thenAccept(json -> json.flatMap(this::parse)
				.map(parsed -> pick(parsed, missing))
				.ifPresent(picked -> picked.forEach(
						(courseId, reason) -> cache.put(cacheKey(courseId, tagName, duration), reason))))
			.whenComplete((ignored, e) -> pending.remove(jobKey));
		try {
			job.get(timeoutMillis, TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException | ExecutionException e) {
			log.debug("Gemini 추천 이유가 제시간에 오지 않았습니다.");
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static String cacheKey(Long courseId, String tagName, DurationOption duration) {
		return courseId + "|" + tagName + "|" + duration;
	}

	private Optional<GeminiReasons> parse(String json) {
		try {
			return Optional.ofNullable(objectMapper.readValue(json, GeminiReasons.class));
		}
		catch (JacksonException e) {
			log.warn("Gemini 추천 이유 응답을 읽지 못했습니다: {}", e.getClass().getSimpleName());
			return Optional.empty();
		}
	}

	private Map<Long, String> pick(GeminiReasons reasons, List<CourseSummaryResponse> courses) {
		if (reasons.reasons() == null) {
			return Map.of();
		}
		Map<Long, String> byCourse = reasons.reasons()
			.stream()
			.filter(reason -> reason != null && reason.courseId() != null && StringUtils.hasText(reason.reason()))
			.collect(Collectors.toMap(GeminiReason::courseId, reason -> trim(reason.reason()), (a, b) -> a));
		Map<Long, String> ordered = new LinkedHashMap<>();
		courses.stream()
			.map(CourseSummaryResponse::courseId)
			.filter(byCourse::containsKey)
			.forEach(courseId -> ordered.put(courseId, byCourse.get(courseId)));
		return ordered;
	}

	private String trim(String reason) {
		String stripped = reason.strip();
		return stripped.length() > MAX_REASON_LENGTH ? stripped.substring(0, MAX_REASON_LENGTH) : stripped;
	}

	private String prompt(String tagName, DurationOption duration, List<CourseSummaryResponse> courses) {
		String list = courses.stream()
			.map(course -> "- courseId: %d | 이름: %s | 지역: %s | 소개: %s | 경험: %s | %d분 | 주민 한마디: %s".formatted(
					course.courseId(), course.name(), course.region().name(), course.intro(),
					course.tags().stream().map(TagItem::name).collect(Collectors.joining(", ")),
					course.durationMinutes(), course.highlightQuote() == null ? "없음" : course.highlightQuote()))
			.collect(Collectors.joining("\n"));
		return """
				너는 대구·경북 동네 산책 코스를 소개하는 도우미야.
				사용자는 "%s" 경험을 %s 안에 하고 싶어 해.
				아래 코스마다 이 사용자에게 왜 맞는지 한국어 존댓말 한 문장(60자 이내)으로 써.
				- 코스 정보에 없는 사실은 지어내지 마.
				- courseId는 목록의 값을 그대로 써.

				코스 목록:
				%s

				다음 JSON 형식으로만 답해:
				{"reasons": [{"courseId": 1, "reason": "추천 이유"}]}
				""".formatted(tagName, duration.getLabel(), list);
	}
}
