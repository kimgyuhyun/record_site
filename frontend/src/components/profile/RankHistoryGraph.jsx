import React, { useEffect, useMemo, useState } from 'react';
import { getRankHistory } from '../../api/summoner';

/*
 * 티어/LP 변동 이력 그래프.
 *  - 전적 갱신 때마다 저장된 랭크 스냅샷(rank_snapshot)을 시계열로 받아 LP(=사다리 점수) 추이를 그린다.
 *  - Riot 은 과거 LP 를 주지 않으므로, 이 그래프는 "갱신을 한 시점부터" 쌓인다(과거 소급 불가).
 *  - X축: 스냅샷(=랭크 게임) 순서, Y축: 티어/디비전/LP 를 합친 절대 점수.
 */

// LadderScore(백엔드)와 동일한 티어 경계. base = 티어 시작 점수(한 티어 = 400).
const TIER_BANDS = [
  { base: 0,    label: '아이언',   color: '#6b6b6b' },
  { base: 400,  label: '브론즈',   color: '#a04000' },
  { base: 800,  label: '실버',     color: '#7f8c8d' },
  { base: 1200, label: '골드',     color: '#e67e22' },
  { base: 1600, label: '플래티넘', color: '#1abc9c' },
  { base: 2000, label: '에메랄드', color: '#27ae60' },
  { base: 2400, label: '다이아',   color: '#3498db' },
  { base: 2800, label: '마스터+',  color: '#9b59b6' },
];

const tierBandOf = (score) => {
  let band = TIER_BANDS[0];
  for (const b of TIER_BANDS) if (score >= b.base) band = b;
  return band;
};
const fmtDate = (ms) => new Date(ms).toLocaleDateString('ko-KR', { month: 'numeric', day: 'numeric' });

const QUEUES = [
  { key: 'solo', label: '개인/2인전' },
  { key: 'flex', label: '자유 5:5' },
];

