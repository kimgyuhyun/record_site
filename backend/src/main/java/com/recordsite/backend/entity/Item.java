package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "item")
@Getter
@Setter
@NoArgsConstructor
// jpa는 Map, List를 컬럼에 바로 못 넣음.
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String itemKey; // 아이템 고유번호

    @Column(nullable = false)
    private String itemName; // 아이템 이름

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 상세 설명

    @Column(nullable = true)
    private String plaintext; // 한 줄 요약

    @Column(nullable = true)
    private String buildsInto; // 이 아이템으로 만들 수 있는 상위 아이템

    @Column(nullable = false)
    private String image; // 아이템 이미지

    @Column(nullable = false)
    private int goldBase; // 이 아이템 자체의 가격
    @Column(nullable = false)
    private int goldTotal; // 재료값 다 합친 최종 가격
    @Column(nullable = false)
    private int goldSell; // 판매 가격
    @Column(nullable = false)
    private boolean purchasable; // 구매 가능 여부

    @Column(nullable = true)
    private String tags; // 태그


}
