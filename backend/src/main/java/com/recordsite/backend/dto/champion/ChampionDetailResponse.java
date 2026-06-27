package com.recordsite.backend.dto.champion;

import java.util.List;

// 챔피언 상세 페이지(초상화 클릭) 응답. 우리가 수집한 매치 DB를 챔피언 단위로 집계한 결과.
// 정적 데이터(스킬 설명/스킨 이미지)는 프론트가 DDragon 에서 직접 받으므로 여기 포함하지 않는다.
public record ChampionDetailResponse(
        int championId,
        String championName,
        String primaryPosition,   // 가장 많이 플레이된 포지션(TOP/JUNGLE/MIDDLE/BOTTOM/UTILITY)
        long games,
        long wins,
        double winRate,           // 0~1
        double pickRate,          // 0~1 (같은 큐 전체 매치 대비)
        double banRate,           // 0~1
        List<RuneBuild> runes,
        List<SkillOrder> skillOrders,
        List<ItemBuild> startingItems, // 시작 아이템 조합(게임 시작 ~90초 내 구매, 장신구 제외)
        List<ItemBuild> boots,         // 신발 선택(items 는 신발 1개)
        List<ItemBuild> coreItems,     // 핵심 빌드 1코어>2코어>3코어 순서
        List<SpellPair> spells,
        List<Expert> experts      // 우리 DB 내 해당 챔피언 다판 유저(=장인) 랭킹
) {
    // 룬 페이지 한 벌(주룬 계열/키스톤+3, 보조 계열/2, 스탯 파편 3). id 는 DDragon 아이콘 매핑용.
    public record RuneBuild(
            Integer primaryStyleId, Integer keystoneId,
            Integer primaryRune1, Integer primaryRune2, Integer primaryRune3,
            Integer subStyleId, Integer subRune1, Integer subRune2,
            Integer statOffense, Integer statFlex, Integer statDefense,
            long games, long wins, double winRate) {}

    // 선마 순서. order 예: "Q>E>W" (R 제외, 5포인트 도달이 빠른 순).
    public record SkillOrder(String order, long games, long wins, double winRate) {}

    // 핵심 아이템 빌드(구매 순서 앞쪽 코어 아이템들). 소비/장신구 제외.
    public record ItemBuild(List<Integer> items, long games, long wins, double winRate) {}

    // 소환사 주문 조합(순서 무관 정규화).
    public record SpellPair(int spell1, int spell2, long games, long wins, double winRate) {}

    // 해당 챔피언 다판 유저.
    public record Expert(String puuid, String gameName, String tagLine, long games, long wins, double winRate) {}
}
