package com.recordsite.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 챔피언 팁 작성 요청. password 는 삭제/수정용 키(비로그인). language 는 작성자 언어(미지정 시 한국어).
// 형식(빈값·길이) 검증은 여기서 하고, 같은 검증을 서비스에서 한 번 더 한다 — 한쪽만 두지 않는다.
// 길이 상수는 ChampionTipService 와 같은 값이어야 한다(애노테이션은 상수식만 받아 참조할 수 없다).
// 메시지는 사용자에게 그대로 노출되므로 서비스 쪽 문구와 같게 맞춘다.
public record ChampionTipCreateRequest(
        int championId,

        @NotBlank(message = "닉네임을(를) 입력하세요.")
        @Size(max = 20, message = "닉네임은(는) 20자 이내여야 합니다.")
        String nickname,

        @NotBlank(message = "팁 내용을(를) 입력하세요.")
        @Size(max = 500, message = "팁 내용은(는) 500자 이내여야 합니다.")
        String content,

        @NotBlank(message = "비밀번호를 입력하세요.")
        @Size(min = 4, max = 30, message = "비밀번호는 4~30자여야 합니다.")
        String password,

        String language
) {
}
