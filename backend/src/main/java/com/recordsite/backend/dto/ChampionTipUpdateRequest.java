package com.recordsite.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 팁 수정 요청. 작성 시 정한 비밀번호가 일치해야 내용을 바꿀 수 있다.
// 비밀번호 일치 여부(도메인 규칙)는 서비스가 판정한다 — 여기서는 값이 왔는지만 본다.
public record ChampionTipUpdateRequest(
        @NotBlank(message = "비밀번호를 입력하세요.")
        String password,

        @NotBlank(message = "팁 내용을(를) 입력하세요.")
        @Size(max = 500, message = "팁 내용은(는) 500자 이내여야 합니다.")
        String content
) {
}
