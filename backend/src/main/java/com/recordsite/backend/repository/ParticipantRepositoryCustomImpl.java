package com.recordsite.backend.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.entity.QMatch;
import com.recordsite.backend.entity.QParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ParticipantRepositoryCustomImpl implements ParticipantRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<MatchRecordDto> findMatchRecordByPuuid(String puuid, Pageable pageable) {
        QParticipant p = QParticipant.participant;
        QMatch m = QMatch.match;

        List<MatchRecordDto> content = queryFactory
                .select(Projections.constructor(MatchRecordDto.class,
                        m.matchId,
                        m.gameCreation,
                        m.gameDuration,
                        m.queueId,
                        m.mapId,
                        m.gameMode,
                        m.gameType,
                        p.puuid,
                        p.gameName,
                        p.tagLine,
                        p.teamId,
                        p.win,
                        p.championId,
                        p.championName,
                        p.championLevel,
                        p.kills,
                        p.deaths,
                        p.assists,
                        p.goldEarned,
                        p.totalDamageDealt,
                        p.totalDamageDealtToChampions,
                        p.totalDamageTaken,
                        p.visionScore,
                        p.item0,
                        p.item1,
                        p.item2,
                        p.item3,
                        p.item4,
                        p.item5,
                        p.item6,
                        p.spell1,
                        p.spell2,
                        p.statPerkOffense,
                        p.statPerkFlex,
                        p.statPerkDefense,
                        p.primaryStyleId,
                        p.keystoneId,
                        p.subStyleId,
                        p.gameEndedInEarlySurrender,
                        p.teamEarlySurrendered,
                        p.totalMinionsKilled,
                        p.neutralMinionsKilled,
                        p.teamKills
                ))
                .from(p)
                .join(p.match, m)
                .where(p.puuid.eq(puuid))
                .orderBy(m.gameCreation.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(p.count())
                .from(p)
                .where(p.puuid.eq(puuid))
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0);
    }
}
