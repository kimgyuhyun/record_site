package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "champion_skin")
@Getter
@Setter
@NoArgsConstructor
public class ChampionSkin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_id")
    private Champion champion; // 챔피언 ID

    @Column(nullable = false)
    private String skinId; // 스킨 고유 ID

    @Column(nullable = false)
    private int num; // 스킨 번호

    @Column(nullable = false)
    private String name; // 스킨 이름

    @Column(nullable = false)
    private boolean chromas;
}
