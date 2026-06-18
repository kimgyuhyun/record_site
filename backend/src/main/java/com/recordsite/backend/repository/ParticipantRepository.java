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
}
