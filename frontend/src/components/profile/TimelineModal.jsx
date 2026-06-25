import React, { useEffect, useMemo, useState } from 'react';
import { createPortal } from 'react-dom';
import { imgChampion } from '../../constants/ddragon';
import HoverTip from '../common/HoverTip';
import { getMatchTimeline } from '../../api/match';

/*
 * 타임라인 모달 — 맵(이벤트 위치) + 이벤트 피드 + 분당 골드 차이 그래프.
 *  - 데이터: GET /api/matches/{matchId}/timeline (열 때 즉석 조회).
 *  - props: matchId, winTeamId, championKeyById, championNameById, onClose
 */
const MAP_MAX = 15000; // Riot 맵 좌표 대략 0~15000

const MONSTER_KO = {
  DRAGON: '드래곤', ELDER_DRAGON: '장로 드래곤', RIFTHERALD: '전령',
  BARON_NASHOR: '바론', HORDE: '공허 유충', ATAKHAN: '아타칸',
};
const monsterName = (e) => MONSTER_KO[e.monsterType] || '오브젝트';
const buildingName = (e) => (e.buildingType === 'INHIBITOR_BUILDING' ? '억제기' : '포탑');

const fmtClock = (ms) => {
  const s = Math.floor(ms / 1000);
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
};

