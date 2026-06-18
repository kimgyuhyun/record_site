package com.recordsite.backend.entity;

import com.recordsite.backend.dto.RiotParticipantResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParticipantTest {

    // Participant.from()이 NPE 없이 동작하도록 최소 필드만 채운 응답
    private RiotParticipantResponse baseResponse() {
        RiotParticipantResponse res = new RiotParticipantResponse();
        res.setPuuid("puuid-1");
        res.setRiotIdGameName("name");
        res.setRiotIdTagline("KR1");
        res.setChampionName("Ahri");
        res.setTotalDamageDealt(0L);
        res.setTotalDamageDealtToChampions(0L);
        res.setTotalDamageTaken(0L);
        return res;
    }

    private RiotParticipantResponse.Style style(String description, int styleId, Integer keystone) {
        RiotParticipantResponse.Style style = new RiotParticipantResponse.Style();
        style.setDescription(description);
        style.setStyle(styleId);
        if (keystone != null) {
            RiotParticipantResponse.Selection selection = new RiotParticipantResponse.Selection();
            selection.setPerk(keystone);
            style.setSelections(List.of(selection));
        } else {
            style.setSelections(List.of());
        }
        return style;
    }

    @Test
    @DisplayName("perks.styles에서 핵심룬/주룬계열/보조룬계열을 추출한다")
    void from_extractsRunes() {
        RiotParticipantResponse res = baseResponse();
        RiotParticipantResponse.Perks perks = new RiotParticipantResponse.Perks();
        perks.setStyles(List.of(
                style("primaryStyle", 8100, 8112), // 지배 / 감전
                style("subStyle", 8000, null)      // 정밀
        ));
        res.setPerks(perks);

        Participant p = Participant.from(res, null);

        assertEquals(8100, p.getPrimaryStyleId());
        assertEquals(8112, p.getKeystoneId());
        assertEquals(8000, p.getSubStyleId());
    }

    @Test
    @DisplayName("perks가 없으면(봇전 등) 룬 필드는 null로 둔다")
    void from_nullPerks_runesNull() {
        RiotParticipantResponse res = baseResponse();
        res.setPerks(null);

        Participant p = Participant.from(res, null);

        assertNull(p.getPrimaryStyleId());
        assertNull(p.getKeystoneId());
        assertNull(p.getSubStyleId());
    }
}
