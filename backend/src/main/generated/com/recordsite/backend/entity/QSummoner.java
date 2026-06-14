package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSummoner is a Querydsl query type for Summoner
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSummoner extends EntityPathBase<Summoner> {

    private static final long serialVersionUID = -1047113098L;

    public static final QSummoner summoner = new QSummoner("summoner");

    public final NumberPath<Integer> flexLosses = createNumber("flexLosses", Integer.class);

    public final NumberPath<Integer> flexLp = createNumber("flexLp", Integer.class);

    public final StringPath flexRank = createString("flexRank");

    public final StringPath flexTier = createString("flexTier");

    public final NumberPath<Integer> flexWins = createNumber("flexWins", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> level = createNumber("level", Integer.class);

    public final StringPath name = createString("name");

    public final ListPath<Participant, QParticipant> participantList = this.<Participant, QParticipant>createList("participantList", Participant.class, QParticipant.class, PathInits.DIRECT2);

    public final NumberPath<Integer> profileIconId = createNumber("profileIconId", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final DateTimePath<java.time.LocalDateTime> rankUpdatedAt = createDateTime("rankUpdatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> revisionDate = createNumber("revisionDate", Long.class);

    public final NumberPath<Integer> soloLosses = createNumber("soloLosses", Integer.class);

    public final NumberPath<Integer> soloLp = createNumber("soloLp", Integer.class);

    public final StringPath soloRank = createString("soloRank");

    public final StringPath soloTier = createString("soloTier");

    public final NumberPath<Integer> soloWins = createNumber("soloWins", Integer.class);

    public final StringPath summonerId = createString("summonerId");

    public final StringPath tagLine = createString("tagLine");

    public QSummoner(String variable) {
        super(Summoner.class, forVariable(variable));
    }

    public QSummoner(Path<? extends Summoner> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSummoner(PathMetadata metadata) {
        super(Summoner.class, metadata);
    }

}

