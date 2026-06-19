import React, { useMemo, useState } from 'react';
import useChampionMeta from '../hooks/useChampionMeta';
import useChampionTierList from '../hooks/useChampionTierList';
import { imgChampion } from '../constants/ddragon';

/*
 * 챔피언 분석 — 자체 수집한 매치 DB를 집계한 전역 티어 리스트.
 *  - 승률/픽률/티어는 우리 DB 표본 기반(외부 통계 API 없음). 표본이 적으면 점진적으로 채워진다.
 *  - 큐(전체/솔로/자유)·포지션·이름 검색으로 필터링한다.
 */

// Riot teamPosition → 한글 라벨
const POSITION_LABEL = {
  TOP: '탑', JUNGLE: '정글', MIDDLE: '미드', BOTTOM: '바텀', UTILITY: '서포터',
};
const POSITION_FILTERS = [
  { key: 'ALL', label: '전체' },
  { key: 'TOP', label: '탑' },
  { key: 'JUNGLE', label: '정글' },
  { key: 'MIDDLE', label: '미드' },
  { key: 'BOTTOM', label: '바텀' },
  { key: 'UTILITY', label: '서포터' },
];
const QUEUE_FILTERS = [
  { key: undefined, label: '전체' },
  { key: 'SOLO', label: '솔로랭크' },
  { key: 'FLEX', label: '자유랭크' },
];

const TIER_STYLE = {
  OP: { bg: '#3a1d2b', fg: '#ff6b9d' },
  '1': { bg: '#1d2c3a', fg: '#5383e8' },
  '2': { bg: '#1b2f33', fg: '#42c9b0' },
  '3': { bg: '#26323f', fg: '#8fa3bd' },
  '4': { bg: '#222a35', fg: '#5a6b7e' },
};

