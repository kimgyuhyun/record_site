package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotSummonerResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "summoner")
@Getter
@Setter
@NoArgsConstructor
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

    public static Summoner from(RiotSummonerResponse res) {
        Summoner summoner = new Summoner();
        summoner.setPuuid(res.getPuuid());
        summoner.setProfileIconId(res.getProfileIconId());
        summoner.setLevel(res.getSummonerLevel());
        summoner.setRevisionDate(res.getRevisionDate());
        return summoner;
    }
}
