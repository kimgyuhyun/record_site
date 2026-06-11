package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChampionSpell is a Querydsl query type for ChampionSpell
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChampionSpell extends EntityPathBase<ChampionSpell> {

    private static final long serialVersionUID = -674617833L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChampionSpell championSpell = new QChampionSpell("championSpell");

    public final QChampion champion;

    public final StringPath cooldownBurn = createString("cooldownBurn");

    public final StringPath costBurn = createString("costBurn");

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Integer> maxRank = createNumber("maxRank", Integer.class);

    public final StringPath name = createString("name");

    public final StringPath rangeBurn = createString("rangeBurn");

    public final NumberPath<Integer> slotIndex = createNumber("slotIndex", Integer.class);

    public final StringPath spellId = createString("spellId");

    public QChampionSpell(String variable) {
        this(ChampionSpell.class, forVariable(variable), INITS);
    }

    public QChampionSpell(Path<? extends ChampionSpell> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChampionSpell(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChampionSpell(PathMetadata metadata, PathInits inits) {
        this(ChampionSpell.class, metadata, inits);
    }

    public QChampionSpell(Class<? extends ChampionSpell> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.champion = inits.isInitialized("champion") ? new QChampion(forProperty("champion"), inits.get("champion")) : null;
    }

}

