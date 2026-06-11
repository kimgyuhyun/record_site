package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChampion is a Querydsl query type for Champion
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChampion extends EntityPathBase<Champion> {

    private static final long serialVersionUID = 2136522129L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChampion champion = new QChampion("champion");

    public final StringPath blurb = createString("blurb");

    public final StringPath championId = createString("championId");

    public final NumberPath<Integer> championKey = createNumber("championKey", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Integer> infoAttack = createNumber("infoAttack", Integer.class);

    public final NumberPath<Integer> infoDefense = createNumber("infoDefense", Integer.class);

    public final NumberPath<Integer> infoDifficulty = createNumber("infoDifficulty", Integer.class);

    public final NumberPath<Integer> infoMagic = createNumber("infoMagic", Integer.class);

    public final StringPath lore = createString("lore");

    public final StringPath nameEn = createString("nameEn");

    public final StringPath nameKor = createString("nameKor");

    public final StringPath partype = createString("partype");

    public final ListPath<ChampionSkin, QChampionSkin> skins = this.<ChampionSkin, QChampionSkin>createList("skins", ChampionSkin.class, QChampionSkin.class, PathInits.DIRECT2);

    public final ListPath<ChampionSpell, QChampionSpell> spells = this.<ChampionSpell, QChampionSpell>createList("spells", ChampionSpell.class, QChampionSpell.class, PathInits.DIRECT2);

    public final QChampionStats stats;

    public final StringPath tags = createString("tags");

    public final StringPath title = createString("title");

    public QChampion(String variable) {
        this(Champion.class, forVariable(variable), INITS);
    }

    public QChampion(Path<? extends Champion> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChampion(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChampion(PathMetadata metadata, PathInits inits) {
        this(Champion.class, metadata, inits);
    }

    public QChampion(Class<? extends Champion> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.stats = inits.isInitialized("stats") ? new QChampionStats(forProperty("stats"), inits.get("stats")) : null;
    }

}

