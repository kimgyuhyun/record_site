package com.recordsite.backend.repository;

// 판당 LP 귀속에 필요한 랭크 게임 한 줄의 사영(projection): 매치 ID, 게임 생성 시각(epoch ms), 승패.
// LP 측정값 두 개 사이에 낀 게임을 가려내고(시각), 그 한 판에 증감을 표시하며(매치 ID),
// 증감 부호가 승패와 맞는지 검증(승패)하는 데 쓴다.
public interface RankedGameView {
    String getMatchId();
    Long getGameCreation();
    Boolean getWin();
}
