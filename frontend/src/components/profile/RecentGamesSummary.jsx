import React, { useMemo } from 'react';
import { imgChampion } from '../../constants/ddragon';

/* ───────── 사이트 공통 테마 토큰 (네이비/틸) ───────── */
const C = {
  panel:       '#111c27',
  panelBorder: '#2a3a4a',
  inner:       '#0f1923',
  divider:     '#1a2535',
  gold:        '#c89b3c',
  text:        '#e8e0d0',
  sub:         '#6b7a8d',
  muted:       '#4a5568',
  win:         '#5383e8',
  loss:        '#e84057',
};

const RANKED_QUEUES = new Set([420, 440]);

const POSITIONS = [
  { key: 'TOP',     label: '탑'   },
  { key: 'JUNGLE',  label: '정글' },
  { key: 'MIDDLE',  label: '미드' },
  { key: 'BOTTOM',  label: '바텀' },
  { key: 'UTILITY', label: '서폿' },
];

/* KDA 평점 색상: 5.0+ 골드 / 3.0+ 그린 / 그 외 서브 */
function kdaColor(ratio) {
  if (ratio >= 5) return C.gold;
  if (ratio >= 3) return '#2bb673';
  return C.sub;
}
/* 승률 색상: 60%+ 빨강 강조 */
function winRateColor(rate) {
  return rate >= 60 ? '#e74c3c' : C.text;
}

/* ───────── 포지션 아이콘 (간단 라인 SVG) ───────── */
function PositionIcon({ pos, color, size = 18 }) {
  const mark = {
    TOP:     <path d="M7 7h5M7 7v5" />,
    JUNGLE:  <path d="M12 16V8M9 11l3-3 3 3" />,
    MIDDLE:  <path d="M8 16L16 8" />,
    BOTTOM:  <path d="M17 17h-5M17 17v-5" />,
    UTILITY: <path d="M12 8v8M8 12h8" />,
  }[pos];
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none"
      stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3.5" y="3.5" width="17" height="17" rx="3" opacity="0.3" />
      {mark}
    </svg>
  );
}

/* ───────── 승률 도넛 ───────── */
function WinRateDonut({ wins, losses, size = 88 }) {
  const total = wins + losses;
  const rate = total ? Math.round((wins / total) * 100) : 0;
  const r = size / 2 - 6;
  const circ = 2 * Math.PI * r;
  const winLen = total ? (wins / total) * circ : 0;
  const cx = size / 2;
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} style={{ flexShrink: 0 }}>
      <circle cx={cx} cy={cx} r={r} fill="none" stroke={C.loss} strokeWidth="9" />
      {total > 0 && (
        <circle cx={cx} cy={cx} r={r} fill="none" stroke={C.win} strokeWidth="9"
          strokeDasharray={`${winLen} ${circ - winLen}`}
          transform={`rotate(-90 ${cx} ${cx})`} strokeLinecap="butt" />
      )}
      <text x={cx} y={cx - 2} textAnchor="middle" fontSize="19" fontWeight="800" fill={C.text}>{rate}%</text>
      <text x={cx} y={cx + 15} textAnchor="middle" fontSize="10" fill={C.sub}>승률</text>
    </svg>
  );
}

/* ───────── 내 포지션 추출 (참가자 목록에서 puuid 매칭) ───────── */
function myPosition(match) {
  const me = match.participantSummaryDtos?.find(p => p.puuid === match.myPuuid);
  return me?.teamPosition || '';
}

