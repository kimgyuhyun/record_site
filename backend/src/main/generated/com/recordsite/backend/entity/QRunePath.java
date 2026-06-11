package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRunePath is a Querydsl query type for RunePath
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRunePath extends EntityPathBase<RunePath> {

    private static final long serialVersionUID = 1525349287L;

    public static final QRunePath runePath = new QRunePath("runePath");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath image = createString("image");

    public final NumberPath<Integer> pathKey = createNumber("pathKey", Integer.class);

    public final StringPath runePathNameEn = createString("runePathNameEn");

    public final StringPath runePathNameKor = createString("runePathNameKor");

    public final ListPath<Rune, QRune> runes = this.<Rune, QRune>createList("runes", Rune.class, QRune.class, PathInits.DIRECT2);

    public QRunePath(String variable) {
        super(RunePath.class, forVariable(variable));
    }

    public QRunePath(Path<? extends RunePath> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRunePath(PathMetadata metadata) {
        super(RunePath.class, metadata);
    }

}

