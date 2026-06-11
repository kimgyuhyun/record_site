package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChampionStats is a Querydsl query type for ChampionStats
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChampionStats extends EntityPathBase<ChampionStats> {

    private static final long serialVersionUID = -674502258L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChampionStats championStats = new QChampionStats("championStats");

    public final NumberPath<Double> armor = createNumber("armor", Double.class);

    public final NumberPath<Double> armorPerLevel = createNumber("armorPerLevel", Double.class);

    public final NumberPath<Double> attackDamage = createNumber("attackDamage", Double.class);

    public final NumberPath<Double> attackDamagePerLevel = createNumber("attackDamagePerLevel", Double.class);

    public final NumberPath<Double> attackRange = createNumber("attackRange", Double.class);

    public final NumberPath<Double> attackSpeed = createNumber("attackSpeed", Double.class);

    public final NumberPath<Double> attackSpeedPerLevel = createNumber("attackSpeedPerLevel", Double.class);

    public final QChampion champion;

    public final NumberPath<Double> crit = createNumber("crit", Double.class);

    public final NumberPath<Double> critPerLevel = createNumber("critPerLevel", Double.class);

    public final NumberPath<Double> hp = createNumber("hp", Double.class);

    public final NumberPath<Double> hpPerLevel = createNumber("hpPerLevel", Double.class);

    public final NumberPath<Double> hpRegen = createNumber("hpRegen", Double.class);

    public final NumberPath<Double> hpRegenPerLevel = createNumber("hpRegenPerLevel", Double.class);

    public final NumberPath<Double> moveSpeed = createNumber("moveSpeed", Double.class);

    public final NumberPath<Double> mp = createNumber("mp", Double.class);

    public final NumberPath<Double> mpPerLevel = createNumber("mpPerLevel", Double.class);

    public final NumberPath<Double> mpRegen = createNumber("mpRegen", Double.class);

    public final NumberPath<Double> mpRegenPerLevel = createNumber("mpRegenPerLevel", Double.class);

    public final NumberPath<Double> spellBlock = createNumber("spellBlock", Double.class);

    public final NumberPath<Double> spellBlockPerLevel = createNumber("spellBlockPerLevel", Double.class);

    public final NumberPath<Long> statId = createNumber("statId", Long.class);

    public QChampionStats(String variable) {
        this(ChampionStats.class, forVariable(variable), INITS);
    }

    public QChampionStats(Path<? extends ChampionStats> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChampionStats(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChampionStats(PathMetadata metadata, PathInits inits) {
        this(ChampionStats.class, metadata, inits);
    }

    public QChampionStats(Class<? extends ChampionStats> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.champion = inits.isInitialized("champion") ? new QChampion(forProperty("champion"), inits.get("champion")) : null;
    }

}