export default function RankHistoryGraph({ puuid }) {
  const [history, setHistory] = useState(null); // { solo:[], flex:[] }
  const [loading, setLoading] = useState(false);
  const [queue, setQueue] = useState('solo');

  useEffect(() => {
    if (!puuid) return undefined;
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      try {
        const res = await getRankHistory(puuid);
        if (!cancelled) setHistory(res.data);
      } catch {
        if (!cancelled) setHistory({ solo: [], flex: [] });
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [puuid]);

  const available = useMemo(() => ({
    solo: history?.solo?.length || 0,
    flex: history?.flex?.length || 0,
  }), [history]);

  // 선택 큐에 데이터가 없으면 데이터 있는 큐로 자동 표시(상태 보정 효과 없이 렌더 시 파생)
  const effQueue = available[queue] > 0
    ? queue
    : (available.solo > 0 ? 'solo' : available.flex > 0 ? 'flex' : queue);

  if (loading && !history) return <Card><Muted>LP 이력 불러오는 중…</Muted></Card>;
  if (!history) return null;

  const totalPoints = available.solo + available.flex;
  if (totalPoints === 0) {
    return (
      <Card>
        <Header queue={queue} setQueue={setQueue} available={available} />
        <Muted>
          아직 LP 기록이 없어요. 전적 갱신을 누르면 그 시점부터 LP 변화가 쌓이고,
          랭크 게임이 누적될수록 그래프가 채워집니다.
        </Muted>
      </Card>
    );
  }

  const points = history[effQueue] || [];
  return (
    <Card>
      <Header queue={effQueue} setQueue={setQueue} available={available} />
      {points.length === 0
        ? <Muted>이 큐는 아직 기록이 없어요.</Muted>
        : <Graph points={points} />}
    </Card>
  );
}

function Graph({ points }) {
  const [hover, setHover] = useState(null);

  const W = 720, H = 240, L = 60, R = 18, T = 16, B = 28;
  const plotW = W - L - R, plotH = H - T - B;

  const scores = points.map(p => p.ladderScore);
  const rawMin = Math.min(...scores), rawMax = Math.max(...scores);
  const pad = Math.max(60, Math.round((rawMax - rawMin) * 0.2));
  const yMin = Math.max(0, rawMin - pad);
  const yMax = rawMax + pad;
  const ySpan = Math.max(1, yMax - yMin);

  const n = points.length;
  const xOf = (i) => (n === 1 ? L + plotW / 2 : L + (i / (n - 1)) * plotW);
  const yOf = (s) => T + plotH - ((s - yMin) / ySpan) * plotH;

  // 100단위(디비전) 가는 선 + 400단위(티어) 라벨 선
  const gridLines = [];
  const start = Math.ceil(yMin / 100) * 100;
  for (let v = start; v <= yMax; v += 100) {
    gridLines.push({ v, tier: v % 400 === 0 });
  }

  const linePts = points.map((p, i) => `${xOf(i)},${yOf(p.ladderScore)}`).join(' ');

  // X축 날짜 라벨: 처음/중간/끝
  const xTicks = n === 1 ? [0] : [0, Math.floor((n - 1) / 2), n - 1];

  return (
    <div style={{ position: 'relative' }}>
      <svg viewBox={`0 0 ${W} ${H}`} style={{ width: '100%', height: 'auto', display: 'block' }}
        onMouseLeave={() => setHover(null)}>
        {/* 가로 그리드 + 티어 라벨 */}
        {gridLines.map(({ v, tier }) => {
          const y = yOf(v);
          const band = tierBandOf(v);
          return (
            <g key={v}>
              <line x1={L} y1={y} x2={W - R} y2={y}
                stroke={tier ? '#2f4256' : '#1d2a38'} strokeWidth={tier ? 1 : 0.6}
                strokeDasharray={tier ? 'none' : '3 3'} />
              {tier && (
                <text x={L - 8} y={y + 3} textAnchor="end" fontSize="10" fill={band.color}>
                  {band.label}
                </text>
              )}
            </g>
          );
        })}

        {/* LP 추이 라인 */}
        {n > 1 && <polyline points={linePts} fill="none" stroke="#00d3ab" strokeWidth="2" />}

        {/* 점 */}
        {points.map((p, i) => {
          const cx = xOf(i), cy = yOf(p.ladderScore);
          const active = hover === i;
          return (
            <circle key={i} cx={cx} cy={cy} r={active ? 5 : 3.5}
              fill={active ? '#fff' : '#00d3ab'} stroke="#0b3b32" strokeWidth="1"
              style={{ cursor: 'pointer' }}
              onMouseEnter={() => setHover(i)} />
          );
        })}

        {/* X축 날짜 */}
        {xTicks.map(i => (
          <text key={i} x={xOf(i)} y={H - 8} textAnchor="middle" fontSize="10" fill="#5a6b7e">
            {fmtDate(points[i].at)}
          </text>
        ))}

        {/* 호버 툴팁 */}
        {hover != null && (() => {
          const p = points[hover];
          const cx = xOf(hover), cy = yOf(p.ladderScore);
          const prev = hover > 0 ? points[hover - 1] : null;
          const delta = prev ? p.ladderScore - prev.ladderScore : null;
          const band = tierBandOf(p.ladderScore);
          const tierText = `${band.label}${p.division ? ' ' + p.division : ''} ${p.leaguePoints}LP`;
          const deltaText = delta == null ? '' : `${delta > 0 ? '▲' : delta < 0 ? '▼' : ''}${Math.abs(delta)} LP`;
          const boxW = 124, boxH = delta == null ? 38 : 52;
          const bx = Math.min(Math.max(cx - boxW / 2, L), W - R - boxW);
          const by = Math.max(cy - boxH - 12, 2);
          return (
            <g pointerEvents="none">
              <line x1={cx} y1={T} x2={cx} y2={T + plotH} stroke="#3a4a5a" strokeWidth="0.6" strokeDasharray="3 3" />
              <rect x={bx} y={by} width={boxW} height={boxH} rx="6" fill="#000" opacity="0.92" />
              <text x={bx + 10} y={by + 16} fontSize="11" fontWeight="700" fill="#fff">{tierText}</text>
              {delta != null && (
                <text x={bx + 10} y={by + 32} fontSize="11" fontWeight="700"
                  fill={delta > 0 ? '#2bb673' : delta < 0 ? '#e84057' : '#9aa7b4'}>{deltaText}</text>
              )}
              <text x={bx + 10} y={by + (delta == null ? 31 : 47)} fontSize="9.5" fill="#9aa7b4">
                {fmtDate(p.at)}
              </text>
            </g>
          );
        })()}
      </svg>
    </div>
  );
}

function Header({ queue, setQueue, available }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
      <span style={{ color: '#e8edf4', fontSize: 14, fontWeight: 700 }}>LP 변동 이력</span>
      <div style={{ display: 'inline-flex', gap: 4 }}>
        {QUEUES.map(q => {
          const has = available[q.key] > 0;
          const active = queue === q.key;
          return (
            <button key={q.key} onClick={() => has && setQueue(q.key)} disabled={!has} style={{
              background: active ? '#00d3ab' : '#15212e',
              color: active ? '#06241e' : has ? '#9aa7b4' : '#3e4a5a',
              border: `1px solid ${active ? '#00d3ab' : '#2a3a4a'}`,
              borderRadius: 6, fontSize: 11.5, fontWeight: 700, padding: '5px 10px',
              cursor: has ? 'pointer' : 'default', fontFamily: 'inherit',
            }}>{q.label}</button>
          );
        })}
      </div>
    </div>
  );
}

function Card({ children }) {
  return (
    <div style={{
      background: 'linear-gradient(135deg, #111c27 0%, #1a2535 100%)',
      border: '1px solid #2a3a4a', borderRadius: 10, padding: '14px 16px',
    }}>{children}</div>
  );
}

function Muted({ children }) {
  return <div style={{ color: '#7a8a9e', fontSize: 12.5, lineHeight: 1.6, padding: '8px 0' }}>{children}</div>;
}
