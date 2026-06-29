package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotSummonerResponse;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "summoner")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Summoner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "summoner_Id", nullable = true, unique = true)
    private String summonerId; // 랭크용 (deprecated 예정)

    @Column(nullable = false, unique = true)
    private String puuid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int profileIconId;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private String tagLine;

    @Column(nullable = false)
    private long revisionDate;

    @OneToMany(mappedBy = "summoner", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Participant> participantList = new ArrayList<>();

    // 솔로랭크
    @Column private String soloTier;
    @Column private String soloRank;
    @Column private Integer soloLp;
    @Column private Integer soloWins;
    @Column private Integer soloLosses;

    // 자유랭크
    @Column private String flexTier;
    @Column private String flexRank;
    @Column private Integer flexLp;
    @Column private Integer flexWins;
    @Column private Integer flexLosses;

    // 마지막 랭크 갱신 시각
    @Column private LocalDateTime rankUpdatedAt;

    // 백그라운드 LP 폴링 추적 만료 시각. 프로필 조회 시 연장되며, 이 시각이 미래인 동안만 폴러가 주기적으로 갱신한다.
    @Column private LocalDateTime trackedUntil;

    public static Summoner from(RiotSummonerResponse res) {
        return Summoner.builder()
                .puuid(res.getPuuid())
                .profileIconId(res.getProfileIconId())
                .level(res.getSummonerLevel())
                .revisionDate(res.getRevisionDate())
                .build();
    }

    public void updateSoloRank(String tier, String rank,
                               int lp, int wins, int losses) {
        this.soloTier = tier;
        this.soloRank = rank;
        this.soloLp = lp;
        this.soloWins = wins;
        this.soloLosses = losses;
    }

    public void updateFlexRank(String tier, String rank,
                               int lp, int wins, int losses) {
        this.flexTier = tier;
        this.flexRank = rank;
        this.flexLp = lp;
        this.flexWins = wins;
        this.flexLosses = losses;
    }

    public void stampRankUpdateAt() {
        this.rankUpdatedAt = LocalDateTime.now();
    }

    // 프로필을 본 사용자의 관심 표시 — 지금부터 window 기간만큼 폴링 추적을 연장한다(폴링 자체는 호출하지 않는다).
    public void extendTracking(java.time.Duration window) {
        this.trackedUntil = LocalDateTime.now().plus(window);
    }
}
