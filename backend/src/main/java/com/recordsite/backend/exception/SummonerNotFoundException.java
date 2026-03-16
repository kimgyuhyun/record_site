package com.recordsite.backend.exception;

public class SummonerNotFoundException extends RuntimeException {
    public SummonerNotFoundException(String name) {
        super("summoner not found: " + name);
        // 부모 클래스에 생성자 호출 / 부모 예외의 message 를 설정하는 용도
    }
}
