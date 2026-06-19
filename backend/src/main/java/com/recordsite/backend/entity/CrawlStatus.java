package com.recordsite.backend.entity;

// 매치망 크롤러 작업 큐의 처리 상태.
public enum CrawlStatus {
    PENDING, // 아직 처리 안 됨(스케줄러 대기)
    DONE,    // 매치 수집 완료
    FAILED   // 처리 중 예외 발생(재시도 안 함, 추적용)
}
