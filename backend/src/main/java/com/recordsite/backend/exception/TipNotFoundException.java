package com.recordsite.backend.exception;

// 팁이 없거나 이미 삭제된 경우. HTTP 상태 코드는 알지 않는다 — 매핑은 GlobalExceptionHandler 가 한다.
public class TipNotFoundException extends RuntimeException {
    public TipNotFoundException() {
        super("팁을 찾을 수 없습니다.");
    }
}
