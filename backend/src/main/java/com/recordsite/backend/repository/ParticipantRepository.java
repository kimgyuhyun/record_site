package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, ParticipantRepositoryCustom {

    @Query("""
            select p
            from Participant p
            join fetch p.match
            where p.puuid = :puuid
            order by p.match.gameCreation desc
            """)
    List<Participant> findAllParticipantListByPuuid(String puuid);
    // puuid 가 같은 모든 참가자 = 그 계정이 참가한 모든 경기

    @Query("""
            select p
            from Participant p
            join fetch p.match m
            where m.matchId = :matchId
            order by p.participantId asc
            """)
    List<Participant> findByMatchIdForParticipantList(@Param("matchId") String matchId);
    // matchId로 해당판에 참가자 목록을 asc로 정렬해서 가져옵니다.

    @Query("""
            select p
            from Participant p
            join p.match m
            where m.matchId = :matchId
            and p.puuid = :puuid
            """)
    Participant findByMatchIdAndPuuid(@Param("matchId") String matchId, @Param("puuid") String puuid);
    // matchId와 puuid로 특정 참가자 한 명 조회

    boolean existsByMatchAndParticipantId(Match match, Integer participantId);

    @Query("""
            select p
            from Participant p
            where p.match.matchId in :matchIds
            """)
    List<Participant> findByMatch_MatchIdIn(@Param("matchIds") List<String> matchIds);

    @Query("""
            select p.match.matchId
            from Participant p
            where p.puuid = :puuid
            and p.match.queueId = :queueId
            order by p.match.gameCreation desc
            """)
    List<String> findMatchIdsByPuuidAndQueueId(@Param("puuid") String puuid,
                                               @Param("queueId") int queueId,
                                               Pageable pageable);
    // 해당 큐에서 puuid가 참가한 매치 ID를 최신순으로. Pageable로 최신 1건만 뽑아 LP 스냅샷 앵커로 사용

    @Query("""
            select p.match.gameCreation
            from Participant p
            where p.puuid = :puuid
            and p.match.matchId = :matchId
            """)
    Long findGameCreationByPuuidAndMatchId(@Param("puuid") String puuid,
                                           @Param("matchId") String matchId);
    // LP 스냅샷 앵커 매치의 게임 생성 시각 — 두 스냅샷 사이에 낀 랭크 게임 수를 셀 때 경계로 사용

    @Query("""
            select count(p)
            from Participant p
            where p.puuid = :puuid
            and p.match.queueId = :queueId
            and p.match.gameCreation > :afterGameCreation
            and p.match.gameCreation <= :untilGameCreation
            """)
    long countRankedMatchesInWindow(@Param("puuid") String puuid,
                                    @Param("queueId") int queueId,
                                    @Param("afterGameCreation") long afterGameCreation,
                                    @Param("untilGameCreation") long untilGameCreation);
    // 직전 앵커(배타) ~ 현재 앵커(포함) 구간의 랭크 게임 수. 정확히 1이면 LP 증감을 그 한 판에 귀속할 수 있다

    // 챔피언 상세 페이지용: 해당 챔피언의 모든 참가 행(룬/스킬/아이템/스펠 집계는 서비스에서 Java로 수행).
    // queueId 가 null 이면 전체 큐, 아니면 해당 큐만.
    @Query("""
            select p
            from Participant p
            join fetch p.match m
            where p.championId = :championId
            and (:queueId is null or m.queueId = :queueId)
            """)
    List<Participant> findByChampionForStats(@Param("championId") int championId,
                                             @Param("queueId") Integer queueId);
}
