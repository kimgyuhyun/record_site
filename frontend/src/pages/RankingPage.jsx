import React from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import useChampionMeta from '../hooks/useChampionMeta';
import useRanking from '../hooks/useRanking';
import { imgChampion } from '../constants/ddragon';

/*
 * 랭킹 — 상위 티어(챌린저/그랜드마스터/마스터) 사다리.
 *  - 솔로랭크/자유랭크를 토글로 전환한다(둘 다 주기 갱신된 스냅샷, 실시간 아님).
 *  - 행 클릭 시 해당 소환사 페이지로 이동(이름 미해소 행은 비활성).
 *  - 모스트 챔피언은 우리가 수집한 매치 기준이라, 미수집 소환사는 비어 있을 수 있다.
 */
const PAGE_SIZE = 50;

// 사다리는 KR 상위 티어 기준이라 소환사 이동도 KR 리전으로 한다.
const LADDER_REGION = 'kr';

// 티어 라벨은 한글 대신 영어로 표기한다.
const TIER_STYLE = {
  CHALLENGER:  { bg: '#3a2a14', fg: '#f0b232', label: 'Challenger' },
  GRANDMASTER: { bg: '#3a1a1a', fg: '#e84057', label: 'Grandmaster' },
  MASTER:      { bg: '#2a1a3a', fg: '#b072f0', label: 'Master' },
};

const QUEUE_TABS = [
  { key: 'SOLO', label: '개인/2인 랭크 게임' },
  { key: 'FLEX', label: '자유 랭크 게임' },
];

export default function RankingPage() {
  const navigate = useNavigate();
  const { championKeyById } = useChampionMeta();
  // 큐/페이지는 URL 쿼리에 담는다. 소환사 페이지로 이동 후 뒤로가기 하면
  // 이전 페이지·큐가 그대로 복원돼(page/queue), 1페이지로 리셋되지 않는다.
  const [searchParams, setSearchParams] = useSearchParams();
  const queueType = searchParams.get('queue') === 'FLEX' ? 'FLEX' : 'SOLO';
  const page = Math.max(0, parseInt(searchParams.get('page'), 10) || 0);
  const { data, isLoading, isError } = useRanking(queueType, page, PAGE_SIZE);

  const rows = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;

  // 페이징은 히스토리를 더럽히지 않도록 replace. 현재 상태만 URL에 남겨
  // 뒤로가기 한 번으로 랭킹 → 이전 화면으로 나가도록 한다.
  const setPage = (next) => {
    setSearchParams(prev => {
      prev.set('page', String(next));
      return prev;
    }, { replace: true });
  };

  const changeQueue = (key) => {
    if (key === queueType) return;
    setSearchParams(prev => {
      prev.set('queue', key);
      prev.set('page', '0'); // 큐 전환 시 첫 페이지부터
      return prev;
    }, { replace: true });
  };

  const goToSummoner = (row) => {
    if (!row.gameName) return; // 이름 미해소 행은 이동 불가
    const slug = row.tagLine ? `${row.gameName}-${row.tagLine}` : row.gameName;
    navigate(`/find/${LADDER_REGION}/${encodeURIComponent(slug)}`);
  };

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: '20px 20px 48px' }}>
      <h1 style={{ color: '#e8edf4', fontSize: 20, fontWeight: 800, margin: '0 0 4px' }}>
        랭킹
      </h1>
      <p style={{ color: '#5a6b7e', fontSize: 12.5, margin: '0 0 14px' }}>
        챌린저~마스터 사다리입니다. 약 1시간마다 갱신되며, 모스트 챔피언은 수집된 매치 기준입니다.
      </p>

      {/* 솔로/자유 토글 */}
      <div style={{ display: 'flex', gap: 4, marginBottom: 12 }}>
        {QUEUE_TABS.map(tab => {
          const active = tab.key === queueType;
          return (
            <button key={tab.key} onClick={() => changeQueue(tab.key)} style={{
              background: active ? '#5383e8' : '#151d2e',
              border: `1px solid ${active ? '#5383e8' : '#2a3a4a'}`,
              color: active ? '#fff' : '#8899aa',
              fontSize: 13, fontWeight: 700, padding: '7px 18px', borderRadius: 6,
              cursor: 'pointer', fontFamily: 'inherit',
            }}>{tab.label}</button>
          );
        })}
      </div>

      {/* 상단 페이저 */}
      {!isError && !isLoading && rows.length > 0 && (
        <Pager page={page} totalPages={totalPages} onChange={setPage} />
      )}

      <div style={cardStyle}>
        <Row header>
          <Cell w={56} align="center">#</Cell>
          <Cell flex>플레이어</Cell>
          <Cell w={120}>티어</Cell>
          <Cell w={80} align="right">LP</Cell>
          <Cell w={150} align="right">승률</Cell>
          <Cell w={180}>모스트 챔피언</Cell>
        </Row>

        {isError && <StatusRow>랭킹을 불러오지 못했습니다.</StatusRow>}
        {!isError && isLoading && <StatusRow>불러오는 중…</StatusRow>}
        {!isError && !isLoading && rows.length === 0 && (
          <StatusRow>
            {queueType === 'FLEX'
              ? '자유랭크 랭킹을 처음 수집 중입니다. 갱신 완료까지 잠시 걸릴 수 있습니다.'
              : '랭킹이 아직 갱신되지 않았습니다. 첫 갱신까지 최대 1시간이 걸릴 수 있습니다.'}
          </StatusRow>
        )}

        {!isError && !isLoading && rows.map(row => {
          const tier = TIER_STYLE[row.tier] ?? TIER_STYLE.MASTER;
          const clickable = !!row.gameName;
          return (
            <Row key={row.rankPosition} onClick={clickable ? () => goToSummoner(row) : undefined}>
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
                  {(row.wins + row.losses) > 0 && (row.mostChampionIds ?? []).length === 0 && (
                    <span style={{ color: '#3a4757', fontSize: 11, alignSelf: 'center' }}>수집 전</span>
                  )}
                </div>
              </Cell>
            </Row>
          );
        })}
      </div>

      {/* 하단 페이저 */}
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

