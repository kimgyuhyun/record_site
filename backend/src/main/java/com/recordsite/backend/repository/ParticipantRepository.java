package com.recordsite.backend.repository;

import com.recordsite.backend.entity.Match;
import com.recordsite.backend.entity.Participant;
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
