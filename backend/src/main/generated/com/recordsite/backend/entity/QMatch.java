package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatch is a Querydsl query type for Match
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatch extends EntityPathBase<Match> {

    private static final long serialVersionUID = 786624925L;

    public static final QMatch match = new QMatch("match");

    public final ListPath<MatchBan, QMatchBan> banList = this.<MatchBan, QMatchBan>createList("banList", MatchBan.class, QMatchBan.class, PathInits.DIRECT2);

    public final NumberPath<Integer> blueBaronKills = createNumber("blueBaronKills", Integer.class);

    public final NumberPath<Integer> blueDragonKills = createNumber("blueDragonKills", Integer.class);

    public final NumberPath<Integer> blueInhibitorKills = createNumber("blueInhibitorKills", Integer.class);

    public final NumberPath<Integer> blueRiftHeraldKills = createNumber("blueRiftHeraldKills", Integer.class);

    public final NumberPath<Integer> blueTowerKills = createNumber("blueTowerKills", Integer.class);

    public final NumberPath<Long> gameCreation = createNumber("gameCreation", Long.class);

    public final NumberPath<Long> gameDuration = createNumber("gameDuration", Long.class);

    public final StringPath gameMode = createString("gameMode");

    public final StringPath gameType = createString("gameType");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> mapId = createNumber("mapId", Integer.class);

    public final StringPath matchId = createString("matchId");

    public final ListPath<Participant, QParticipant> participantList = this.<Participant, QParticipant>createList("participantList", Participant.class, QParticipant.class, PathInits.DIRECT2);

    public final StringPath platformId = createString("platformId");

    public final NumberPath<Integer> queueId = createNumber("queueId", Integer.class);

    public final NumberPath<Integer> redBaronKills = createNumber("redBaronKills", Integer.class);

    public final NumberPath<Integer> redDragonKills = createNumber("redDragonKills", Integer.class);

    public final NumberPath<Integer> redInhibitorKills = createNumber("redInhibitorKills", Integer.class);

    public final NumberPath<Integer> redRiftHeraldKills = createNumber("redRiftHeraldKills", Integer.class);

    public final NumberPath<Integer> redTowerKills = createNumber("redTowerKills", Integer.class);

    public QMatch(String variable) {
        super(Match.class, forVariable(variable));
    }

    public QMatch(Path<? extends Match> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMatch(PathMetadata metadata) {
        super(Match.class, metadata);
    }

}

