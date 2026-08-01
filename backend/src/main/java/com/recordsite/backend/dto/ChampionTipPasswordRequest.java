package com.recordsite.backend.dto;

import jakarta.validation.constraints.NotBlank;

// 팁 삭제 시 본인 확인용 비밀번호 요청 본문.
// 일치 여부(도메인 규칙)는 서비스가 판정한다 — 여기서는 값이 왔는지만 본다.
public record ChampionTipPasswordRequest(
        @NotBlank(message = "비밀번호를 입력하세요.")
        String password
) {
}
