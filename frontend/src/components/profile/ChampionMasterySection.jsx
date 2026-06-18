import React from 'react';
import { imgChampion } from '../../constants/ddragon';

/* ───────── 사이트 공통 테마 토큰 ───────── */
const C = {
  panel:       '#111c27',
  panelBorder: '#2a3a4a',
  inner:       '#0f1923',
  gold:        '#c89b3c',
  text:        '#e8e0d0',
  sub:         '#6b7a8d',
  muted:       '#4a5568',
};

// 숙련도 레벨별 뱃지 색 (높을수록 화려하게)
function levelColor(level) {
  if (level >= 10) return '#e6b800';
  if (level >= 7)  return '#9b59b6';
  if (level >= 5)  return '#3b82f6';
  return '#5b6b7d';
}

const fmtPoints = (p) => (p ?? 0).toLocaleString();

function MasteryCard({ entry, championKeyById, championNameById }) {
  const key = championKeyById[entry.championId] ||
    String(entry.championId);
  const name = championNameById[entry.championId] || key;
  return (
    <div style={{ width: 84, flexShrink: 0, textAlign: 'center' }}>
      <div style={{ position: 'relative', width: 56, height: 56, margin: '0 auto 10px' }}>
        <img
          src={imgChampion(key)} alt={name}
          style={{ width: 56, height: 56, borderRadius: '50%', objectFit: 'cover',
            border: `2px solid ${levelColor(entry.championLevel)}66` }}
          onError={e => { e.target.style.visibility = 'hidden'; }}
        />
        <span style={{
          position: 'absolute', bottom: -7, left: '50%', transform: 'translateX(-50%)',
          background: levelColor(entry.championLevel), color: '#fff',
          fontSize: 10, fontWeight: 800, lineHeight: '14px',
          minWidth: 18, padding: '0 5px', borderRadius: 8,
          border: '1px solid #0a0f17',
        }}>{entry.championLevel}</span>
      </div>
      <div style={{ color: C.text, fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap',
        overflow: 'hidden', textOverflow: 'ellipsis' }}>{name}</div>
      <div style={{ color: C.gold, fontSize: 11, fontWeight: 700, marginTop: 2 }}>
        {fmtPoints(entry.championPoints)}
      </div>
    </div>
  );
}

export default function ChampionMasterySection({
  mastery = [], championKeyById = {}, championNameById = {}, loading = false,
}) {
  return (
    <div style={{
      background: C.panel, border: `1px solid ${C.panelBorder}`,
      borderRadius: 10, padding: '16px 20px', marginBottom: 16,
    }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 14 }}>
        <h3 style={{ margin: 0, color: C.text, fontSize: 15, fontWeight: 700 }}>챔피언 숙련도</h3>
        <span style={{ color: C.sub, fontSize: 12 }}>숙련도 점수 순</span>
      </div>

      {loading ? (
        <div style={{ color: C.muted, fontSize: 13, padding: '12px 0' }}>숙련도 조회 중...</div>
      ) : mastery.length === 0 ? (
        <div style={{ color: C.muted, fontSize: 13, padding: '12px 0' }}>숙련도 데이터가 없습니다.</div>
      ) : (
        <div style={{ display: 'flex', gap: 14, overflowX: 'auto', paddingBottom: 6 }}>
          {mastery.map(entry => (
            <MasteryCard key={entry.championId} entry={entry}
              championKeyById={championKeyById} championNameById={championNameById} />
          ))}
        </div>
      )}
    </div>
  );
}
