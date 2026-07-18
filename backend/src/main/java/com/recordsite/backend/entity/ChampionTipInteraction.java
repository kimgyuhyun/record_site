package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 누가 어떤 팁에 추천/신고를 했는지의 이력. 비로그인 게시판이라 "누구"는 계정이 아니라
// 클라이언트 IP 를 솔트와 함께 해시한 actor_key 로 식별한다(원본 IP 는 저장하지 않는다).
// (tip_id, actor_key, interaction_type) 유니크 제약이 1인 1회를 DB 에서 보장한다.
@Entity
@Table(
        name = "champion_tip_interaction",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_champion_tip_interaction",
                columnNames = {"tip_id", "actor_key", "interaction_type"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChampionTipInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tip_id", nullable = false)
    private Long tipId;

    // SHA-256 HMAC 을 hex 로 담는다(64자 고정).
    @Column(name = "actor_key", nullable = false, length = 64)
    private String actorKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    private TipInteractionType interactionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private ChampionTipInteraction(Long tipId, String actorKey, TipInteractionType interactionType) {
        this.tipId = tipId;
        this.actorKey = actorKey;
        this.interactionType = interactionType;
        this.createdAt = LocalDateTime.now();
    }

    public static ChampionTipInteraction of(Long tipId, String actorKey, TipInteractionType interactionType) {
        return new ChampionTipInteraction(tipId, actorKey, interactionType);
    }
}
