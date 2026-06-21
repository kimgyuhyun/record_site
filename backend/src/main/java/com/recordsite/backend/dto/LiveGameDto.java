package com.recordsite.backend.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

// 인게임(실시간) 패널용 응답. Riot Spectator-V5 응답을 프론트가 쓰기 쉬운 형태로 가공한다.
@Getter
@Builder
public class LiveGameDto {

    private Long gameQueueConfigId; // 큐 ID (420 솔랭 등)
    private String gameMode;
    private Long gameLength;         // 현재까지 진행 시간(초)
    private Long gameStartTime;      // epoch ms (로딩 중이면 0)
    private Long mapId;

    private List<BannedChampion> bannedChampions;
    private List<LiveParticipant> participants;

    public static LiveGameDto from(RiotActiveGameResponse res) {
        List<BannedChampion> bans = res.getBannedChampions() == null ? List.of()
                : res.getBannedChampions().stream().map(BannedChampion::from).toList();
        List<LiveParticipant> players = res.getParticipants() == null ? List.of()
                : res.getParticipants().stream().map(LiveParticipant::from).toList();

        return LiveGameDto.builder()
                .gameQueueConfigId(res.getGameQueueConfigId())
                .gameMode(res.getGameMode())
                .gameLength(res.getGameLength())
                .gameStartTime(res.getGameStartTime())
                .mapId(res.getMapId())
                .bannedChampions(bans)
                .participants(players)
                .build();
    }

    @Getter
    @Builder
    public static class BannedChampion {
        private Long teamId;
        private Integer championId;

        static BannedChampion from(RiotActiveGameResponse.BannedChampion b) {
            return BannedChampion.builder()
                    .teamId(b.getTeamId())
                    .championId(b.getChampionId())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class LiveParticipant {
        private String puuid;
        private Long teamId;
        private Integer championId;
        private Integer spell1Id;
        private Integer spell2Id;
        private String gameName;        // riotId에서 분리
        private String tagLine;
        private Integer primaryStyleId; // 주 룬 계열 (perkStyle)
        private Integer subStyleId;     // 보조 룬 계열 (perkSubStyle)
        private Integer keystoneId;     // 핵심 룬 (perkIds[0])

        static LiveParticipant from(RiotActiveGameResponse.CurrentGameParticipant p) {
            RiotId riotId = RiotId.parse(p.getRiotId());
            RiotActiveGameResponse.Perks perks = p.getPerks();

            return LiveParticipant.builder()
                    .puuid(p.getPuuid())
                    .teamId(p.getTeamId())
                    .championId(p.getChampionId())
                    .spell1Id(p.getSpell1Id())
                    .spell2Id(p.getSpell2Id())
                    .gameName(riotId.gameName())
                    .tagLine(riotId.tagLine())
                    .primaryStyleId(perks == null ? null : toInt(perks.getPerkStyle()))
                    .subStyleId(perks == null ? null : toInt(perks.getPerkSubStyle()))
                    .keystoneId(perks == null ? null : firstPerk(perks))
                    .build();
        }

        private static Integer firstPerk(RiotActiveGameResponse.Perks perks) {
            if (perks.getPerkIds() == null || perks.getPerkIds().isEmpty()) return null;
            return toInt(perks.getPerkIds().get(0));
        }

        private static Integer toInt(Long v) {
            return v == null ? null : v.intValue();
        }
    }

    // "게임이름#태그" → (게임이름, 태그). 태그가 없으면 전체를 게임이름으로 둔다.
    private record RiotId(String gameName, String tagLine) {
        static RiotId parse(String riotId) {
            if (riotId == null || riotId.isBlank()) return new RiotId("", "");
            int idx = riotId.lastIndexOf('#');
            if (idx < 0) return new RiotId(riotId, "");
            return new RiotId(riotId.substring(0, idx), riotId.substring(idx + 1));
        }
    }
}
