package com.recordsite.backend.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LadderScoreTest {

    @Test
    @DisplayName("같은 디비전 안에서는 LP 차이가 그대로 증감이 된다")
    void sameDivisionDelta() {
        int before = LadderScore.of("GOLD", "IV", 30);
        int after = LadderScore.of("GOLD", "IV", 55);

        assertEquals(25, after - before);
    }

    @Test
    @DisplayName("디비전 승급(GOLD I 90 -> PLATINUM IV 15)도 단순 뺄셈으로 +25가 나온다")
    void promotionAcrossDivision() {
        int before = LadderScore.of("GOLD", "I", 90);      // 1200 + 300 + 90 = 1590
        int after = LadderScore.of("PLATINUM", "IV", 15);  // 1600 + 0 + 15 = 1615

        assertEquals(25, after - before);
    }

    @Test
    @DisplayName("마스터+는 디비전 없이 LP가 누적되어 음수 증감도 그대로 계산된다")
    void masterLpContinues() {
        int before = LadderScore.of("MASTER", null, 120);
        int after = LadderScore.of("MASTER", null, 98);

        assertEquals(-22, after - before);
    }

    @Test
    @DisplayName("강등(SILVER IV 5 -> BRONZE I 80)도 음수 증감으로 계산된다")
    void demotionAcrossTier() {
        int before = LadderScore.of("SILVER", "IV", 5);  // 800 + 0 + 5 = 805
        int after = LadderScore.of("BRONZE", "I", 80);   // 400 + 300 + 80 = 780

        assertEquals(-25, after - before);
    }
}
