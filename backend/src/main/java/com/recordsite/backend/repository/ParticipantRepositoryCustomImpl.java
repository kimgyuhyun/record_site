package com.recordsite.backend.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.recordsite.backend.dto.ChampionPickCount;
import com.recordsite.backend.dto.ChampionPositionAggregate;
import com.recordsite.backend.dto.MatchRecordDto;
import com.recordsite.backend.dto.PlayedChampionAggregate;
import com.recordsite.backend.entity.QMatch;
import com.recordsite.backend.entity.QParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
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

    @Override
    public List<PlayedChampionAggregate> aggregatePlayedChampions(String puuid, Integer queueId) {
        QParticipant p = QParticipant.participant;
        QMatch m = QMatch.match;

        // 승리 1 / 패배 0 으로 환산해 합산 → 승리 수
        NumberExpression<Integer> winToInt = new CaseBuilder()
                .when(p.win.isTrue()).then(1).otherwise(0);
        // CS = 미니언 + 중립 몬스터
        NumberExpression<Integer> cs = p.totalMinionsKilled.add(p.neutralMinionsKilled);

        BooleanBuilder where = new BooleanBuilder().and(p.puuid.eq(puuid));
        if (queueId != null) {
            where.and(m.queueId.eq(queueId));
        }

        return queryFactory
                .select(Projections.constructor(PlayedChampionAggregate.class,
                        p.championId,
                        p.championName,
                        p.count(),
                        winToInt.sum().longValue(),
                        p.kills.avg(),
                        p.deaths.avg(),
                        p.assists.avg(),
                        cs.avg(),
                        p.goldEarned.avg()
                ))
                .from(p)
                .join(p.match, m)
                .where(where)
                .groupBy(p.championId, p.championName)
                .orderBy(p.count().desc())
                .fetch();
    }

    @Override
    public List<ChampionPositionAggregate> aggregateChampionStatsByPosition(Integer queueId) {
        QParticipant p = QParticipant.participant;
        QMatch m = QMatch.match;

        NumberExpression<Integer> winToInt = new CaseBuilder()
                .when(p.win.isTrue()).then(1).otherwise(0);

        // 포지션이 비어있는 행(리메이크/칼바람 등)은 티어 리스트 품질을 위해 제외
        BooleanBuilder where = new BooleanBuilder()
                .and(p.teamPosition.isNotNull())
                .and(p.teamPosition.ne(""));
        if (queueId != null) {
            where.and(m.queueId.eq(queueId));
        }

        return queryFactory
                .select(Projections.constructor(ChampionPositionAggregate.class,
                        p.championId,
                        p.championName,
                        p.teamPosition,
                        p.count(),
                        winToInt.sum().longValue()
                ))
                .from(p)
                .join(p.match, m)
                .where(where)
                .groupBy(p.championId, p.championName, p.teamPosition)
                .fetch();
    }

    @Override
    public List<ChampionPickCount> findChampionPickCounts(Collection<String> puuids) {
        if (puuids.isEmpty()) {
            return List.of();
        }
        QParticipant p = QParticipant.participant;

        return queryFactory
                .select(Projections.constructor(ChampionPickCount.class,
                        p.puuid,
                        p.championId,
                        p.count()
                ))
                .from(p)
                .where(p.puuid.in(puuids))
                .groupBy(p.puuid, p.championId)
                .fetch();
    }
}
