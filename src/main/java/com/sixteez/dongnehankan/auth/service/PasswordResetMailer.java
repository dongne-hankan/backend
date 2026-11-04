package com.sixteez.dongnehankan.auth.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class PasswordResetMailer {

	private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);

	private final ObjectProvider<JavaMailSender> mailSender;

	private final String resetUrl;

	private final String from;

	private final boolean mailConfigured;

	public PasswordResetMailer(ObjectProvider<JavaMailSender> mailSender,
			@Value("${app.password-reset.url}") String resetUrl, @Value("${app.mail.from}") String from,
			@Value("${spring.mail.host:}") String mailHost) {
		this.mailSender = mailSender;
		this.resetUrl = resetUrl;
		this.from = from;
		this.mailConfigured = StringUtils.hasText(mailHost);
	}

	// 메일 발송 실패가 응답에 드러나면 계정 존재 여부를 알 수 있으므로 로그만 남긴다.
	public void send(String email, String token, Duration validFor) {
		String link = UriComponentsBuilder.fromUriString(resetUrl).queryParam("token", token).encode().toUriString();
		JavaMailSender sender = mailConfigured ? mailSender.getIfAvailable() : null;
		if (sender == null) {
			log.info("메일 서버가 설정되지 않아 재설정 링크를 로그로 남깁니다. to={} link={}", email, link);
			return;
		}
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(email);
		message.setSubject("[동네 한 칸] 비밀번호 재설정 안내");
		message.setText("""
				아래 링크에서 새 비밀번호를 설정해 주세요.
				링크는 %d분 동안 한 번만 쓸 수 있어요.

				%s

				직접 요청하지 않았다면 이 메일을 무시해 주세요.
				""".formatted(validFor.toMinutes(), link));
		try {
			sender.send(message);
		}
		catch (MailException e) {
			log.error("비밀번호 재설정 메일 발송에 실패했습니다. to={}", email, e);
		}
	}
}
