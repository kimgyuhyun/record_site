package com.recordsite.backend.exception;

// 같은 사람이 한 팁에 두 번 투표·신고한 경우(1인 1회 제약 위반).
// 생성자로 받는 문구는 사용자에게 그대로 노출할 목적으로 쓴 것이다.
public class DuplicateTipInteractionException extends RuntimeException {
    public DuplicateTipInteractionException(String userMessage) {
        super(userMessage);
    }
}