// < > 버튼 페이저 — 왼쪽 정렬, 표 위·아래에 하나씩 배치한다.
function Pager({ page, totalPages, onChange }) {
  const last = Math.max(totalPages, 1);
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-start', gap: 8, margin: '10px 0' }}>
      <ArrowButton disabled={page <= 0} onClick={() => onChange(page - 1)}>{'<'}</ArrowButton>
      <span style={{ color: '#8899aa', fontSize: 13, minWidth: 64, textAlign: 'center', fontVariantNumeric: 'tabular-nums' }}>
        {page + 1} / {last}
      </span>
      <ArrowButton disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>{'>'}</ArrowButton>
    </div>
  );
}

function ArrowButton({ children, disabled, onClick }) {
  return (
    <button disabled={disabled} onClick={onClick} style={{
      width: 34, height: 34,
      background: disabled ? '#11161f' : '#151d2e',
      border: `1px solid ${disabled ? '#1a2433' : '#2a3a4a'}`,
      color: disabled ? '#3a4757' : '#c8d0dc',
      fontSize: 15, fontWeight: 700, borderRadius: 6,
      cursor: disabled ? 'default' : 'pointer', fontFamily: 'inherit',
      display: 'flex', alignItems: 'center', justifyContent: 'center', lineHeight: 1,
    }}>{children}</button>
  );
}

function Row({ children, header, onClick }) {
  const clickable = !!onClick;
  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex', alignItems: 'center', gap: 8,
        padding: header ? '10px 16px' : '9px 16px',
        borderBottom: '1px solid #1a2433',
        background: header ? '#101826' : 'transparent',
        cursor: clickable ? 'pointer' : 'default',
        transition: 'background 0.12s',
      }}
      onMouseEnter={clickable ? e => { e.currentTarget.style.background = '#1a2333'; } : undefined}
      onMouseLeave={clickable ? e => { e.currentTarget.style.background = 'transparent'; } : undefined}
    >
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
