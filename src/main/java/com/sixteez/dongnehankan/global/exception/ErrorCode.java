package com.sixteez.dongnehankan.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
	AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "접근 토큰이 만료되었습니다."),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "다시 로그인해 주세요."),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 맞지 않아요."),
	LOGIN_ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 아이디예요."),
	CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 맞지 않아요."),
	PASSWORD_NOT_SET(HttpStatus.BAD_REQUEST, "카카오로 가입한 계정은 비밀번호가 없어요."),
	SIGNUP_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "카카오 로그인부터 다시 진행해 주세요."),
	PRIVACY_AGREEMENT_REQUIRED(HttpStatus.BAD_REQUEST, "개인정보 수집·이용에 동의해 주세요."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	ADMIN_CANNOT_WITHDRAW(HttpStatus.FORBIDDEN, "운영자 계정은 탈퇴할 수 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
	CONDITION_REQUIRED(HttpStatus.BAD_REQUEST, "지역, 경험, 시간을 모두 골라 주세요."),
	REGION_NOT_FOUND(HttpStatus.BAD_REQUEST, "없는 지역입니다."),
	TAG_NOT_FOUND(HttpStatus.BAD_REQUEST, "없는 경험 태그입니다."),
	COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "지금은 볼 수 없는 코스예요."),
	PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "지금은 볼 수 없는 장소예요."),
	COURSE_HIDDEN(HttpStatus.CONFLICT, "지금은 담을 수 없는 코스예요."),
	FEELING_REQUIRED(HttpStatus.BAD_REQUEST, "느낌을 골라 주세요."),
	RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "없는 기록이에요."),
	RECORD_FORBIDDEN(HttpStatus.FORBIDDEN, "내 기록만 바꿀 수 있어요."),
	KAKAO_CODE_INVALID(HttpStatus.BAD_REQUEST, "카카오 인가 코드가 유효하지 않습니다."),
	KAKAO_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "카카오 서버 응답에 실패했습니다."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}
}
