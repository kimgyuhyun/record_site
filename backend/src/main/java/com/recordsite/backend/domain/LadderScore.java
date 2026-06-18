package com.recordsite.backend.domain;

// 티어/디비전/LP를 하나의 절대 사다리 점수로 환산한다.
// 디비전·티어 경계를 넘는 LP 증감(승급/강등)도 두 점수의 단순 뺄셈으로 구하기 위함.
// 예) GOLD I 90 -> PLATINUM IV 15 : 1590 -> 1615 이므로 +25.
public final class LadderScore {

    private LadderScore() {
    }

    // 한 티어 = 400점 (디비전 4칸 × 100). 마스터+는 디비전 없는 공통 LP 풀이라 같은 기준점에서 LP가 계속 누적된다.
    private static int tierBase(String tier) {
        return switch (tier.toUpperCase()) {
            case "IRON" -> 0;
            case "BRONZE" -> 400;
            case "SILVER" -> 800;
            case "GOLD" -> 1200;
            case "PLATINUM" -> 1600;
            case "EMERALD" -> 2000;
            case "DIAMOND" -> 2400;
            case "MASTER", "GRANDMASTER", "CHALLENGER" -> 2800;
            default -> throw new IllegalArgumentException("알 수 없는 티어: " + tier);
        };
    }

    // 디비전 오프셋: IV(0) < III(100) < II(200) < I(300). 마스터+는 디비전이 없어 0.
    private static int divisionOffset(String division) {
        if (division == null) {
            return 0;
        }
        return switch (division.toUpperCase()) {
            case "IV" -> 0;
            case "III" -> 100;
            case "II" -> 200;
            case "I" -> 300;
            default -> throw new IllegalArgumentException("알 수 없는 디비전: " + division);
        };
    }

    public static int of(String tier, String division, int leaguePoints) {
        return tierBase(tier) + divisionOffset(division) + leaguePoints;
    }
}
