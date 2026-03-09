package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "champion_spell")
@Getter
@Setter
@NoArgsConstructor
public class ChampionSpell {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 다대일
    @JoinColumn(name = "champion_id")
    private Champion champion;

    @Column(nullable = true)
    private String spellId; // AhriQ

    @Column(nullable = false)
    private String name; // 현혹의 구슬

    @Column(nullable = false)
    private int slotIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 스킬 설명

    @Column(nullable = true)
    private Integer maxRank; // 최대 스킬 레벨 / 패시브는 스킬레벨이 null이라 Integer(래퍼타입)으로 바꿔줌

    @Column(nullable = true)
    private String cooldownBurn; // 쿨타임 배열을 문자열 하나로 요약한것 db에는 Burn만 저장하면됨

    @Column(nullable = true)
    private String costBurn; // 소모값 배열을 문자열 하나로 요약

    @Column(nullable = true)
    private String rangeBurn; // 사거리

    @Column(nullable = false)
    private String imageUrl; // 스킬 아이콘
    
    
}
