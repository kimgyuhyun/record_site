package com.recordsite.backend.service;

// 전적 갱신 진행 상황을 외부(잡 상태 저장소)로 흘려보내는 콜백.
// 수집 로직(MatchService)은 저장 매체(Redis 등)를 모른 채 "총 개수 확정"과 "한 건 처리됨"만 알린다.
public interface RefreshProgress {

    // 신규 매치 수가 확정된 시점에 1회 호출(진행률의 분모).
    void onTotal(int total);

    // 매치 한 건 처리(성공/스킵 무관)마다 호출(진행률의 분자 +1).
    void onMatchDone();

    // 진행률 추적이 필요 없는 호출(테스트, 동기 직접 호출)용 무동작 구현.
    RefreshProgress NONE = new RefreshProgress() {
        @Override public void onTotal(int total) { }
        @Override public void onMatchDone() { }
    };
}
