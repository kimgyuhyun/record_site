package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QPendingSummonerRefresh is a Querydsl query type for PendingSummonerRefresh
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPendingSummonerRefresh extends EntityPathBase<PendingSummonerRefresh> {

    private static final long serialVersionUID = -2036359618L;

    public static final QPendingSummonerRefresh pendingSummonerRefresh = new QPendingSummonerRefresh("pendingSummonerRefresh");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> depth = createNumber("depth", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> processedAt = createDateTime("processedAt", java.time.LocalDateTime.class);

    public final StringPath puuid = createString("puuid");

    public final EnumPath<CrawlStatus> status = createEnum("status", CrawlStatus.class);

    public QPendingSummonerRefresh(String variable) {
        super(PendingSummonerRefresh.class, forVariable(variable));
    }

    public QPendingSummonerRefresh(Path<? extends PendingSummonerRefresh> path) {
        super(path.getType(), path.getMetadata());
    }

    public QPendingSummonerRefresh(PathMetadata metadata) {
        super(PendingSummonerRefresh.class, metadata);
    }

}