export default function TimelineModal({ matchId, winTeamId, championKeyById = {}, championNameById = {}, onClose }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState(false);
  const [showKill, setShowKill] = useState(true);
  const [showTower, setShowTower] = useState(true);
  const [atMinute, setAtMinute] = useState(99);

  useEffect(() => {
    let cancelled = false;
    setData(null); setError(false);
    getMatchTimeline(matchId)
      .then(res => { if (!cancelled) setData(res.data); })
      .catch(() => { if (!cancelled) setError(true); });
    return () => { cancelled = true; };
  }, [matchId]);

  // ESC 닫기
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onClose?.(); };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const pidMap = useMemo(() => {
    const m = {};
    (data?.participants || []).forEach(p => { m[p.participantId] = p; });
    return m;
  }, [data]);

  const maxMinute = data?.goldFrames?.length ? data.goldFrames.length - 1 : 0;
  const effMinute = Math.min(atMinute, maxMinute);

  const champImg = (pid) => {
    const p = pidMap[pid];
    const key = p && championKeyById[p.championId];
    return key ? imgChampion(key) : null;
  };
  const champKo = (pid) => {
    const p = pidMap[pid];
    return (p && (championNameById[p.championId] || p.championName)) || '';
  };
  const teamColor = (teamId) => (teamId === 100 ? '#4a8fe7' : '#e84057');

  return createPortal(
    <div onClick={onClose} style={{
      position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.72)', zIndex: 10000,
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16,
      fontFamily: 'Pretendard, "Apple SD Gothic Neo", -apple-system, sans-serif',
    }}>
      <div onClick={e => e.stopPropagation()} style={{
        width: 'min(1120px, 96vw)', maxHeight: '94vh', overflow: 'auto',
        background: '#15171c', border: '1px solid #2a2f3a', borderRadius: 10, color: '#e2e8f0',
      }}>
        {/* 헤더 */}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '12px 16px', borderBottom: '1px solid #2a2f3a' }}>
          <span style={{ fontSize: 15, fontWeight: 800 }}>타임라인</span>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#9aa7b4',
            fontSize: 20, cursor: 'pointer', lineHeight: 1 }}>×</button>
        </div>

        {!data && !error && <div style={{ padding: 40, textAlign: 'center', color: '#7a8a9e' }}>불러오는 중…</div>}
        {error && <div style={{ padding: 40, textAlign: 'center', color: '#7a8a9e' }}>타임라인을 불러오지 못했습니다.</div>}

        {data && (
          <div style={{ padding: 16 }}>
            {/* 팀 로스터 + 필터 */}
            <RosterBar data={data} winTeamId={winTeamId} champImg={champImg} champKo={champKo}
              showKill={showKill} setShowKill={setShowKill} showTower={showTower} setShowTower={setShowTower} />

            {/* 맵 + 이벤트 피드 */}
            <div style={{ display: 'flex', gap: 12, marginTop: 14, flexWrap: 'wrap' }}>
              <EventMap data={data} effMinute={effMinute} showKill={showKill} showTower={showTower}
                teamColor={teamColor} pidMap={pidMap} />
              <EventFeed data={data} champImg={champImg} champKo={champKo} teamColor={teamColor} pidMap={pidMap} />
            </div>

            {/* 골드 차이 그래프 + 시간 슬라이더 */}
            <GoldGraph data={data} effMinute={effMinute} />
            <div style={{ marginTop: 6 }}>
              <input type="range" min={0} max={maxMinute} value={effMinute}
                onChange={e => setAtMinute(Number(e.target.value))}
                style={{ width: '100%' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', color: '#7a8a9e', fontSize: 11 }}>
                <span>0m</span><span>{effMinute}m</span><span>{maxMinute}m</span>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>,
    document.body,
  );
}

function RosterBar({ data, winTeamId, champImg, champKo, showKill, setShowKill, showTower, setShowTower }) {
  const blue = data.participants.filter(p => p.teamId === 100);
  const red = data.participants.filter(p => p.teamId === 200);
  const label = (teamId) => {
    const side = teamId === 100 ? '블루팀' : '레드팀';
    if (winTeamId == null) return side;
    return `${teamId === winTeamId ? '승리팀' : '패배팀'} (${side})`;
  };
  const teamIcons = (rows, color) => (
    <div>
      <div style={{ color: '#9aa7b4', fontSize: 12, marginBottom: 6 }}>{label(rows[0]?.teamId)}</div>
      <div style={{ display: 'flex', gap: 6 }}>
        {rows.map(p => {
          const src = champImg(p.participantId);
          return (
            <HoverTip key={p.participantId} label={champKo(p.participantId)}>
              {src
                ? <img src={src} alt="" width={34} height={34}
                    style={{ borderRadius: 6, border: `2px solid ${color}`, background: '#0d1520' }} />
                : <span style={{ width: 34, height: 34, borderRadius: 6, background: '#0d1520', display: 'inline-block' }} />}
            </HoverTip>
          );
        })}
      </div>
    </div>
  );
  const check = (on, set, text) => (
    <label style={{ display: 'flex', alignItems: 'center', gap: 5, cursor: 'pointer', color: '#cdd6e2', fontSize: 13 }}>
      <input type="checkbox" checked={on} onChange={e => set(e.target.checked)} />{text}
    </label>
  );
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
      <div style={{ display: 'flex', gap: 28, flexWrap: 'wrap' }}>
        {teamIcons(blue, '#4a8fe7')}
        {teamIcons(red, '#e84057')}
      </div>
      <div style={{ display: 'flex', gap: 14 }}>
        {check(showKill, setShowKill, '킬')}
        {check(showTower, setShowTower, '타워')}
      </div>
    </div>
  );
}

function EventMap({ data, effMinute, showKill, showTower, teamColor, pidMap }) {
  const size = 440;
  const cutoff = (effMinute + 1) * 60000; // 선택 분까지 발생한 이벤트
  const markers = data.events.filter(e => {
    if (e.timestamp > cutoff) return false;
    if (e.x == null || e.y == null) return false;
    if (e.category === 'CHAMPION_KILL') return showKill;
    if (e.category === 'BUILDING_KILL') return showTower;
    return true; // 엘리트 몬스터는 항상 표시
  });
  const px = (x) => (x / MAP_MAX) * size;
  const py = (y) => (1 - y / MAP_MAX) * size; // Riot y는 위로 증가 → 화면은 반전

  return (
    <div style={{
      width: size, height: size, flexShrink: 0, position: 'relative',
      background: 'radial-gradient(circle at 30% 70%, #18324a 0%, #0e1726 60%, #0a0f1a 100%)',
      border: '1px solid #243246', borderRadius: 8, overflow: 'hidden',
    }}>
      {/* 대각선(중앙 강) 가이드 */}
      <div style={{ position: 'absolute', left: 0, top: 0, width: '141%', height: 1,
        background: 'rgba(255,255,255,0.06)', transform: 'rotate(45deg)', transformOrigin: 'top left' }} />
      {markers.map((e, i) => {
        const left = px(e.x), top = py(e.y);
        if (e.category === 'CHAMPION_KILL') {
          const color = teamColor(pidMap[e.killerId]?.teamId);
          return <span key={i} title={fmtClock(e.timestamp)} style={{
            position: 'absolute', left: left - 4, top: top - 4, width: 8, height: 8,
            borderRadius: '50%', background: color, border: '1px solid rgba(0,0,0,0.5)' }} />;
        }
        if (e.category === 'BUILDING_KILL') {
          return <span key={i} title={`${fmtClock(e.timestamp)} 포탑`} style={{
            position: 'absolute', left: left - 4, top: top - 4, width: 8, height: 8,
            background: teamColor(e.teamId === 100 ? 200 : 100), border: '1px solid #000' }} />;
        }
        // 엘리트 몬스터 = 마름모
        return <span key={i} title={`${fmtClock(e.timestamp)} ${MONSTER_KO[e.monsterType] || ''}`} style={{
          position: 'absolute', left: left - 5, top: top - 5, width: 10, height: 10,
          background: '#f0a800', transform: 'rotate(45deg)', border: '1px solid #000' }} />;
      })}
    </div>
  );
}

function EventFeed({ data, champImg, champKo, teamColor, pidMap }) {
  let lastMinute = -1;
  const champChip = (pid) => {
    const src = champImg(pid);
    return (
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
        {src && <img src={src} alt="" width={18} height={18} style={{ borderRadius: 4 }} />}
        <span style={{ color: '#cdd6e2' }}>{champKo(pid)}</span>
      </span>
    );
  };
  return (
    <div style={{ flex: 1, minWidth: 320, maxHeight: 440, overflowY: 'auto',
      border: '1px solid #243246', borderRadius: 8, background: '#11141b' }}>
      {data.events.map((e, i) => {
        const minute = Math.floor(e.timestamp / 60000);
        const showMin = minute !== lastMinute; lastMinute = minute;
        const accent = e.category === 'ELITE_MONSTER_KILL'
          ? '#f0a800'
          : e.category === 'BUILDING_KILL'
            ? teamColor(e.teamId === 100 ? 200 : 100)
            : teamColor(pidMap[e.killerId]?.teamId);
        return (
          <div key={i} style={{ display: 'flex', borderBottom: '1px solid #1a212d' }}>
            <div style={{ width: 44, flexShrink: 0, padding: '8px 6px', color: '#7a8a9e', fontSize: 11,
              borderRight: '1px solid #1a212d', textAlign: 'center' }}>
              {showMin ? `${minute}분` : ''}
            </div>
            <div style={{ flex: 1, padding: '8px 10px', borderLeft: `3px solid ${accent || '#3a4252'}`,
              fontSize: 12.5, display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
              <span style={{ color: '#7a8a9e', fontSize: 11 }}>{fmtClock(e.timestamp)}</span>
              {e.category === 'CHAMPION_KILL' && (
                <>{champChip(e.killerId)}<span style={{ color: '#5a8fe7' }}>처치</span>{champChip(e.victimId)}</>
              )}
              {e.category === 'ELITE_MONSTER_KILL' && (
                <>{champChip(e.killerId)}<span style={{ color: '#5a8fe7' }}>처치</span>
                  <span style={{ color: '#f0a800', fontWeight: 700 }}>{MONSTER_KO[e.monsterType] || '오브젝트'}</span></>
              )}
              {e.category === 'BUILDING_KILL' && (
                <>{e.killerId ? champChip(e.killerId) : <span style={{ color: '#9aa7b4' }}>미니언</span>}
                  <span style={{ color: '#5a8fe7' }}>처치</span>
                  <span style={{ color: '#cdd6e2' }}>{buildingName(e)}</span></>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}

function GoldGraph({ data, effMinute }) {
  const frames = data.goldFrames || [];
  const w = 1080, h = 120, pad = 24;
  const diffs = frames.map(f => f.blueGold - f.redGold);
  const maxAbs = Math.max(1000, ...diffs.map(d => Math.abs(d)));
  const x = (i) => pad + (frames.length <= 1 ? 0 : (i / (frames.length - 1)) * (w - pad * 2));
  const y = (d) => h / 2 - (d / maxAbs) * (h / 2 - 10);
  const pts = diffs.map((d, i) => `${x(i)},${y(d)}`).join(' ');

  return (
    <div style={{ marginTop: 14 }}>
      <div style={{ color: '#9aa7b4', fontSize: 12, marginBottom: 4 }}>골드 차이 (블루 − 레드)</div>
      <svg viewBox={`0 0 ${w} ${h}`} style={{ width: '100%', height: 'auto', display: 'block' }}>
        <line x1={pad} y1={h / 2} x2={w - pad} y2={h / 2} stroke="#2a3140" strokeWidth="1" />
        <text x="2" y="12" fill="#5a6b7e" fontSize="10">+{Math.round(maxAbs / 1000)}k</text>
        <text x="2" y={h - 2} fill="#5a6b7e" fontSize="10">-{Math.round(maxAbs / 1000)}k</text>
        {frames.length > 1 && <polyline points={pts} fill="none" stroke="#5a6678" strokeWidth="1.5" />}
        {diffs.map((d, i) => (
          <circle key={i} cx={x(i)} cy={y(d)} r={i === effMinute ? 4.5 : 3}
            fill={d >= 0 ? '#4a8fe7' : '#e84057'} stroke={i === effMinute ? '#fff' : 'none'} strokeWidth="1" />
        ))}
      </svg>
    </div>
  );
}
