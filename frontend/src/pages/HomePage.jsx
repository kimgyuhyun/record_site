import React from 'react';
import useChampionMeta from '../hooks/useChampionMeta';
import PatchNotes from '../components/home/PatchNotes';
import ChampionRotation from '../components/home/ChampionRotation';
import ChampionSkinSale from '../components/home/ChampionSkinSale';
import HomeSidebar from '../components/home/HomeSidebar';

/*
 * 홈 랜딩.
 *  - 좌측: 주요 챔피언 / 패치노트 / 로테이션 / 세일
 *  - 우측: 배너 / 즐겨찾기 / 최근 검색
 *  - 챔피언 메타(id→key/이름)는 여기서 1회 로드해 좌측 컴포넌트에 전달(중복 fetch 방지).
 */
export default function HomePage() {
  const { championKeyById, championNameById } = useChampionMeta();

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: '20px 20px 48px' }}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 16 }}>

        {/* 좌측 메인 컬럼 */}
        <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: 16 }}>
          <PatchNotes />
          <ChampionRotation
            championKeyById={championKeyById}
            championNameById={championNameById} />
          <ChampionSkinSale
            championKeyById={championKeyById}
            championNameById={championNameById} />
        </div>

        {/* 우측 사이드바 */}
        <div style={{ width: 310, flexShrink: 0 }}>
          <HomeSidebar />
        </div>
      </div>
    </div>
  );
}
