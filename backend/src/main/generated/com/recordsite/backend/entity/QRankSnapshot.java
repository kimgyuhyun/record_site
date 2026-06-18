package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRankSnapshot is a Querydsl query type for RankSnapshot
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRankSnapshot extends EntityPathBase<RankSnapshot> {

    private static final long serialVersionUID = 1332903736L;

    public static final QRankSnapshot rankSnapshot = new QRankSnapshot("rankSnapshot");

    public final StringPath anchorMatchId = createString("anchorMatchId");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath division = createString("division");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> ladderScore = createNumber("ladderScore", Integer.class);

    public final NumberPath<Integer> leaguePoints = createNumber("leaguePoints", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final EnumPath<QueueType> queueType = createEnum("queueType", QueueType.class);

    public final StringPath tier = createString("tier");

    public QRankSnapshot(String variable) {
        super(RankSnapshot.class, forVariable(variable));
    }

    public QRankSnapshot(Path<? extends RankSnapshot> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRankSnapshot(PathMetadata metadata) {
        super(RankSnapshot.class, metadata);
    }

}

