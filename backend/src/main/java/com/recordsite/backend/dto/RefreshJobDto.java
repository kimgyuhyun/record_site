package com.recordsite.backend.dto;

// 전적 갱신 작업의 현재 상태. 제출(POST) 응답과 폴링(GET) 응답에 공통으로 쓰인다.
// status: PENDING(큐 대기) → PROCESSING(수집 중) → DONE/FAILED.
public record RefreshJobDto(String jobId, String status, int total, int done) {
}
