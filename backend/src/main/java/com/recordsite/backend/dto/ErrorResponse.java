package com.recordsite.backend.dto;

// 에러 응답 본문. code 는 클라이언트 분기용, message 는 사용자에게 그대로 보여줄 문구다.
// 스택 트레이스·SQL·내부 클래스명·원본 예외 메시지는 담지 않는다(그건 로그에만 남긴다).
// message 라는 키 이름은 프론트(ChampionTips.jsx 의 e.response.data.message)에 맞춘 것이라 바꾸면 안 된다.
public record ErrorResponse(String code, String message) {
}
