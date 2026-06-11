package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRune is a Querydsl query type for Rune
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRune extends EntityPathBase<Rune> {

    private static final long serialVersionUID = 856826978L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRune rune = new QRune("rune");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath image = createString("image");

    public final StringPath longDesc = createString("longDesc");

    public final QRunePath path;

    public final NumberPath<Integer> runeKey = createNumber("runeKey", Integer.class);

    public final StringPath runeNameEn = createString("runeNameEn");

    public final StringPath runeNameKor = createString("runeNameKor");

    public final StringPath shortDesc = createString("shortDesc");

    public QRune(String variable) {
        this(Rune.class, forVariable(variable), INITS);
    }

    public QRune(Path<? extends Rune> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRune(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRune(PathMetadata metadata, PathInits inits) {
        this(Rune.class, metadata, inits);
    }

    public QRune(Class<? extends Rune> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.path = inits.isInitialized("path") ? new QRunePath(forProperty("path")) : null;
    }

}

