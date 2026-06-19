import React, { useState } from 'react';
import useChampionMeta from '../hooks/useChampionMeta';
import useRanking from '../hooks/useRanking';
import { imgChampion } from '../constants/ddragon';

/*
 * 랭킹 — 상위 티어(챌린저/그랜드마스터/마스터) 솔로랭크 사다리.
 *  - 주기 갱신된 스냅샷을 페이지 단위로 보여준다(실시간 아님).
 *  - 모스트 챔피언은 우리가 수집한 매치 기준이라, 미수집 소환사는 비어 있을 수 있다.
 */
const PAGE_SIZE = 50;

const TIER_STYLE = {
  CHALLENGER:  { bg: '#3a2a14', fg: '#f0b232', label: '챌린저' },
  GRANDMASTER: { bg: '#3a1a1a', fg: '#e84057', label: '그랜드마스터' },
  MASTER:      { bg: '#2a1a3a', fg: '#b072f0', label: '마스터' },
};

export default function RankingPage() {
  const { championKeyById } = useChampionMeta();
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useRanking('SOLO', page, PAGE_SIZE);

  const rows = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: '20px 20px 48px' }}>
      <h1 style={{ color: '#e8edf4', fontSize: 20, fontWeight: 800, margin: '0 0 4px' }}>
        랭킹 · 솔로랭크
      </h1>
      <p style={{ color: '#5a6b7e', fontSize: 12.5, margin: '0 0 16px' }}>
        챌린저~마스터 사다리입니다. 약 1시간마다 갱신되며, 모스트 챔피언은 수집된 매치 기준입니다.
      </p>

      <div style={cardStyle}>
        <Row header>
          <Cell w={56} align="center">순위</Cell>
          <Cell flex>플레이어</Cell>
          <Cell w={120}>티어</Cell>
          <Cell w={80} align="right">LP</Cell>
          <Cell w={150} align="right">승률</Cell>
          <Cell w={180}>모스트 챔피언</Cell>
        </Row>

        {isError && <StatusRow>랭킹을 불러오지 못했습니다.</StatusRow>}
        {!isError && isLoading && <StatusRow>불러오는 중…</StatusRow>}
        {!isError && !isLoading && rows.length === 0 && (
          <StatusRow>랭킹이 아직 갱신되지 않았습니다. 첫 갱신까지 최대 1시간이 걸릴 수 있습니다.</StatusRow>
        )}

        {!isError && !isLoading && rows.map(row => {
          const tier = TIER_STYLE[row.tier] ?? TIER_STYLE.MASTER;
          const games = row.wins + row.losses;
          return (
            <Row key={row.rankPosition}>
              <Cell w={56} align="center">
                <span style={{ color: '#8899aa', fontWeight: 700, fontSize: 13 }}>{row.rankPosition}</span>
              </Cell>
              <Cell flex>
                <span style={{ color: '#dbe2ea', fontSize: 13.5, fontWeight: 600,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                  {row.gameName
                    ? <>{row.gameName}<span style={{ color: '#5a6b7e' }}>#{row.tagLine}</span></>
                    : <span style={{ color: '#5a6b7e' }}>이름 해소 중…</span>}
                </span>
              </Cell>
              <Cell w={120}>
                <span style={{ fontSize: 11, fontWeight: 700, color: tier.fg, background: tier.bg,
                  padding: '3px 9px', borderRadius: 4 }}>{tier.label}</span>
              </Cell>
              <Cell w={80} align="right">
                <span style={{ color: '#dbe2ea', fontSize: 13, fontWeight: 600 }}>{row.leaguePoints.toLocaleString()}</span>
              </Cell>
              <Cell w={150} align="right">
                <span style={{ color: '#8899aa', fontSize: 12 }}>
                  {row.wins}승 {row.losses}패
                  <span style={{ color: winRateColor(row.winRate), marginLeft: 6, fontWeight: 700 }}>
                    ({row.winRate}%)
                  </span>
                </span>
              </Cell>
              <Cell w={180}>
                <div style={{ display: 'flex', gap: 4 }}>
                  {(row.mostChampionIds ?? []).map(id => {
                    const key = championKeyById[id];
                    if (!key) return null;
                    return <img key={id} src={imgChampion(key)} alt={key} width={26} height={26}
                      title={key} style={{ borderRadius: 5, background: '#0d1520' }} />;
                  })}
                  {games > 0 && (row.mostChampionIds ?? []).length === 0 && (
                    <span style={{ color: '#3a4757', fontSize: 11, alignSelf: 'center' }}>수집 전</span>
                  )}
                </div>
              </Cell>
            </Row>
          );
        })}
      </div>

      {!isError && !isLoading && rows.length > 0 && (
        <Pager page={page} totalPages={totalPages} onChange={setPage} />
      )}
    </div>
  );
}

function winRateColor(winRate) {
  if (winRate >= 55) return '#e84057';
  if (winRate >= 50) return '#5383e8';
  return '#8899aa';
}

function Pager({ page, totalPages, onChange }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12, marginTop: 16 }}>
      <PagerButton disabled={page <= 0} onClick={() => onChange(page - 1)}>‹ 이전</PagerButton>
      <span style={{ color: '#8899aa', fontSize: 13 }}>{page + 1} / {Math.max(totalPages, 1)}</span>
      <PagerButton disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>다음 ›</PagerButton>
    </div>
  );
}

function PagerButton({ children, disabled, onClick }) {
  return (
    <button disabled={disabled} onClick={onClick} style={{
      background: disabled ? '#11161f' : '#151d2e',
      border: `1px solid ${disabled ? '#1a2433' : '#2a3a4a'}`,
      color: disabled ? '#3a4757' : '#c8d0dc',
      fontSize: 13, fontWeight: 600, padding: '7px 14px', borderRadius: 6,
      cursor: disabled ? 'default' : 'pointer', fontFamily: 'inherit',
    }}>{children}</button>
  );
}

function Row({ children, header }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8,
      padding: header ? '10px 16px' : '9px 16px',
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