export default function RecentGamesSummary({ matches = [], championKeyById = {}, search, onSearchChange }) {
  const stat = useMemo(() => {
    const total = matches.length;
    let wins = 0, losses = 0;
    let sumK = 0, sumD = 0, sumA = 0, sumKp = 0, kpCount = 0;
    const champMap = new Map();   // championId -> { id, name, games, wins, sumK, sumD, sumA }
    const posMap = new Map();     // position -> count (랭크만)

    for (const m of matches) {
      const remake = m.gameEndedInEarlySurrender;
      if (!remake) (m.myWin ? wins++ : losses++);

      sumK += m.myKills; sumD += m.myDeaths; sumA += m.myAssists;
      if (typeof m.myKillParticipation === 'number') { sumKp += m.myKillParticipation; kpCount++; }

      const c = champMap.get(m.myChampionId) ||
        { id: m.myChampionId, name: m.myChampionName, games: 0, wins: 0, sumK: 0, sumD: 0, sumA: 0 };
      c.games++; if (m.myWin && !remake) c.wins++;
      c.sumK += m.myKills; c.sumD += m.myDeaths; c.sumA += m.myAssists;
      champMap.set(m.myChampionId, c);

      if (RANKED_QUEUES.has(m.queueId)) {
        const pos = myPosition(m);
        if (pos) posMap.set(pos, (posMap.get(pos) || 0) + 1);
      }
    }

    const decided = wins + losses;
    const winRate = decided ? Math.round((wins / decided) * 100) : 0;
    const avgK = total ? sumK / total : 0;
    const avgD = total ? sumD / total : 0;
    const avgA = total ? sumA / total : 0;
    const kdaRatio = avgD === 0 ? (avgK + avgA) : (avgK + avgA) / avgD;
    const killPart = kpCount ? Math.round(sumKp / kpCount) : 0;

    const topChampions = [...champMap.values()]
      .map(c => {
        const ratio = c.sumD === 0 ? (c.sumK + c.sumA) : (c.sumK + c.sumA) / c.sumD;
        return { ...c, losses: c.games - c.wins, winRate: Math.round((c.wins / c.games) * 100), kda: ratio };
      })
      .sort((a, b) => b.games - a.games || b.kda - a.kda)
      .slice(0, 3);

    const maxPos = Math.max(1, ...POSITIONS.map(p => posMap.get(p.key) || 0));

    return { total, wins, losses, winRate, avgK, avgD, avgA, kdaRatio, killPart, topChampions, posMap, maxPos };
  }, [matches]);

  return (
    <div style={{
      background: C.panel, border: `1px solid ${C.panelBorder}`,
      borderRadius: 10, padding: '16px 20px', marginBottom: 16,
    }}>
      {/* 헤더: 타이틀 + 챔피언 검색 */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        marginBottom: 14, gap: 12 }}>
        <h3 style={{ margin: 0, color: C.text, fontSize: 15, fontWeight: 700 }}>최근 게임</h3>
        <input
          value={search ?? ''}
          onChange={e => onSearchChange?.(e.target.value)}
          placeholder="챔피언 검색 (가렌, ㄱ, ㄹ ...)"
          style={{
            width: 200, maxWidth: '45%',
            background: C.inner, border: `1px solid ${C.panelBorder}`,
            borderRadius: 6, padding: '7px 12px', color: C.text, fontSize: 12, outline: 'none',
          }}
          onFocus={e => { e.target.style.borderColor = C.gold; }}
          onBlur={e => { e.target.style.borderColor = C.panelBorder; }}
        />
      </div>

      {/* 본문 3분할 — 비례 flex(1 : 1.25 : 1)로 폭을 균등 분배해 밸런스 유지 */}
      <div style={{ display: 'flex', alignItems: 'stretch' }}>

        {/* ① 종합 전적 */}
        <div style={{ flex: '1 1 0', minWidth: 0, paddingRight: 20 }}>
          <div style={{ color: C.sub, fontSize: 12, marginBottom: 10 }}>
            {stat.total}전 <span style={{ color: C.win, fontWeight: 700 }}>{stat.wins}승</span>{' '}
            <span style={{ color: C.loss, fontWeight: 700 }}>{stat.losses}패</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <WinRateDonut wins={stat.wins} losses={stat.losses} />
            <div>
              <div style={{ color: C.sub, fontSize: 13, fontWeight: 600 }}>
                {stat.avgK.toFixed(1)} <span style={{ color: C.muted }}>/</span>{' '}
                <span style={{ color: C.loss }}>{stat.avgD.toFixed(1)}</span>{' '}
                <span style={{ color: C.muted }}>/</span> {stat.avgA.toFixed(1)}
              </div>
              <div style={{ margin: '3px 0', fontSize: 18, fontWeight: 800, color: kdaColor(stat.kdaRatio) }}>
                {stat.kdaRatio.toFixed(2)}<span style={{ fontSize: 13, color: C.sub, fontWeight: 600 }}> : 1</span>
              </div>
              <div style={{ color: C.sub, fontSize: 12 }}>
                킬관여 <span style={{ color: '#e74c3c', fontWeight: 700 }}>{stat.killPart}%</span>
              </div>
            </div>
          </div>
        </div>

        {/* ② 플레이한 챔피언 */}
        <div style={{ flex: '1.25 1 0', minWidth: 0,
          borderLeft: `1px solid ${C.divider}`, paddingLeft: 24, paddingRight: 20 }}>
          <div style={{ color: C.sub, fontSize: 12, marginBottom: 12 }}>
            플레이한 챔피언 (최근 {stat.total}게임)
          </div>
          {stat.topChampions.length === 0 ? (
            <div style={{ color: C.muted, fontSize: 13 }}>데이터 없음</div>
          ) : stat.topChampions.map(c => {
            const key = championKeyById[c.id] || c.name?.replace(/[^a-zA-Z0-9]/g, '');
            return (
              <div key={c.id} style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
                {key
                  ? <img src={imgChampion(key)} alt={c.name}
                      style={{ width: 34, height: 34, borderRadius: '50%', objectFit: 'cover',
                        border: `1px solid ${C.panelBorder}`, flexShrink: 0 }}
                      onError={e => { e.target.style.visibility = 'hidden'; }} />
                  : <div style={{ width: 34, height: 34, borderRadius: '50%', background: C.divider, flexShrink: 0 }} />}
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontSize: 12 }}>
                    <span style={{ color: winRateColor(c.winRate), fontWeight: 700 }}>{c.winRate}%</span>
                    <span style={{ color: C.sub }}> ({c.wins}승 {c.losses}패)</span>
                  </div>
                  <div style={{ fontSize: 12, marginTop: 2 }}>
                    <span style={{ color: kdaColor(c.kda), fontWeight: 700 }}>{c.kda.toFixed(2)}:1</span>
                    <span style={{ color: C.sub }}> 평점</span>
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* ③ 선호 포지션 (랭크) */}
        <div style={{ flex: '1 1 0', minWidth: 0,
          borderLeft: `1px solid ${C.divider}`, paddingLeft: 24 }}>
          <div style={{ color: C.sub, fontSize: 12, marginBottom: 12 }}>선호 포지션 (랭크)</div>
          <div style={{ display: 'flex', alignItems: 'flex-end',
            justifyContent: 'space-between', height: 72, padding: '0 4px' }}>
            {POSITIONS.map(p => {
              const count = stat.posMap.get(p.key) || 0;
              const isTop = count > 0 && count === stat.maxPos;
              const barH = Math.max(4, Math.round((count / stat.maxPos) * 56));
              const color = isTop ? C.win : '#33445a';
              return (
                <div key={p.key} style={{ display: 'flex', flexDirection: 'column',
                  alignItems: 'center', gap: 7 }} title={`${p.label} ${count}게임`}>
                  <div style={{ width: 16, height: 56, background: C.inner, borderRadius: 3,
                    display: 'flex', alignItems: 'flex-end', overflow: 'hidden' }}>
                    <div style={{ width: '100%', height: barH, background: color, borderRadius: 3,
                      transition: 'height 0.4s ease' }} />
                  </div>
                  <PositionIcon pos={p.key} color={isTop ? C.win : C.sub} />
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