export default function ChampionAnalysisPage() {
  const { championKeyById, championNameById } = useChampionMeta();
  const [queueType, setQueueType] = useState(undefined);
  const [position, setPosition] = useState('ALL');
  const [search, setSearch] = useState('');

  const { rows, isLoading, isError } = useChampionTierList(queueType);

  const filtered = useMemo(() => {
    const keyword = search.trim().toLowerCase();
    return rows.filter(row => {
      if (position !== 'ALL' && row.position !== position) return false;
      if (!keyword) return true;
      const ko = (championNameById[row.championId] || '').toLowerCase();
      const en = (row.championName || '').toLowerCase();
      return ko.includes(keyword) || en.includes(keyword);
    });
  }, [rows, position, search, championNameById]);

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '20px 20px 48px' }}>
      <h1 style={{ color: '#e8edf4', fontSize: 20, fontWeight: 800, margin: '0 0 4px' }}>
        챔피언 분석
      </h1>
      <p style={{ color: '#5a6b7e', fontSize: 12.5, margin: '0 0 16px' }}>
        자체 수집한 매치 데이터를 집계한 통계입니다. 검색이 쌓일수록 표본이 늘어납니다.
      </p>

      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 14 }}>
        <FilterGroup items={QUEUE_FILTERS} activeKey={queueType} onSelect={setQueueType} />
        <span style={{ width: 1, height: 20, background: '#23303f' }} />
        <FilterGroup items={POSITION_FILTERS} activeKey={position} onSelect={setPosition} />
        <input
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder="챔피언 검색"
          style={{
            marginLeft: 'auto', height: 34, width: 180,
            background: '#151d2e', border: '1px solid #2a3a4a', borderRadius: 6,
            color: '#e2e8f0', fontSize: 13, padding: '0 12px', outline: 'none',
          }}
        />
      </div>

      <div style={cardStyle}>
        <Row header>
          <Cell w={48} align="center">#</Cell>
          <Cell flex>챔피언</Cell>
          <Cell w={80} align="center">포지션</Cell>
          <Cell w={70} align="center">티어</Cell>
          <Cell w={80} align="right">승률</Cell>
          <Cell w={80} align="right">픽률</Cell>
          <Cell w={90} align="right">게임</Cell>
        </Row>

        {isError && <StatusRow>통계를 불러오지 못했습니다.</StatusRow>}
        {!isError && isLoading && <StatusRow>불러오는 중…</StatusRow>}
        {!isError && !isLoading && filtered.length === 0 && (
          <StatusRow>
            {rows.length === 0
              ? '아직 집계된 매치가 부족합니다. 소환사 검색이 쌓이면 자동으로 채워집니다.'
              : '검색 결과가 없습니다.'}
          </StatusRow>
        )}

        {!isError && !isLoading && filtered.map((row, i) => {
          const key = championKeyById[row.championId];
          const name = championNameById[row.championId] || row.championName || '알 수 없음';
          const tier = TIER_STYLE[row.tier] ?? TIER_STYLE['4'];
          return (
            <Row key={row.championId}>
              <Cell w={48} align="center">
                <span style={{ color: i < 3 ? '#5383e8' : '#5a6b7e', fontWeight: 700, fontSize: 13 }}>{i + 1}</span>
              </Cell>
              <Cell flex>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
                  {key && <img src={imgChampion(key)} alt={name} width={32} height={32}
                    style={{ borderRadius: 6, background: '#0d1520', flexShrink: 0 }} />}
                  <span style={{ color: '#dbe2ea', fontSize: 13.5, fontWeight: 600,
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</span>
                </div>
              </Cell>
              <Cell w={80} align="center">
                <span style={{ color: '#8899aa', fontSize: 12.5 }}>
                  {POSITION_LABEL[row.position] ?? '-'}
                </span>
              </Cell>
              <Cell w={70} align="center">
                <span style={{
                  fontSize: 11, fontWeight: 700, color: tier.fg, background: tier.bg,
                  padding: '3px 9px', borderRadius: 4, display: 'inline-block', minWidth: 30,
                }}>{row.tier === 'OP' ? 'OP' : row.tier}</span>
              </Cell>
              <Cell w={80} align="right">
                <span style={{ color: '#dbe2ea', fontSize: 13 }}>{row.winRate.toFixed(1)}%</span>
              </Cell>
              <Cell w={80} align="right">
                <span style={{ color: '#aeb9c7', fontSize: 13 }}>{row.pickRate.toFixed(1)}%</span>
              </Cell>
              <Cell w={90} align="right">
                <span style={{ color: '#8899aa', fontSize: 12.5 }}>{row.games.toLocaleString()}</span>
              </Cell>
            </Row>
          );
        })}
      </div>
    </div>
  );
}

function FilterGroup({ items, activeKey, onSelect }) {
  return (
    <div style={{ display: 'flex', gap: 4 }}>
      {items.map(item => {
        const active = item.key === activeKey;
        return (
          <button key={item.label} onClick={() => onSelect(item.key)} style={{
            background: active ? '#5383e8' : '#151d2e',
            border: `1px solid ${active ? '#5383e8' : '#2a3a4a'}`,
            color: active ? '#fff' : '#8899aa',
            fontSize: 12.5, fontWeight: 600, padding: '6px 12px', borderRadius: 6,
            cursor: 'pointer', fontFamily: 'inherit', whiteSpace: 'nowrap',
          }}>{item.label}</button>
        );
      })}
    </div>
  );
}

function Row({ children, header }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8,
      padding: header ? '10px 16px' : '8px 16px',
      borderBottom: '1px solid #1a2433',
      background: header ? '#101826' : 'transparent',
    }}>
      {children}
    </div>
  );
}

function Cell({ children, w, flex, align = 'left' }) {
  return (
    <div style={{
      width: flex ? undefined : w, flex: flex ? 1 : undefined, minWidth: 0,
      textAlign: align, color: '#5a6b7e', fontSize: 12, fontWeight: 600,
    }}>
      {children}
    </div>
  );
}

function StatusRow({ children }) {
  return (
    <div style={{ color: '#5a6b7e', fontSize: 13, padding: '40px 16px', textAlign: 'center' }}>
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
