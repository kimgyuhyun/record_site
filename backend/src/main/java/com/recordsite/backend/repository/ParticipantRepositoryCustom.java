package com.recordsite.backend.repository;

import com.recordsite.backend.dto.ChampionPickCount;
import com.recordsite.backend.dto.ChampionPositionAggregate;
import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.PlayedChampionAggregate;
import com.recordsite.backend.dto.champion.ChampionMatchupAggregate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface ParticipantRepositoryCustom {
    // puuid로 매치목록 페이징 조회
    Page<MatchRecordDto> findMatchRecordByPuuid(String puuid, Pageable pageable);

    // puuid의 챔피언별 통계 집계. queueId가 null이면 전체 큐, 아니면 해당 큐만(솔로 420/자유 440)
    List<PlayedChampionAggregate> aggregatePlayedChampions(String puuid, Integer queueId);

    // 전역 챔피언×포지션 통계 집계(전 소환사). queueId가 null이면 전체 큐, 아니면 해당 큐만.
    // 포지션이 비어있는 행(리메이크/특수 모드 등)은 제외한다.
    List<ChampionPositionAggregate> aggregateChampionStatsByPosition(Integer queueId);

    // 주어진 puuid들의 (puuid, championId)별 플레이 횟수. 랭킹 페이지 '모스트 챔피언' 계산용.
    List<ChampionPickCount> findChampionPickCounts(Collection<String> puuids);

    // 챔피언 상세 카운터: 같은 포지션·상대 팀의 챔피언별로 기준 챔피언의 만난 판/이긴 판 집계. queueId null=전체 큐.
    List<ChampionMatchupAggregate> aggregateMatchups(int championId, Integer queueId);

    // 같은 큐의 전체 참가자 수(픽률 분모: ÷10 ≈ 매치 수). queueId null=전체 큐.
    long countParticipantsByQueue(Integer queueId);
}
