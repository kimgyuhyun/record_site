package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotMatchResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "matchId", nullable = false, unique = true)
    private String matchId; // 해당 판의 matchId (매치 고유 ID)

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL)
    private List<Participant> participantList = new ArrayList<>(); // 참가자 10명의 puuID List

    private Long gameCreation; // 게임 생성 시간

    private Long gameDuration; // 게임 길이

    private int queueId; // 큐 ID (솔랭, 자랭, 칼바람)

    private int mapId; // 맵 ID(소환사의 협곡);

    private String gameMode; // 게임 모드

    private String gameType; // 게임 타입

    private String platformId; // 플랫폼 ID


    public static Match from(RiotMatchResponse res) {
        Match match = new Match();
        match.setMatchId(res.getMetadata().getMatchId());
        match.setGameCreation(res.getInfo().getGameCreation());
        match.setGameDuration(res.getInfo().getGameDuration());
        match.setQueueId(res.getInfo().getQueueId());
        match.setMapId(res.getInfo().getMapId());
        match.setGameMode(res.getInfo().getGameMode());
        match.setGameType(res.getInfo().getGameType());

        String platformId = (res.getInfo() != null && res.getInfo().getPlatformId() != null)
                ? res.getInfo().getPlatformId() : "";
        match.setPlatformId(platformId);

        return match;
    }
}

// int, boolean, long 은 null 처리 불필요
// Integer, Long, String, 이나 중첩객체는 Null 처리 필요
