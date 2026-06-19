package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLadderEntry is a Querydsl query type for LadderEntry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLadderEntry extends EntityPathBase<LadderEntry> {

    private static final long serialVersionUID = -488116408L;

    public static final QLadderEntry ladderEntry = new QLadderEntry("ladderEntry");

    public final StringPath gameName = createString("gameName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> leaguePoints = createNumber("leaguePoints", Integer.class);

    public final NumberPath<Integer> losses = createNumber("losses", Integer.class);

    public final StringPath puuid = createString("puuid");

    public final EnumPath<QueueType> queueType = createEnum("queueType", QueueType.class);

    public final NumberPath<Integer> rankPosition = createNumber("rankPosition", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> refreshedAt = createDateTime("refreshedAt", java.time.LocalDateTime.class);

    public final StringPath tagLine = createString("tagLine");

    public final EnumPath<ApexTier> tier = createEnum("tier", ApexTier.class);

    public final NumberPath<Integer> wins = createNumber("wins", Integer.class);

    public QLadderEntry(String variable) {
        super(LadderEntry.class, forVariable(variable));
    }

    public QLadderEntry(Path<? extends LadderEntry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLadderEntry(PathMetadata metadata) {
        super(LadderEntry.class, metadata);
    }

}

