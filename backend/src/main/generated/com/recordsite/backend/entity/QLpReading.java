package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLpReading is a Querydsl query type for LpReading
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLpReading extends EntityPathBase<LpReading> {

    private static final long serialVersionUID = -1187624896L;

    public static final QLpReading lpReading = new QLpReading("lpReading");

    public final StringPath division = createString("division");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> ladderScore = createNumber("ladderScore", Integer.class);

    public final NumberPath<Integer> leaguePoints = createNumber("leaguePoints", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final EnumPath<QueueType> queueType = createEnum("queueType", QueueType.class);

    public final NumberPath<Long> readAtEpochMs = createNumber("readAtEpochMs", Long.class);

    public final StringPath tier = createString("tier");

    public QLpReading(String variable) {
        super(LpReading.class, forVariable(variable));
    }

    public QLpReading(Path<? extends LpReading> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLpReading(PathMetadata metadata) {
        super(LpReading.class, metadata);
    }

}

