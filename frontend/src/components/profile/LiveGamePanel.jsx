import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { getLiveGame } from '../../api/liveGame';
import { getSummonerSpellData, getRuneData } from '../../api/ddragon';
import { imgChampion, imgSpell, imgRune } from '../../constants/ddragon';

/*
 * 인게임(실시간) 패널 — Riot Spectator-V5 프록시 결과를 보여준다.
 *  - 게임 시작 시 확정된 정보만 존재(챔피언/스펠/룬/팀/밴). 실시간 체력·골드는 Riot이 주지 않는다.
 *  - 게임 중이 아니면 백엔드가 204를 주므로 "관전 불가" 안내를 띄운다.
 *  - 경과 시간은 gameStartTime 기준으로 1초마다 갱신한다(데이터 재호출 없이 타이머만).
 */
const QUEUE_NAME = {
  420: '솔로랭크', 440: '자유랭크', 450: '칼바람 나락',
  400: '일반(드래프트)', 430: '일반(블라인드)', 490: '빠른 대전',
  700: '격전', 900: 'URF', 1700: '아레나', 1710: '아레나', 1900: 'URF',
};
// queueId가 매핑에 없을 때 gameMode로 보정 (예: 아레나는 CHERRY로 내려오는 경우가 있음)
const MODE_NAME = {
  CHERRY: '아레나', ARAM: '칼바람 나락', CLASSIC: '일반', URF: 'URF', ARURF: 'URF',
  NEXUSBLITZ: '돌격! 넥서스', ONEFORALL: '단일 챔피언', ULTBOOK: '궁극기 주문서',
};
const queueName = (id, mode) => QUEUE_NAME[id] || MODE_NAME[mode] || mode || '게임';

// mapId → 한국어 맵 이름 (아레나=30 '번개 고리')
const MAP_NAME = { 11: '소환사의 협곡', 12: '칼바람 나락', 21: '발로란 도시 공원', 30: '번개 고리' };
const mapName = (id) => MAP_NAME[id] || '소환사의 협곡';

// 헤더 구분점 (모드 · 맵 · 시간)
const Sep = () => <span style={{ color: '#3e4a5a', margin: '0 7px', fontWeight: 400 }}>·</span>;

const cardStyle = {
  background: '#111c27', border: '1px solid #2a3a4a',
  borderRadius: 10, padding: 0, overflow: 'hidden',
};
const emptyStyle = {
  ...cardStyle, padding: '48px 0', textAlign: 'center', color: '#4a5568', fontSize: 15,
};

// 핵심룬 + 보조 계열 아이콘 매핑(룬 메타에서 추출) — MatchList와 동일 구조
function buildRuneMaps(reforged) {
  const styleIconById = {};
  const runeIconById = {};
  (reforged || []).forEach(path => {
    styleIconById[path.id] = path.icon;
    (path.slots || []).forEach(slot =>
      (slot.runes || []).forEach(r => { runeIconById[r.id] = r.icon; }));
  });
  return { styleIconById, runeIconById };
}

