package com.recordsite.backend.exception;

// 팁 수정·삭제 시 작성 비밀번호가 일치하지 않는 경우.
public class TipPasswordMismatchException extends RuntimeException {
    public TipPasswordMismatchException() {
        super("비밀번호가 일치하지 않습니다.");
    }
}
