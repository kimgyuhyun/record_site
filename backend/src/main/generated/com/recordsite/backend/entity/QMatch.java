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