export default function LiveGamePanel({ puuid, championKeyById = {} }) {
  const navigate = useNavigate();
  const { region } = useParams();
  const regionLower = (region || 'kr').toLowerCase();

  const [status, setStatus] = useState('loading'); // loading | in | out | error
  const [game, setGame]     = useState(null);
  const [now, setNow]       = useState(Date.now());

  const [spellMap, setSpellMap]           = useState({});
  const [runeIconById, setRuneIconById]   = useState({});
  const [styleIconById, setStyleIconById] = useState({});

  // 스펠/룬 메타 로드 (아이콘 매핑용) — ko 실패 시 en 폴백
  useEffect(() => {
    let cancelled = false;
    (async () => {
      const load = async (locale) => {
        const [s, r] = await Promise.all([getSummonerSpellData(locale), getRuneData(locale)]);
        return [s.data, r.data];
      };
      try {
        const [sj, rj] = await load('ko_KR').catch(() => load('en_US'));
        const spells = {};
        Object.values(sj.data).forEach(s => {
          const sid = Number(s.key);
          if (!isNaN(sid)) spells[sid] = s.image.full;
        });
        const { runeIconById: runeMap, styleIconById: styleMap } = buildRuneMaps(rj);
        if (!cancelled) { setSpellMap(spells); setRuneIconById(runeMap); setStyleIconById(styleMap); }
      } catch (e) { console.error('DDragon 로드 실패', e); }
    })();
    return () => { cancelled = true; };
  }, []);

  // 인게임 정보 조회
  useEffect(() => {
    if (!puuid) return;
    let cancelled = false;
    setStatus('loading');
    setGame(null);
    (async () => {
      try {
        const res = await getLiveGame(puuid);
        if (cancelled) return;
        if (res.status === 204 || !res.data) { setStatus('out'); return; }
        setGame(res.data);
        setNow(Date.now());
        setStatus('in');
      } catch (e) {
        console.error('인게임 정보 조회 실패', e);
        if (!cancelled) setStatus('error');
      }
    })();
    return () => { cancelled = true; };
  }, [puuid]);

  // 경과 시간 1초 타이머 (인게임일 때만)
  useEffect(() => {
    if (status !== 'in') return;
    const t = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(t);
  }, [status]);

  if (status === 'loading') return <div style={emptyStyle}>인게임 정보를 불러오는 중...</div>;
  if (status === 'error')   return <div style={emptyStyle}>인게임 정보를 불러오지 못했습니다.</div>;
  if (status === 'out' || !game) return <div style={emptyStyle}>현재 인게임 중이 아닙니다.</div>;

  // 경과 시간: gameStartTime이 있으면 실시간 계산, 없으면(로딩 중) gameLength 사용
  const elapsedSec = game.gameStartTime > 0
    ? Math.max(0, Math.floor((now - game.gameStartTime) / 1000))
    : (game.gameLength || 0);
  const elapsedStr = `${Math.floor(elapsedSec / 60)}분 ${String(elapsedSec % 60).padStart(2, '0')}초`;

  const teams = [100, 200];
  const teamColor = { 100: '#5383f3', 200: '#e84057' };
  const teamLabel = { 100: '블루팀', 200: '레드팀' };

  const goToSummoner = (gameName, tagLine) => {
    if (!gameName) return;
    const slug = tagLine ? `${gameName}-${tagLine}` : gameName;
    navigate(`/find/${regionLower}/${encodeURIComponent(slug)}`);
  };

  return (
    <div style={cardStyle}>
      {/* 헤더 */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 10,
        padding: '12px 16px', borderBottom: '1px solid #2a3a4a', background: '#0f1923',
      }}>
        <span style={{
          width: 8, height: 8, borderRadius: '50%', background: '#2bb673',
          boxShadow: '0 0 8px #2bb673',
        }} />
        <span style={{ color: '#e2e8f0', fontSize: 14, fontWeight: 700, display: 'flex', alignItems: 'center' }}>
          {queueName(game.gameQueueConfigId, game.gameMode)}
          <Sep />
          {mapName(game.mapId)}
          <Sep />
          {elapsedStr}
        </span>
        <span style={{ color: '#2bb673', fontSize: 12, fontWeight: 700, marginLeft: 'auto' }}>
          진행 중
        </span>
      </div>

      {/* 밴 목록 (있을 때만) */}
      {game.bannedChampions?.length > 0 && (
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap',
          padding: '8px 16px', borderBottom: '1px solid #1a2433',
        }}>
          <span style={{ color: '#6b7a8d', fontSize: 11, fontWeight: 600 }}>밴</span>
          {game.bannedChampions.filter(b => b.championId > 0).map((b, i) => {
            const key = championKeyById[b.championId];
            return key
              ? <img key={i} src={imgChampion(key)} alt="" width={22} height={22}
                  title={key} style={{ borderRadius: 4, filter: 'grayscale(0.4)', opacity: 0.85 }} />
              : <div key={i} style={{ width: 22, height: 22, borderRadius: 4, background: '#1e2535' }} />;
          })}
        </div>
      )}

      {/* 팀별 참가자 */}
      <div style={{ display: 'flex', flexWrap: 'wrap' }}>
        {teams.map(teamId => {
          const members = (game.participants || []).filter(p => p.teamId === teamId);
          if (members.length === 0) return null;
          return (
            <div key={teamId} style={{ flex: '1 1 320px', minWidth: 300 }}>
              <div style={{
                padding: '8px 16px', fontSize: 12.5, fontWeight: 700,
                color: teamColor[teamId], borderBottom: '1px solid #1a2433',
              }}>{teamLabel[teamId]}</div>
              {members.map(p => {
                const key = championKeyById[p.championId];
                const isMe = p.puuid === puuid;
                return (
                  <div key={p.puuid} style={{
                    display: 'flex', alignItems: 'center', gap: 8,
                    padding: '8px 16px', borderBottom: '1px solid #141d28',
                    background: isMe ? 'rgba(83,131,243,0.10)' : 'transparent',
                  }}>
                    {/* 챔피언 */}
                    {key
                      ? <img src={imgChampion(key)} alt="" width={36} height={36}
                          style={{ borderRadius: '50%', border: '2px solid rgba(255,255,255,0.15)' }} />
                      : <div style={{ width: 36, height: 36, borderRadius: '50%', background: '#1e2535' }} />}

                    {/* 스펠 */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      {[p.spell1Id, p.spell2Id].map((sid, i) =>
                        sid && spellMap[sid]
                          ? <img key={i} src={imgSpell(spellMap[sid])} alt="" width={17} height={17}
                              style={{ borderRadius: 3 }} />
                          : <div key={i} style={{ width: 17, height: 17, borderRadius: 3, background: '#1a2030' }} />)}
                    </div>

                    {/* 핵심룬 + 보조 계열 */}
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'center' }}>
                      {runeIconById[p.keystoneId]
                        ? <img src={imgRune(runeIconById[p.keystoneId])} alt="" width={20} height={20}
                            style={{ borderRadius: '50%', background: '#0a0c14' }} />
                        : <div style={{ width: 20, height: 20, borderRadius: '50%', background: 'rgba(255,255,255,0.05)' }} />}
                      {styleIconById[p.subStyleId]
                        ? <img src={imgRune(styleIconById[p.subStyleId])} alt="" width={13} height={13} />
                        : <div style={{ width: 13, height: 13, borderRadius: '50%', background: 'rgba(255,255,255,0.05)' }} />}
                    </div>

                    {/* 이름 (클릭 시 해당 소환사 페이지로) */}
                    <span
                      onClick={() => goToSummoner(p.gameName, p.tagLine)}
                      title={`${p.gameName}#${p.tagLine}`}
                      style={{
                        color: isMe ? '#fff' : '#c8d0e0', fontSize: 13,
                        fontWeight: isMe ? 700 : 500, cursor: 'pointer',
                        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                      }}
                      onMouseEnter={e => { e.currentTarget.style.textDecoration = 'underline'; }}
                      onMouseLeave={e => { e.currentTarget.style.textDecoration = 'none'; }}
                    >
                      {p.gameName}
                      <span style={{ color: '#3e4a5a', fontWeight: 400, fontSize: 11 }}>#{p.tagLine}</span>
                    </span>
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>
    </div>
  );
}
