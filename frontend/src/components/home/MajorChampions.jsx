import React, { useMemo } from 'react';
import { imgChampion, DDRAGON_VERSION } from '../../constants/ddragon';
import useChampionTierList from '../../hooks/useChampionTierList';
import HoverTip from '../common/HoverTip';

/*
 * 주요 챔피언 위젯 (홈) — 자체 수집한 매치 DB 집계 기반 승률/픽률/밴율.
 *  - '주요' = 픽률(가장 많이 플레이된) 상위 N. 전체 큐 기준.
 *  - 밴율은 밴 데이터가 쌓인 신규 매치부터 채워지므로, 초기엔 0%일 수 있다.
 *  - championKeyById/championNameById 는 HomePage에서 1회 로드해 전달한다.
 */
const TOP_N = 10;

export default function MajorChampions({ championKeyById = {}, championNameById = {} }) {
  const { rows, isLoading, isError } = useChampionTierList(undefined);
  const metaReady = Object.keys(championKeyById).length > 0;

  // 픽률 내림차순 상위 N (많이 플레이된 = 주요 챔피언)
  const topRows = useMemo(
    () => [...rows].sort((a, b) => b.pickRate - a.pickRate).slice(0, TOP_N),
    [rows],
  );

  return (
    <section style={cardStyle}>
      <div style={headerStyle}>
        <h3 style={{ margin: 0, color: '#e2e8f0', fontSize: 14, fontWeight: 700 }}>주요 챔피언</h3>
        <span style={{ color: '#5a6b7e', fontSize: 11 }}>v{DDRAGON_VERSION} · 수집 표본</span>
      </div>

      {/* 컬럼 헤더 */}
      <div style={{ ...rowStyle, borderBottom: '1px solid #1a2433', cursor: 'default' }}>
        <span style={rankColStyle}>#</span>
        <span style={{ flex: 1, color: '#5a6b7e', fontSize: 11, fontWeight: 600 }}>챔피언</span>
        <span style={headColStyle}>승률</span>
        <span style={headColStyle}>픽률</span>
        <span style={headColStyle}>밴율</span>
      </div>

      {isError && <StatusText>통계를 불러오지 못했습니다.</StatusText>}
      {!isError && (isLoading || !metaReady) && <StatusText>불러오는 중…</StatusText>}
      {!isError && !isLoading && metaReady && topRows.length === 0 && (
        <StatusText>아직 집계된 매치가 부족합니다. 검색이 쌓이면 채워집니다.</StatusText>
      )}

      {!isError && !isLoading && metaReady && topRows.map((row, i) => {
        const key = championKeyById[row.championId];
        const name = championNameById[row.championId] || row.championName || '—';
        return (
          <div key={row.championId} style={rowStyle}>
            <span style={rankColStyle}>{i + 1}</span>
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8, minWidth: 0 }}>
              {key && (
                <HoverTip label={name}>
                  <img src={imgChampion(key)} alt={name} width={24} height={24}
                    style={{ borderRadius: 5, background: '#0d1520', flexShrink: 0 }} />
                </HoverTip>
              )}
              <span style={{ color: '#cdd6e2', fontSize: 12.5, overflow: 'hidden',
                textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</span>
            </div>
            <span style={{ ...pctColStyle, color: '#dbe2ea' }}>{row.winRate.toFixed(1)}%</span>
            <span style={{ ...pctColStyle, color: '#aeb9c7' }}>{row.pickRate.toFixed(1)}%</span>
            <span style={{ ...pctColStyle, color: '#aeb9c7' }}>{row.banRate.toFixed(1)}%</span>
          </div>
        );
      })}
    </section>
  );
}

function StatusText({ children }) {
  return (
    <div style={{ color: '#5a6b7e', fontSize: 12.5, padding: '18px 0', textAlign: 'center' }}>
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

const headerStyle = {
  padding: '12px 14px', borderBottom: '1px solid #1a2433',
  display: 'flex', alignItems: 'baseline', justifyContent: 'space-between',
};

const rowStyle = {
  display: 'flex', alignItems: 'center', gap: 8,
  padding: '7px 14px', borderBottom: '1px solid #141c29',
};

const rankColStyle = { width: 16, color: '#5a6b7e', fontSize: 11, fontWeight: 700, textAlign: 'center', flexShrink: 0 };
const headColStyle = { width: 52, color: '#5a6b7e', fontSize: 11, fontWeight: 600, textAlign: 'right', flexShrink: 0 };
const pctColStyle = { width: 52, fontSize: 12, fontWeight: 600, textAlign: 'right', flexShrink: 0, fontVariantNumeric: 'tabular-nums' };
