package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMatchBan is a Querydsl query type for MatchBan
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMatchBan extends EntityPathBase<MatchBan> {

    private static final long serialVersionUID = 1001640242L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMatchBan matchBan = new QMatchBan("matchBan");

    public final NumberPath<Integer> championId = createNumber("championId", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMatch match;

    public final NumberPath<Integer> pickTurn = createNumber("pickTurn", Integer.class);

    public final NumberPath<Integer> teamId = createNumber("teamId", Integer.class);

    public QMatchBan(String variable) {
        this(MatchBan.class, forVariable(variable), INITS);
    }

    public QMatchBan(Path<? extends MatchBan> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMatchBan(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMatchBan(PathMetadata metadata, PathInits inits) {
        this(MatchBan.class, metadata, inits);
    }

    public QMatchBan(Class<? extends MatchBan> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.match = inits.isInitialized("match") ? new QMatch(forProperty("match")) : null;
    }

}

