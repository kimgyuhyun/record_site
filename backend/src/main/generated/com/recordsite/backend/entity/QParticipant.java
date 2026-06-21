package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QParticipant is a Querydsl query type for Participant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QParticipant extends EntityPathBase<Participant> {

    private static final long serialVersionUID = -1941453557L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QParticipant participant = new QParticipant("participant");

    public final NumberPath<Integer> assists = createNumber("assists", Integer.class);

    public final NumberPath<Integer> championId = createNumber("championId", Integer.class);

    public final NumberPath<Integer> championLevel = createNumber("championLevel", Integer.class);

    public final StringPath championName = createString("championName");

    public final NumberPath<Integer> deaths = createNumber("deaths", Integer.class);

    public final BooleanPath gameEndedInEarlySurrender = createBoolean("gameEndedInEarlySurrender");

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Integer> goldEarned = createNumber("goldEarned", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath individualPosition = createString("individualPosition");

    public final NumberPath<Integer> item0 = createNumber("item0", Integer.class);

    public final NumberPath<Integer> item1 = createNumber("item1", Integer.class);

    public final NumberPath<Integer> item2 = createNumber("item2", Integer.class);

    public final NumberPath<Integer> item3 = createNumber("item3", Integer.class);

    public final NumberPath<Integer> item4 = createNumber("item4", Integer.class);

    public final NumberPath<Integer> item5 = createNumber("item5", Integer.class);

    public final NumberPath<Integer> item6 = createNumber("item6", Integer.class);

    public final NumberPath<Integer> keystoneId = createNumber("keystoneId", Integer.class);

    public final NumberPath<Integer> kills = createNumber("kills", Integer.class);

    public final QMatch match;

    public final NumberPath<Integer> neutralMinionsKilled = createNumber("neutralMinionsKilled", Integer.class);

    public final NumberPath<Integer> participantId = createNumber("participantId", Integer.class);

    public final NumberPath<Integer> placement = createNumber("placement", Integer.class);

    public final NumberPath<Integer> playerSubteamId = createNumber("playerSubteamId", Integer.class);

    public final NumberPath<Integer> primaryStyleId = createNumber("primaryStyleId", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final NumberPath<Integer> spell1 = createNumber("spell1", Integer.class);

    public final NumberPath<Integer> spell2 = createNumber("spell2", Integer.class);

    public final NumberPath<Integer> statPerkDefense = createNumber("statPerkDefense", Integer.class);

    public final NumberPath<Integer> statPerkFlex = createNumber("statPerkFlex", Integer.class);

    public final NumberPath<Integer> statPerkOffense = createNumber("statPerkOffense", Integer.class);

    public final NumberPath<Integer> subStyleId = createNumber("subStyleId", Integer.class);

    public final NumberPath<Integer> subteamPlacement = createNumber("subteamPlacement", Integer.class);

    public final QSummoner summoner;

    public final StringPath tagLine = createString("tagLine");

    public final BooleanPath teamEarlySurrendered = createBoolean("teamEarlySurrendered");

    public final NumberPath<Integer> teamId = createNumber("teamId", Integer.class);

    public final NumberPath<Integer> teamKills = createNumber("teamKills", Integer.class);

    public final StringPath teamPosition = createString("teamPosition");

    public final NumberPath<Long> totalDamageDealt = createNumber("totalDamageDealt", Long.class);

    public final NumberPath<Long> totalDamageDealtToChampions = createNumber("totalDamageDealtToChampions", Long.class);

    public final NumberPath<Long> totalDamageTaken = createNumber("totalDamageTaken", Long.class);

    public final NumberPath<Integer> totalMinionsKilled = createNumber("totalMinionsKilled", Integer.class);

    public final NumberPath<Integer> visionScore = createNumber("visionScore", Integer.class);

    public final BooleanPath win = createBoolean("win");

    public QParticipant(String variable) {
        this(Participant.class, forVariable(variable), INITS);
    }

    public QParticipant(Path<? extends Participant> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QParticipant(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QParticipant(PathMetadata metadata, PathInits inits) {
        this(Participant.class, metadata, inits);
    }

    public QParticipant(Class<? extends Participant> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QMatch(forProperty("match")) : null;
        this.summoner = inits.isInitialized("summoner") ? new QSummoner(forProperty("summoner")) : null;
    }

}

