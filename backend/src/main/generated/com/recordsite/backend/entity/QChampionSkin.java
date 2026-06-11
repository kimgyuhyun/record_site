package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChampionSkin is a Querydsl query type for ChampionSkin
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChampionSkin extends EntityPathBase<ChampionSkin> {

    private static final long serialVersionUID = -2099976530L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChampionSkin championSkin = new QChampionSkin("championSkin");

    public final QChampion champion;

    public final BooleanPath chromas = createBoolean("chromas");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> num = createNumber("num", Integer.class);

    public final StringPath skinId = createString("skinId");

    public QChampionSkin(String variable) {
        this(ChampionSkin.class, forVariable(variable), INITS);
    }

    public QChampionSkin(Path<? extends ChampionSkin> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChampionSkin(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChampionSkin(PathMetadata metadata, PathInits inits) {
        this(ChampionSkin.class, metadata, inits);
    }

    public QChampionSkin(Class<? extends ChampionSkin> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.champion = inits.isInitialized("champion") ? new QChampion(forProperty("champion"), inits.get("champion")) : null;
    }

}

