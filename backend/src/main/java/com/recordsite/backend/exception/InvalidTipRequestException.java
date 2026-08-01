package com.recordsite.backend.exception;

// 팁 요청 값이 형식·길이 규칙을 어긴 경우.
// 생성자로 받는 문구는 사용자에게 그대로 노출할 목적으로 쓴 것이다(내부 정보를 담지 말 것).
public class InvalidTipRequestException extends RuntimeException {
    public InvalidTipRequestException(String userMessage) {
        super(userMessage);
    }
}
