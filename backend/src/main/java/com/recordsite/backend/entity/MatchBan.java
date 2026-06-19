package com.recordsite.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 한 매치에서 밴된 챔피언 한 칸. 밴율(밴된 매치 수 ÷ 전체 매치 수) 집계의 원천 데이터다.
//  - 한 매치당 챔피언은 최대 1번 밴되므로, 챔피언별 행 수 = 그 챔피언이 밴된 매치 수가 된다.
@Entity
@Table(
        name = "match_ban",
        indexes = @Index(name = "idx_match_ban_champion", columnList = "champion_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "champion_id", nullable = false)
    private int championId;

    @Column(name = "team_id", nullable = false)
    private int teamId;

    @Column(name = "pick_turn", nullable = false)
    private int pickTurn;

    private MatchBan(Match match, int championId, int teamId, int pickTurn) {
        this.match = match;
        this.championId = championId;
        this.teamId = teamId;
        this.pickTurn = pickTurn;
    }

    public static MatchBan of(Match match, int championId, int teamId, int pickTurn) {
        return new MatchBan(match, championId, teamId, pickTurn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchBan other)) return false;
        return id != null && id.equals(other.id); // 미저장(id=null) 상태는 동일성 비교 안 함
    }

    @Override
    public int hashCode() {
        return getClass().hashCode(); // 미저장 상태에서도 Set 오작동 방지를 위한 클래스 기반 고정값
    }
}
