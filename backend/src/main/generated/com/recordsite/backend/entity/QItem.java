package com.recordsite.backend.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QItem is a Querydsl query type for Item
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItem extends EntityPathBase<Item> {

    private static final long serialVersionUID = 856557627L;

    public static final QItem item = new QItem("item");

    public final StringPath buildsInto = createString("buildsInto");

    public final StringPath description = createString("description");

    public final NumberPath<Integer> goldBase = createNumber("goldBase", Integer.class);

    public final NumberPath<Integer> goldSell = createNumber("goldSell", Integer.class);

    public final NumberPath<Integer> goldTotal = createNumber("goldTotal", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath image = createString("image");

    public final StringPath itemKey = createString("itemKey");

    public final StringPath itemName = createString("itemName");

    public final StringPath plaintext = createString("plaintext");

    public final BooleanPath purchasable = createBoolean("purchasable");

    public final StringPath tags = createString("tags");

    public QItem(String variable) {
        super(Item.class, forVariable(variable));
    }

    public QItem(Path<? extends Item> path) {
        super(path.getType(), path.getMetadata());
    }

    public QItem(PathMetadata metadata) {
        super(Item.class, metadata);
    }

}

