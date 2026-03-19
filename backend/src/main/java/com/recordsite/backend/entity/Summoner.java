package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotSummonerResponse;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String summonerId; // 랭크용

    @Column(nullable = false, unique = true)
    private String puuid; // 전역으로 사용가능한 소환사 고유 ID / 끝난 게임 매치 조회키

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

    public static Summoner from(RiotSummonerResponse res) {
        Summoner summoner = new Summoner();
        summoner.setPuuid(res.getPuuid());
        summoner.setProfileIconId(res.getProfileIconId());
        summoner.setLevel(res.getSummonerLevel());
        summoner.setRevisionDate(res.getRevisionDate());

        return summoner;
    }
}
