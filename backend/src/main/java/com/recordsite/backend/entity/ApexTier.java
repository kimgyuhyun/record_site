package com.recordsite.backend.entity;

// 상위 티어(사다리) 구분. League-V4 by-queue 엔드포인트의 경로 조각과 사다리 정렬 순서를 함께 가진다.
public enum ApexTier {

    CHALLENGER("challengerleagues", 0),
    GRANDMASTER("grandmasterleagues", 1),
    MASTER("masterleagues", 2);

    private final String pathSegment; // /lol/league/v4/{pathSegment}/by-queue/{queue}
    private final int order;          // 사다리에서 위에 오는 순서(작을수록 상위)

    ApexTier(String pathSegment, int order) {
        this.pathSegment = pathSegment;
        this.order = order;
    }

    public String pathSegment() {
        return pathSegment;
    }

    public int order() {
        return order;
    }
}
