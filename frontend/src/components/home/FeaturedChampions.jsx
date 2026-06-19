import React from 'react';
import { imgChampion } from '../../constants/ddragon';

/*
 * 주요 챔피언 (큐레이션 티어 리스트).
 *  - 무료 메타 통계 API가 없으므로 측정 승률/픽률을 지어내지 않는다.
 *  - 챔피언 아이콘/한글명은 실제 Data Dragon 메타에서, 티어/포지션만 편집형으로 부여한다.
 *  - props.championKeyById / championNameById: HomePage에서 1회 로드해 전달.
 */

// 편집형 추천(이번 패치 기준). key 는 ddragon 영문 id.
const FEATURED = [
  { key: 'Ahri',      position: '미드',   tier: 'OP' },
  { key: 'Jinx',      position: '원딜',   tier: 'OP' },
  { key: 'Leona',     position: '서폿',   tier: '1' },
  { key: 'LeeSin',    position: '정글',   tier: '1' },
  { key: 'Garen',     position: '탑',     tier: '1' },
  { key: 'Yasuo',     position: '미드',   tier: '2' },
  { key: 'Thresh',    position: '서폿',   tier: '2' },
  { key: 'Kaisa',     position: '원딜',   tier: '2' },
];

const TIER_COLOR = {
  OP: { bg: '#3a1d2b', fg: '#ff6b9d', label: 'OP' },
  '1': { bg: '#1d2c3a', fg: '#5383e8', label: '1티어' },
  '2': { bg: '#26323f', fg: '#8fa3bd', label: '2티어' },
};

// ddragon key → 한글명 매핑을 위해 championKeyById(숫자id→key)의 역방향이 필요하다.
function buildNameByKey(championKeyById, championNameById) {
  const nameByKey = {};
  Object.entries(championKeyById).forEach(([id, key]) => {
    nameByKey[key] = championNameById[id];
  });
  return nameByKey;
}

export default function FeaturedChampions({ championKeyById = {}, championNameById = {} }) {
  const nameByKey = buildNameByKey(championKeyById, championNameById);

  return (
    <section style={cardStyle}>
      <Header />
      <div>
        {FEATURED.map((c, i) => {
          const tier = TIER_COLOR[c.tier] ?? TIER_COLOR['2'];
          return (
            <div key={c.key} style={{
              display: 'flex', alignItems: 'center', gap: 10,
              padding: '8px 14px',
              borderTop: i === 0 ? 'none' : '1px solid #1a2433',
            }}>
              <span style={{
                width: 18, textAlign: 'center', flexShrink: 0,
                color: i < 3 ? '#5383e8' : '#5a6b7e', fontSize: 13, fontWeight: 700,
              }}>{i + 1}</span>

              <img src={imgChampion(c.key)} alt={c.key} width={28} height={28}
                style={{ borderRadius: 5, flexShrink: 0, background: '#0d1520' }} />

              <span style={{
                flex: 1, minWidth: 0, color: '#dbe2ea', fontSize: 13, fontWeight: 600,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
              }}>{nameByKey[c.key] ?? c.key}</span>

              <span style={{
                flexShrink: 0, color: '#8899aa', fontSize: 12, width: 38, textAlign: 'center',
              }}>{c.position}</span>

              <span style={{
                flexShrink: 0, fontSize: 11, fontWeight: 700,
                color: tier.fg, background: tier.bg,
                padding: '3px 8px', borderRadius: 4, minWidth: 46, textAlign: 'center',
              }}>{tier.label}</span>
            </div>
          );
        })}
      </div>
    </section>
  );
}

function Header() {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '12px 14px', borderBottom: '1px solid #1a2433',
    }}>
      <h3 style={{ margin: 0, color: '#e2e8f0', fontSize: 14, fontWeight: 700 }}>
        주요 챔피언
      </h3>
      <span style={{ color: '#5a6b7e', fontSize: 11 }}>추천 티어</span>
    </div>
  );
}

const cardStyle = {
  background: '#151d2e',
  border: '1px solid #1f2a3a',
  borderRadius: 8,
  overflow: 'hidden',
};
