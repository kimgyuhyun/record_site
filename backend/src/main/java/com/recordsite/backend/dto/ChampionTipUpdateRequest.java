package com.recordsite.backend.dto;

// 팁 수정 요청. 작성 시 정한 비밀번호가 일치해야 내용을 바꿀 수 있다.
public record ChampionTipUpdateRequest(String password, String content) {
}
