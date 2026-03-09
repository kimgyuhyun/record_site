package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "champion_stats")
@Getter
@Setter
@NoArgsConstructor // 인자가 없는 기본 생성자 자동 생성
public class ChampionStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long statId; // db 자체 id

    @OneToOne // 일대일 매핑
    @JoinColumn(name = "champion_id") // champion 엔티티에 기본키를 champion_id 외래키로 사용함
    private Champion champion;

    @Column(nullable = false)
    private double hp; // 기본 체력
    @Column(nullable = false)
    private double hpPerLevel; // 성장 체력

    @Column(nullable = false)
    private double mp;
    @Column(nullable = false)
    private double mpPerLevel;

    @Column(nullable = false)
    private double moveSpeed; // 이동속도

    @Column(nullable = false)
    private double armor; // 방어력
    @Column(nullable = false)
    private double armorPerLevel; // 성장 방어

    @Column(nullable = false)
    private double spellBlock; // 마법저항력
    @Column(nullable = false)
    private double spellBlockPerLevel; // 성장 마저

    @Column(nullable = false)
    private double attackRange; // 사거리

    @Column(nullable = false)
    private double hpRegen; // 체젠
    @Column(nullable = false)
    private double hpRegenPerLevel; // 성장 체젠

    @Column(nullable = false)
    private double mpRegen; // 마젠
    @Column(nullable = false)
    private double mpRegenPerLevel; // 성장 마젠

    @Column(nullable = false)
    private double crit; // 치명타
    @Column(nullable = false)
    private double critPerLevel; // 성장 치명타

    @Column(nullable = false)
    private double attackDamage; // 공격력
    @Column(nullable = false)
    private double attackDamagePerLevel; // 성장 공격력

    @Column(nullable = false)
    private double attackSpeed; // 공격속도
    @Column(nullable = false)
    private double attackSpeedPerLevel; // 성장 공속
}
