import React from 'react';
import { imgChampion, imgSkinLoading } from '../../constants/ddragon';

/*
 * 챔피언 / 스킨 세일 (큐레이션).
 *  - 실제 세일 API가 없어 항목은 편집형이지만, 아이콘/일러스트는 실제 Data Dragon 이미지를 쓴다.
 *  - 스킨 번호가 어긋나면 onError 로 기본 스킨(num 0) 일러스트로 폴백한다.
 *  - props.championKeyById/championNameById: 챔피언 세일의 한글명 표시에 사용.
 */

// 챔피언 세일 (RP/파랑정수는 편집형 예시)
const CHAMPION_SALES = [
  { key: 'Garen',  oldRp: 790, newRp: 395 },
  { key: 'Ashe',   oldRp: 790, newRp: 395 },
  { key: 'Darius', oldRp: 880, newRp: 440 },
  { key: 'Lux',    oldRp: 880, newRp: 440 },
];

// 스킨 세일 (skin num·이름은 편집형 예시)
const SKIN_SALES = [
  { key: 'Ahri',   num: 4,  name: 'K/DA 아리',        oldRp: 1350, newRp: 675 },
  { key: 'Lux',    num: 5,  name: '엘레멘탈리스트 럭스', oldRp: 1820, newRp: 910 },
  { key: 'Jinx',   num: 4,  name: '별 수호자 징크스',   oldRp: 1350, newRp: 675 },
  { key: 'Yasuo',  num: 3,  name: '검은 장미단 야스오',  oldRp: 1350, newRp: 675 },
];

function discountPct(oldRp, newRp) {
  if (!oldRp) return 0;
  return Math.round((1 - newRp / oldRp) * 100);
}

function fallbackToBaseSkin(e, key) {
  if (e.currentTarget.dataset.fallback) return;
  e.currentTarget.dataset.fallback = '1';
  e.currentTarget.src = imgSkinLoading(key, 0);
}

export default function ChampionSkinSale({ championKeyById = {}, championNameById = {} }) {
  // championNameById 는 숫자 id 키이므로, ddragon key → 한글명 역매핑을 만든다.
  const nameByKey = {};
  Object.entries(championKeyById).forEach(([id, key]) => { nameByKey[key] = championNameById[id]; });

  return (
    <section style={cardStyle}>
      <div style={{ padding: '12px 14px', borderBottom: '1px solid #1a2433' }}>
        <h3 style={{ margin: 0, color: '#e2e8f0', fontSize: 14, fontWeight: 700 }}>
          진행 중인 세일
        </h3>
      </div>

      <div style={{ padding: 14 }}>
        <SubTitle>챔피언</SubTitle>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: 10 }}>
          {CHAMPION_SALES.map(c => (
            <div key={c.key} style={championTileStyle}>
              <img src={imgChampion(c.key)} alt={c.key} width={40} height={40}
                style={{ borderRadius: 6, flexShrink: 0, background: '#0d1520' }} />
              <div style={{ minWidth: 0 }}>
                <div style={{ color: '#dbe2ea', fontSize: 12.5, fontWeight: 600,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{nameByKey[c.key] ?? c.key}</div>
                <PriceRow oldRp={c.oldRp} newRp={c.newRp} />
              </div>
            </div>
          ))}
        </div>

        <SubTitle style={{ marginTop: 16 }}>스킨</SubTitle>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: 12 }}>
          {SKIN_SALES.map(s => (
            <div key={`${s.key}-${s.num}`} style={skinCardStyle}>
              <div style={{ position: 'relative' }}>
                <img src={imgSkinLoading(s.key, s.num)} alt={s.name}
                  onError={e => fallbackToBaseSkin(e, s.key)}
                  style={{ width: '100%', height: 130, objectFit: 'cover', display: 'block' }} />
                <span style={discountBadgeStyle}>-{discountPct(s.oldRp, s.newRp)}%</span>
              </div>
              <div style={{ padding: '8px 9px' }}>
                <div style={{ color: '#dbe2ea', fontSize: 12, fontWeight: 600,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{s.name}</div>
                <PriceRow oldRp={s.oldRp} newRp={s.newRp} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function PriceRow({ oldRp, newRp }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 3 }}>
      <span style={{ color: '#5a6b7e', fontSize: 11, textDecoration: 'line-through' }}>{oldRp} RP</span>
      <span style={{ color: '#ffb454', fontSize: 12, fontWeight: 700 }}>{newRp} RP</span>
    </div>
  );
}

function SubTitle({ children, style }) {
  return (
    <div style={{ color: '#8899aa', fontSize: 12, fontWeight: 600, marginBottom: 8, ...style }}>
      {children}
    </div>
  );
}

const cardStyle = {
  background: '#151d2e',
  border: '1px solid #1f2a3a',
  borderRadius: 8,
  overflow: 'hidden',
};

const championTileStyle = {
  display: 'flex', alignItems: 'center', gap: 9,
  background: '#0f1726', border: '1px solid #1f2a3a', borderRadius: 7, padding: 8,
};

const skinCardStyle = {
  background: '#0f1726', border: '1px solid #1f2a3a', borderRadius: 7, overflow: 'hidden',
};

const discountBadgeStyle = {
  position: 'absolute', top: 6, right: 6,
  background: '#c0392b', color: '#fff', fontSize: 10, fontWeight: 700,
  padding: '2px 6px', borderRadius: 4,
};
