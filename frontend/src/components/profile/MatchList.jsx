import React, { useState, useEffect } from 'react';
import { getMatchSummary } from '../../api/match';
import { imgChampion, imgItem, imgSpell, imgObjective, imgRune } from '../../constants/ddragon';
import { getSummonerSpellData, getChampionData, getRuneData } from '../../api/ddragon';

/* ═══════════════════════════════════════════════════════════════
   디자인 토큰
   - 전체 배경: #13131b
   - 팀 컨테이너: rgba(255,255,255,0.03) 레이어링
   - 텍스트 위계: 소환사명 #fff / KDA평점 #00d3ab(민트) / 서브 #7a8a9e / 뮤트 #3e4a5a
   - 아이템슬롯: 32×32, border rgba(255,255,255,0.08)
   - row min-height: 62px
════════════════════════════════════════════════════════════════ */
const T = {
  bg:           '#13131b',
  layer:        'rgba(255,255,255,0.03)',
  layerHover:   'rgba(255,255,255,0.05)',
  blueHead:     'rgba(83,131,243,0.12)',
  redHead:      'rgba(232,64,87,0.12)',
  blueLine:     '#5383f3',
  redLine:      '#e84057',
  divider:      '#0d0d14',
  border:       'rgba(255,255,255,0.07)',
  borderStrong: 'rgba(255,255,255,0.13)',
  /* 텍스트 */
  txtName:      '#ffffff',
  txtPrimary:   '#c8d0e0',
  txtSub:       '#7a8a9e',
  txtMuted:     '#3e4a5a',
  txtKills:     '#ffffff',
  txtDeaths:    '#e84057',
  txtAssists:   '#c8d0e0',
  /* 강조 */
  mint:         '#00d3ab',
  gold:         '#f0a800',
  blue:         '#5383f3',
  red:          '#e84057',
  mvpBg:        '#f7a600',
  aceBg:        '#7a5ccc',
};

/* ═══════════════════════════════════════════════════════════════
   챔피언 아이콘
════════════════════════════════════════════════════════════════ */
function ChampionIcon({ championId, championKeyById, championName, size = 40 }) {
  const key = championKeyById[championId] ||
    (championName ? championName.replace(/[^a-zA-Z0-9]/g, '') : null);
  const baseStyle = {
    width: size, height: size, borderRadius: '50%', flexShrink: 0,
    objectFit: 'cover', border: '2px solid rgba(255,255,255,0.15)',
  };
  if (!key) return <div style={{ ...baseStyle, background: '#1e2535' }} />;
  return (
    <img src={imgChampion(key)} alt={championName || key} style={baseStyle}
      onError={e => { e.target.style.visibility = 'hidden'; }} />
  );
}

/* ═══════════════════════════════════════════════════════════════
   소환사 주문 아이콘 (2개 세로)
════════════════════════════════════════════════════════════════ */
function SpellIcons({ spell1, spell2, spellMap }) {
  const sz = { width: 20, height: 20, borderRadius: 4, flexShrink: 0,
    border: '1px solid rgba(255,255,255,0.1)' };
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flexShrink: 0 }}>
      {[spell1, spell2].map((sid, i) =>
        sid && spellMap[sid]
          ? <img key={i} src={imgSpell(spellMap[sid])} alt="" style={sz} />
          : <div key={i} style={{ ...sz, background: '#1a2030' }} />
      )}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   룬 아이콘 (핵심룬 + 보조 계열, 2개 세로)
   - runeIconById:  perk id → 아이콘 경로 (핵심룬)
   - styleIconById: style id → 아이콘 경로 (보조 계열 트리)
════════════════════════════════════════════════════════════════ */
function buildRuneMaps(reforged) {
  const styleIconById = {};
  const runeIconById = {};
  (reforged || []).forEach(path => {
    styleIconById[path.id] = path.icon;
    (path.slots || []).forEach(slot =>
      (slot.runes || []).forEach(r => { runeIconById[r.id] = r.icon; })
    );
  });
  return { styleIconById, runeIconById };
}

function KeystoneRunes({ keystoneId, subStyleId, runeIconById, styleIconById }) {
  const keystone = runeIconById[keystoneId];
  const subTree = styleIconById[subStyleId];
  const placeholder = (size) => ({
    width: size, height: size, borderRadius: '50%', flexShrink: 0,
    background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.06)',
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'center', flexShrink: 0 }}>
      {keystone
        ? <img src={imgRune(keystone)} alt="" style={{ width: 24, height: 24, borderRadius: '50%', background: '#0a0c14' }} />
        : <div style={placeholder(24)} />}
      {subTree
        ? <img src={imgRune(subTree)} alt="" style={{ width: 15, height: 15 }} />
        : <div style={placeholder(15)} />}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   룬 파편 아이콘 (3개 세로)
════════════════════════════════════════════════════════════════ */
const PERK_ICON_MAP = {
  5008: 'perk-images/StatMods/StatModsAdaptiveForceIcon.png',
  5005: 'perk-images/StatMods/StatModsAttackSpeedIcon.png',
  5007: 'perk-images/StatMods/StatModsCDRScalingIcon.png',
  5002: 'perk-images/StatMods/StatModsArmorIcon.png',
  5003: 'perk-images/StatMods/StatModsMagicResIcon.MagicResist_fix.png',
  5001: 'perk-images/StatMods/StatModsHealthScalingIcon.png',
};
const PERK_BASE = 'https://ddragon.leagueoflegends.com/cdn/img/';

function RuneIcons({ off, flex, def }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flexShrink: 0 }}>
      {[off, flex, def].map((id, i) =>
        id && PERK_ICON_MAP[id]
          ? <img key={i} src={`${PERK_BASE}${PERK_ICON_MAP[id]}`} alt=""
              style={{ width: 15, height: 15, borderRadius: 2, opacity: 0.8 }} />
          : <div key={i} style={{ width: 15, height: 15, borderRadius: 2,
              background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.06)' }} />
      )}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   아이템 슬롯: 32×32, border rgba(255,255,255,0.08)
════════════════════════════════════════════════════════════════ */
function ItemSlots({ itemIds = [] }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'nowrap' }}>
      {itemIds.map((id, i) => {
        const slotStyle = {
          width: 32, height: 32, borderRadius: 4, flexShrink: 0,
          border: '1px solid rgba(255,255,255,0.08)',
          objectFit: 'cover',
        };
        return id > 0
          ? <img key={i} src={imgItem(id)} alt="" title={`item ${id}`} style={slotStyle} />
          : <div key={i} style={{ ...slotStyle, background: 'rgba(255,255,255,0.03)' }} />;
      })}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   MVP / ACE 뱃지
════════════════════════════════════════════════════════════════ */
function Badge({ type }) {
  if (!type) return null;
  const cfg = type === 'MVP'
    ? { bg: T.mvpBg, label: 'MVP' }
    : { bg: T.aceBg, label: 'ACE' };
  return (
    <span style={{
      display: 'inline-block',
      background: cfg.bg,
      color: '#fff',
      fontSize: 10,
      fontWeight: 800,
      padding: '1px 6px',
      borderRadius: 3,
      letterSpacing: 0.5,
      marginLeft: 4,
      verticalAlign: 'middle',
      lineHeight: '16px',
    }}>{cfg.label}</span>
  );
}

/* ═══════════════════════════════════════════════════════════════
   티어 뱃지 (해당 큐 기준 소환사 랭크)
   - 마스터+ 는 단계(rank) 표기 없음
════════════════════════════════════════════════════════════════ */
const TIER_APEX = new Set(['MASTER', 'GRANDMASTER', 'CHALLENGER']);
function TierBadge({ tier, rank }) {
  if (!tier) return null;
  const name = tier.charAt(0) + tier.slice(1).toLowerCase();
  const text = TIER_APEX.has(tier) ? name : `${name} ${rank ?? ''}`.trim();
  return (
    <span style={{
      display: 'inline-flex', alignItems: 'center', gap: 3,
      marginTop: 3, fontSize: 10, fontWeight: 700,
      color: T.gold,
    }}>
      <span style={{ fontSize: 9 }}>🔥</span>{text}
    </span>
  );
}

/* ═══════════════════════════════════════════════════════════════
   피해량 그래프
   - 딜(빨강) + 탱(파랑) 두 줄
   - 수치: 좌측 수치 표시, 바는 flex 비율
════════════════════════════════════════════════════════════════ */
function DamageGraph({ dealt, taken, maxDealt, maxTaken }) {
  const dp = maxDealt > 0 ? Math.max(3, Math.round((dealt / maxDealt) * 100)) : 0;
  const tp = maxTaken > 0 ? Math.max(3, Math.round((taken / maxTaken) * 100)) : 0;
  const fmtK = v => v >= 1000 ? Math.round(v / 100) / 10 + 'k' : String(v);

  return (
    <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: 4 }}>
      {/* 딜 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{
          width: 34, textAlign: 'right', flexShrink: 0,
          fontSize: 11, fontWeight: 700, color: T.txtPrimary,
          fontVariantNumeric: 'tabular-nums',
        }}>{fmtK(dealt)}</span>
        <div style={{ flex: 1, height: 7, background: 'rgba(255,255,255,0.05)',
          borderRadius: 3, overflow: 'hidden' }}>
          <div style={{ width: `${dp}%`, height: '100%',
            background: 'linear-gradient(90deg,#c8203a,#e84057)', borderRadius: 3 }} />
        </div>
      </div>
      {/* 탱 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{
          width: 34, textAlign: 'right', flexShrink: 0,
          fontSize: 11, color: T.txtSub,
          fontVariantNumeric: 'tabular-nums',
        }}>{fmtK(taken)}</span>
        <div style={{ flex: 1, height: 7, background: 'rgba(255,255,255,0.05)',
          borderRadius: 3, overflow: 'hidden' }}>
          <div style={{ width: `${tp}%`, height: '100%',
            background: 'linear-gradient(90deg,#2a4a9e,#5383f3)', borderRadius: 3 }} />
        </div>
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   queueId → 게임모드 한글 매핑
════════════════════════════════════════════════════════════════ */
const QUEUE_NAME = {
  420: '솔로랭크', 440: '자유랭크', 450: '칼바람',
  900: 'URF', 1020: '단일챔피언', 1300: '전쟁영웅',
  1700: '아레나', 1701: '아레나', 1900: 'URF',
  400: '일반', 430: '일반(블라인드)',
};
const getQueueName = (queueId, gameMode) =>
  QUEUE_NAME[queueId] || gameMode || '일반';
/* ═══════════════════════════════════════════════════════════════
   팀 구분선 — 이미지 기준: 승리팀(좌) vs 패배팀(우) 오브젝트 + 양방향 바
   winRows = 승리팀 rows, loseRows = 패배팀 rows
════════════════════════════════════════════════════════════════ */
const OBJ_KEYS = ['voidGrub', 'herald', 'dragon', 'baron', 'tower', 'inhibitor'];
const OBJ_EMOJI = { voidGrub:'🪲', herald:'👁', dragon:'🐉', baron:'🟣', tower:'🗼', inhibitor:'💠' };
const OBJ_LABEL = { voidGrub:'공허 유충 처치', herald:'협곡의 전령 처치', dragon:'드래곤 처치', baron:'내셔 남작 처치', tower:'포탑 파괴', inhibitor:'억제기 파괴' };
// matchObj 필드명 매핑 (riftHerald 등 백엔드 필드명 그대로)
const OBJ_FIELD = { voidGrub: 'Horde', herald:'RiftHerald', dragon:'Dragon', baron:'Baron', tower:'Tower', inhibitor:'Inhibitor' };

function TeamDivider({ winRows, loseRows, matchObj, blueIsWin }) {
  const wKills = winRows.reduce((s, r)  => s + r.kills, 0);
  const lKills = loseRows.reduce((s, r) => s + r.kills, 0);
  const wGold  = winRows.reduce((s, r)  => s + r.goldEarned, 0);
  const lGold  = loseRows.reduce((s, r) => s + r.goldEarned, 0);
  const totK   = wKills + lKills || 1;
  const totG   = wGold  + lGold  || 1;
  const wKPct  = Math.round((wKills / totK) * 100);
  const wGPct  = Math.round((wGold  / totG) * 100);

  /* 오브젝트: 승리팀이 블루(100)면 blue* 필드, 아니면 red* 필드 */
  const winPfx   = blueIsWin ? 'blue' : 'red';
  const losePfx  = blueIsWin ? 'red'  : 'blue';
  const winTeamId  = blueIsWin ? 100 : 200;
  const loseTeamId = blueIsWin ? 200 : 100;
  const cap = s => s.charAt(0).toUpperCase() + s.slice(1);

  /* 양방향 바 */
  const BiBar = ({ wPct, wVal, lVal, label }) => (
    <div style={{ display:'flex', alignItems:'center', marginBottom: 5 }}>
      {/* 승리(파랑) 숫자 */}
      <span style={{ color: T.blue, fontWeight:700, fontSize:13,
        width:80, textAlign:'right', paddingRight:10, flexShrink:0 }}>{wVal}</span>
      {/* 바 */}
      <div style={{ flex:1, display:'flex', height:22, borderRadius:3, overflow:'hidden' }}>
        {/* 승리 바 (파랑, 오른쪽 정렬) */}
        <div style={{ width:`${wPct}%`, background: T.blue, minWidth:10,
          display:'flex', alignItems:'center', justifyContent:'flex-end', paddingRight:6 }}>
          {wPct >= 15 && <span style={{ fontSize:11, color:'#fff', fontWeight:700 }}>{wPct}%</span>}
        </div>
        {/* 중앙 라벨 */}
        <div style={{ background:'#1a1f2e', flexShrink:0,
          display:'flex', alignItems:'center', padding:'0 14px' }}>
          <span style={{ fontSize:11, color:T.txtSub, fontWeight:600,
            whiteSpace:'nowrap' }}>{label}</span>
        </div>
        {/* 패배 바 (빨강) */}
        <div style={{ flex:1, background: T.red, minWidth:10,
          display:'flex', alignItems:'center', justifyContent:'flex-start', paddingLeft:6 }}>
          {(100-wPct) >= 15 &&
            <span style={{ fontSize:11, color:'#fff', fontWeight:700 }}>{100-wPct}%</span>}
        </div>
      </div>
      {/* 패배(빨강) 숫자 */}
      <span style={{ color: T.red, fontWeight:700, fontSize:13,
        width:80, paddingLeft:10, flexShrink:0 }}>{lVal}</span>
    </div>
  );

  return (
    <div style={{ background:'#0d1018', padding:'10px 16px',
      borderTop:`1px solid ${T.border}`, borderBottom:`1px solid ${T.border}` }}>

      {/* 오브젝트 행 */}
      {matchObj && (
        <div style={{ display:'flex', alignItems:'center',
          justifyContent:'center', gap:6, marginBottom:10 }}>
          {/* 승리팀 오브젝트 */}
          <div style={{ display:'flex', gap:10 }}>
            {OBJ_KEYS.map(k => (
              <div key={`w-${k}`} title={OBJ_LABEL[k]}
                style={{ display:'flex', alignItems:'center', gap:4, cursor:'default' }}>
                <img
                  src={imgObjective(k, winTeamId)}
                  alt={OBJ_LABEL[k]}
                  style={{ width:18, height:18, objectFit:'contain' }}
                  onError={e => { e.target.style.display='none'; }}
                />
                <span style={{ fontSize:13, color:T.blue, fontWeight:800 }}>
                  {OBJ_FIELD[k] === null ? '?' : (matchObj[`${winPfx}${OBJ_FIELD[k]}Kills`] ?? 0)}
                </span>
              </div>
            ))}
          </div>
          {/* 구분 */}
          <div style={{ width:1, height:20, background:T.border, margin:'0 8px' }} />
          {/* 패배팀 오브젝트 */}
          <div style={{ display:'flex', gap:10 }}>
            {OBJ_KEYS.map(k => (
              <div key={`l-${k}`} title={OBJ_LABEL[k]}
                style={{ display:'flex', alignItems:'center', gap:4, cursor:'default' }}>
                <img
                  src={imgObjective(k, loseTeamId)}
                  alt={OBJ_LABEL[k]}
                  style={{ width:18, height:18, objectFit:'contain' }}
                  onError={e => { e.target.style.display='none'; }}
                />
                <span style={{ fontSize:13, color:T.red, fontWeight:800 }}>
                  {OBJ_FIELD[k] === null ? '?' : (matchObj[`${losePfx}${OBJ_FIELD[k]}Kills`] ?? 0)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      <BiBar wPct={wKPct} wVal={wKills} lVal={lKills} label="Total Kill" />
      <BiBar wPct={wGPct} wVal={wGold.toLocaleString()} lVal={lGold.toLocaleString()} label="Total Gold" />
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   그리드 컬럼 정의
   플레이어 | KDA | 피해량 | 와드 | CS | 아이템(7슬롯×32+gap)
════════════════════════════════════════════════════════════════ */
const GRID = '1fr 120px 175px 60px 72px 242px';

/* ═══════════════════════════════════════════════════════════════
   팀 헤더 (패배/승리 라벨 + 컬럼명)
════════════════════════════════════════════════════════════════ */
function TeamHeader({ label, accentColor, teamSide }) {
  return (
    <div style={{
      display: 'grid', gridTemplateColumns: GRID,
      alignItems: 'center', padding: '8px 16px',
      background: 'transparent',
      borderBottom: `1px solid ${T.border}`,
    }}>
      <div style={{ color: '#ffffff', fontWeight: 800, fontSize: 13 }}>{label}</div>
      {['KDA', '피해량', '와드', 'CS', '아이템'].map(h => (
        <div key={h} style={{ color: 'rgba(255,255,255,0.35)', fontSize: 11, fontWeight: 600,
          textAlign: 'center' }}>{h}</div>
      ))}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   플레이어 행  (min-height 62px)
════════════════════════════════════════════════════════════════ */
function PlayerRow({ row, championKeyById, spellMap, maxDealt, maxTaken,
  teamSide, isMe, gameDuration, isWin }) {

  const items = [row.item0, row.item1, row.item2, row.item3,
                 row.item4, row.item5, row.item6];

  /* KDA 계산 */
  const ratio = row.deaths === 0
    ? Infinity : (row.kills + row.assists) / row.deaths;
  const kdaStr = ratio === Infinity ? 'Perfect' : ratio.toFixed(2) + ':1';
  /* 소수부 분리 */
  const [kdaInt, kdaDec] = ratio === Infinity
    ? ['Perfect', ''] : kdaStr.split('.');

  /* KDA 색상: 5.0+ 골드, 3.0+ 민트, 그 외 서브 */
  const kdaColor = ratio === Infinity || ratio >= 5 ? T.gold
    : ratio >= 3 ? T.mint : T.txtSub;

  /* 킬 기여율 */
  const teamKills = (teamSide === 'blue'
    ? /* 같은 팀 kills 합산은 외부에서 넘겨줘야 정확하나, 단순 표시용 */ 1 : 1);
  const killPct = row.kills + row.deaths + row.assists > 0
    ? Math.round(row.kills / (row.kills + row.deaths + row.assists) * 100) : 0;

  /* CS */
  const cs = (row.totalMinionsKilled ?? 0) + (row.neutralMinionsKilled ?? 0);
  const csPerMin = gameDuration > 0
    ? (cs / (gameDuration / 60)).toFixed(1) : null;

  /* MVP/ACE 뱃지 제거 */

  /* 배경 — 승리/패배 기준, 내 행은 살짝 더 진하게 */
  const rowBg = isMe
    ? (isWin ? 'rgba(83,130,243,0.20)' : 'rgba(235,87,87,0.18)')
    : (isWin ? 'rgba(83,130,243,0.08)' : 'rgba(235,87,87,0.08)');

  return (
    <div style={{
      display: 'grid', gridTemplateColumns: GRID,
      alignItems: 'center', minHeight: 62,
      padding: '0 16px',
      background: rowBg,
      borderBottom: `1px solid ${T.border}`,
    }}>

      {/* ① 플레이어: 챔프 + 스펠 + 파편 + 이름/챔피언명 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 7, minWidth: 0 }}>
        {/* 챔프 + 레벨 뱃지 */}
        <div style={{ position: 'relative', flexShrink: 0 }}>
          <ChampionIcon championId={row.championId} championKeyById={championKeyById}
            championName={row.championName} size={40} />
          <span style={{
            position: 'absolute', right: -3, bottom: -3,
            background: '#0a0c14', color: '#b0bbd0',
            fontSize: 9, fontWeight: 800, padding: '0 3px',
            borderRadius: 3, border: '1px solid rgba(255,255,255,0.12)',
            lineHeight: '15px', minWidth: 15, textAlign: 'center',
          }}>{row.championLevel}</span>
        </div>

        {/* 스펠 */}
        <SpellIcons spell1={row.spell1} spell2={row.spell2} spellMap={spellMap} />

        {/* 파편 */}
        <RuneIcons off={row.statPerkOffense} flex={row.statPerkFlex} def={row.statPerkDefense} />

        {/* 이름 + 챔피언 */}
        <div style={{ minWidth: 0 }}>
          <div style={{
            color: T.txtName, fontSize: 13, fontWeight: isMe ? 700 : 500,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
          }}>
            {row.gameName}
            <span style={{ color: T.txtMuted, fontWeight: 400, fontSize: 11 }}>
              #{row.tagLine}
            </span>
          </div>
          <div style={{ color: T.txtSub, fontSize: 11, marginTop: 2 }}>
            {row.championName}
          </div>
        </div>
      </div>

      {/* ② KDA */}
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 13, fontWeight: 700, letterSpacing: 0.2 }}>
          <span style={{ color: T.txtKills }}>{row.kills}</span>
          <span style={{ color: T.txtMuted }}> / </span>
          <span style={{ color: T.txtDeaths }}>{row.deaths}</span>
          <span style={{ color: T.txtMuted }}> / </span>
          <span style={{ color: T.txtAssists }}>{row.assists}</span>
        </div>
        <div style={{ fontSize: 11, marginTop: 2 }}>
          {ratio !== Infinity ? (
            <>
              <span style={{ color: kdaColor, fontWeight: 700 }}>{kdaInt}</span>
              {kdaDec && (
                <span style={{ color: kdaColor, fontWeight: 400 }}>.{kdaDec.replace(':1','')}</span>
              )}
              <span style={{ color: kdaColor, fontWeight: 400 }}>:1</span>
            </>
          ) : (
            <span style={{ color: T.gold, fontWeight: 700 }}>Perfect</span>
          )}
          <span style={{ color: T.txtMuted, fontSize: 10 }}> ({killPct}%)</span>
        </div>
      </div>

      {/* ③ 피해량 */}
      <div style={{ padding: '0 6px' }}>
        <DamageGraph
          dealt={row.totalDamageDealtToChampions ?? 0}
          taken={row.totalDamageTaken ?? 0}
          maxDealt={maxDealt} maxTaken={maxTaken}
        />
      </div>

      {/* ④ 와드 */}
      <div style={{ textAlign: 'center' }}>
        <div style={{ color: T.txtPrimary, fontSize: 13, fontWeight: 700 }}>
          {row.visionScore}
        </div>
        <div style={{ color: T.txtMuted, fontSize: 10, marginTop: 2 }}>와드점수</div>
      </div>

      {/* ⑤ CS */}
      <div style={{ textAlign: 'center' }}>
        <div style={{ color: T.txtPrimary, fontSize: 13, fontWeight: 700 }}>{cs}</div>
        <div style={{ color: T.txtMuted, fontSize: 10, marginTop: 2 }}>
          {csPerMin ? `분당 ${csPerMin}` : '—'}
        </div>
      </div>

      {/* ⑥ 아이템 */}
      <div>
        <ItemSlots itemIds={items} />
      </div>
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   팀 섹션 (헤더 + 5명)
   — MVP/ACE 뱃지: 각 팀 내 KDA 1위 → MVP, 2위 → ACE
════════════════════════════════════════════════════════════════ */
function TeamSection({ rows, championKeyById, spellMap, maxDealt, maxTaken,
  isWin, isRemake, teamSide, myPuuid, gameDuration }) {

  /* MVP/ACE 뱃지 할당 */
  const sorted = [...rows].sort((a, b) => {
    const ra = a.deaths === 0 ? Infinity : (a.kills + a.assists) / a.deaths;
    const rb = b.deaths === 0 ? Infinity : (b.kills + b.assists) / b.deaths;
    return rb - ra;
  });
  const mvpPuuid = sorted[0]?.puuid;
  const acePuuid = sorted[1]?.puuid;
  const taggedRows = rows.map(r => ({
    ...r,
    _badge: r.puuid === mvpPuuid ? 'MVP' : r.puuid === acePuuid ? 'ACE' : null,
  }));

  const label = isRemake
    ? `다시하기 (${teamSide === 'blue' ? '블루팀' : '레드팀'})`
    : isWin
      ? `승리 (${teamSide === 'blue' ? '블루팀' : '레드팀'})`
      : `패배 (${teamSide === 'blue' ? '블루팀' : '레드팀'})`;

  const accentColor = isRemake ? '#666' : isWin
    ? (teamSide === 'blue' ? T.blue : T.blue)
    : T.red;
  const winAccent = isWin
    ? (teamSide === 'blue' ? T.blue : T.blue) : T.red;
  const finalAccent = isRemake ? '#666' : winAccent;

  const teamBg = isRemake
    ? 'rgba(255,255,255,0.02)'
    : isWin
      ? 'rgba(83,130,243,0.05)'
      : 'rgba(235,87,87,0.05)';

  return (
    <div style={{ background: teamBg }}>
      <TeamHeader label={label} accentColor={finalAccent} teamSide={teamSide} />
      {rows.map(row => (
        <PlayerRow
          key={row.puuid + (row.participantId ?? '')}
          row={row}
          championKeyById={championKeyById}
          spellMap={spellMap}
          maxDealt={maxDealt} maxTaken={maxTaken}
          teamSide={teamSide}
          isMe={row.puuid === myPuuid}
          gameDuration={gameDuration}
          isWin={isWin}
        />
      ))}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   상세 테이블  DetailTable
════════════════════════════════════════════════════════════════ */
function DetailTable({ rows, championKeyById, spellMap, myPuuid }) {
  if (!rows?.length) return (
    <div style={{ color: T.txtMuted, fontSize: 13, padding: '20px 16px' }}>
      데이터가 없습니다.
    </div>
  );

  const blue = rows.filter(r => r.teamId === 100);
  const red  = rows.filter(r => r.teamId === 200);

  const isRemake   = rows.every(r => r.gameEndedInEarlySurrender);
  const blueWin    = blue[0]?.win ?? false;
  const maxDealt   = Math.max(1, ...rows.map(r => r.totalDamageDealtToChampions ?? 0));
  const maxTaken   = Math.max(1, ...rows.map(r => r.totalDamageTaken ?? 0));
  const matchObj   = rows[0] ?? null;
  const gameDur    = matchObj?.gameDuration ?? 0;

  return (
    <div style={{
      overflowX: 'auto', minWidth: 820,
      fontFamily: 'Pretendard, "Apple SD Gothic Neo", -apple-system, sans-serif',
      background: T.bg,
    }}>
      <TeamSection
        rows={blueWin ? blue : red} championKeyById={championKeyById} spellMap={spellMap}
        maxDealt={maxDealt} maxTaken={maxTaken}
        isWin={true} isRemake={isRemake}
        teamSide={blueWin ? 'blue' : 'red'} myPuuid={myPuuid} gameDuration={gameDur}
      />
      <TeamDivider
        winRows={blueWin ? blue : red}
        loseRows={blueWin ? red : blue}
        matchObj={matchObj}
        blueIsWin={blueWin}
      />
      <TeamSection
        rows={blueWin ? red : blue} championKeyById={championKeyById} spellMap={spellMap}
        maxDealt={maxDealt} maxTaken={maxTaken}
        isWin={false} isRemake={isRemake}
        teamSide={blueWin ? 'red' : 'blue'} myPuuid={myPuuid} gameDuration={gameDur}
      />
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   매치 카드 (요약 행 + 드롭다운)
════════════════════════════════════════════════════════════════ */
function MatchCard({ match, championKeyById, spellMap, runeIconById, styleIconById,
  onToggle, isExpanded, summaryLoading, summaryRows }) {

  const isRemake    = match.gameEndedInEarlySurrender;
  const resultText  = isRemake ? '다시하기' : match.myWin ? '승리' : '패배';
  const accent      = isRemake ? '#5a6270' : match.myWin ? T.blue : T.red;
  const cardBg      = isRemake
    ? 'rgba(255,255,255,0.04)'
    : match.myWin
      ? 'rgba(83,131,243,0.20)'
      : 'rgba(232,64,87,0.20)';

  const items = [match.myItem0, match.myItem1, match.myItem2,
                 match.myItem3, match.myItem4, match.myItem5, match.myItem6];

  const dur = match.gameDuration ?? 0;
  const durStr = `${Math.floor(dur / 60)}분 ${String(dur % 60).padStart(2, '0')}초`;
  const diffSec  = Math.floor((Date.now() - match.gameCreation) / 1000);
  const diffMin  = Math.floor(diffSec / 60);
  const diffHour = Math.floor(diffMin / 60);
  const diffDay  = Math.floor(diffHour / 24);
  const diffWeek = Math.floor(diffDay / 7);
  const diffMonth = Math.floor(diffDay / 30);
  const dateStr =
    diffMin  < 1   ? `${diffSec}초 전` :
    diffHour < 1   ? `${diffMin}분 전` :
    diffDay  < 1   ? `${diffHour}시간 전` :
    diffDay  <= 7  ? `${diffDay}일 전` :
    diffMonth < 1  ? `${diffWeek}주 전` :
    `${diffMonth}개월 전`;

  /* 요약 KDA */
  const mRatio = match.myDeaths === 0
    ? Infinity : (match.myKills + match.myAssists) / match.myDeaths;
  const mKdaStr = mRatio === Infinity ? 'Perfect' : mRatio.toFixed(2) + ':1';
  const mKdaColor = mRatio === Infinity || mRatio >= 5 ? T.gold
    : mRatio >= 3 ? T.mint : T.txtSub;

  /* 킬관여율 */
  const myKillPct = match.myKills + match.myDeaths + match.myAssists > 0
    ? Math.round(match.myKills / (match.myKills + match.myDeaths + match.myAssists) * 100) : 0;

  /* CS */
  const myCs = (match.myTotalMinionsKilled ?? 0) + (match.myNeutralMinionsKilled ?? 0);
  const myCsPerMin = dur > 0 ? (myCs / (dur / 60)).toFixed(1) : null;

  return (
    <div style={{
      marginBottom: 5, borderRadius: 6, overflow: 'hidden',
      border: `1px solid ${T.border}`,
      background: T.bg,
    }}>
      {/* ── 요약 행 ── */}
      <div style={{
        display: 'flex', alignItems: 'center',
        padding: '10px 14px', gap: 12,
        background: cardBg,
        borderLeft: `4px solid ${accent}`,
        minHeight: 70,
      }}>
        {/* 게임모드 / 시간 / 승리여부 / 게임시간 */}
        <div style={{ flexShrink: 0, width: 68, textAlign: 'center' }}>
          <div style={{ color: T.txtSub, fontSize: 10, marginBottom: 2 }}>
            {getQueueName(match.queueId, match.gameMode)}
          </div>
          <div style={{ color: T.txtMuted, fontSize: 10, marginBottom: 4 }}>{dateStr}</div>
          <div style={{ color: accent, fontWeight: 800, fontSize: 15 }}>{resultText}</div>
          <div style={{ color: T.txtMuted, fontSize: 10, marginTop: 2 }}>{durStr}</div>
        </div>

        {/* 챔피언 */}
        <div style={{ position: 'relative', flexShrink: 0 }}>
          <ChampionIcon championId={match.myChampionId} championKeyById={championKeyById}
            championName={match.myChampionName} size={52} />
          <span style={{
            position: 'absolute', right: -4, bottom: -4,
            background: '#0a0c14', color: T.gold,
            fontSize: 10, fontWeight: 800, padding: '0 4px',
            borderRadius: 4, border: '1px solid rgba(255,255,255,0.12)',
            lineHeight: '16px',
          }}>{match.myChampionLevel}</span>
        </div>

        {/* 스펠 */}
        <SpellIcons spell1={match.mySpell1} spell2={match.mySpell2} spellMap={spellMap} />

        {/* 룬 (핵심룬 + 보조 계열) */}
        <KeystoneRunes
          keystoneId={match.myKeystoneId}
          subStyleId={match.mySubStyleId}
          runeIconById={runeIconById}
          styleIconById={styleIconById}
        />

        {/* KDA + 킬관여/CS */}
        <div style={{ flexShrink: 0, minWidth: 110 }}>
          <div style={{ fontSize: 16, fontWeight: 700 }}>
            <span style={{ color: T.txtKills }}>{match.myKills}</span>
            <span style={{ color: T.txtMuted }}> / </span>
            <span style={{ color: T.txtDeaths }}>{match.myDeaths}</span>
            <span style={{ color: T.txtMuted }}> / </span>
            <span style={{ color: T.txtAssists }}>{match.myAssists}</span>
          </div>
          <div style={{ marginTop: 2, fontSize: 11 }}>
            <span style={{ color: mKdaColor, fontWeight: 700 }}>{mKdaStr}</span>
          </div>
          <div style={{ marginTop: 2, fontSize: 10, color: T.txtSub }}>
            킬관여 <span style={{ color: T.txtPrimary }}>{myKillPct}%</span>
            {myCs > 0 && (
              <span style={{ marginLeft: 6 }}>
                CS <span style={{ color: T.txtPrimary }}>{myCs}</span>
                {myCsPerMin && <span style={{ color: T.txtMuted }}> ({myCsPerMin})</span>}
              </span>
            )}
          </div>
          {/* 티어 (랭크 큐일 때만) */}
          <div><TierBadge tier={match.myTier} rank={match.myRank} /></div>
        </div>

        {/* 아이템 */}
        <div style={{ minWidth: 0 }}>
          <ItemSlots itemIds={items} />
        </div>

        {/* 신축 스페이서 — 좌측(내 정보)과 우측(팀 목록/버튼)을 양 끝으로 분리.
            자식이 전부 flexShrink:0 이라 남는 공간이 한쪽 꼬리 여백으로 몰리던 문제 해소 */}
        <div style={{ flex: 1, minWidth: 12 }} />

        {/* 블루팀 / 레드팀 미니 참가자 목록 — participantSummaryDtos 직접 사용 */}
        {match.participantSummaryDtos?.length > 0 && (
          <div style={{ display: 'flex', gap: 8, flexShrink: 0 }}>
            {[100, 200].map(teamId => (
              <div key={teamId} style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                {match.participantSummaryDtos.filter(r => r.teamId === teamId).map(r => (
                  <div key={r.puuid} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                    <ChampionIcon
                      championId={r.championId}
                      championKeyById={championKeyById}
                      championName={r.championName}
                      size={16}
                    />
                    <span style={{
                      fontSize: 11, color: r.puuid === match.myPuuid ? T.txtName : T.txtSub,
                      fontWeight: r.puuid === match.myPuuid ? 700 : 400,
                      whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                      maxWidth: 72,
                    }}>{r.gameName}</span>
                  </div>
                ))}
              </div>
            ))}
          </div>
        )}

        {/* 토글 버튼 */}
        <button onClick={() => onToggle(match.matchId)} style={{
          background: 'rgba(255,255,255,0.04)',
          border: `1px solid ${T.border}`,
          color: T.txtSub, borderRadius: 5, padding: '6px 14px',
          fontSize: 12, cursor: 'pointer', flexShrink: 0,
          transition: 'border-color 0.15s, color 0.15s',
        }}
          onMouseEnter={e => {
            e.currentTarget.style.borderColor = accent;
            e.currentTarget.style.color = accent;
          }}
          onMouseLeave={e => {
            e.currentTarget.style.borderColor = T.border;
            e.currentTarget.style.color = T.txtSub;
          }}
        >
          {isExpanded ? '접기 ▲' : '상세 ▼'}
        </button>
      </div>

      {/* ── 상세 드롭다운 ── */}
      {isExpanded && (
        <div style={{ borderTop: `1px solid ${T.border}` }}>
          {summaryLoading
            ? <div style={{ color: T.txtMuted, fontSize: 13, padding: '18px 16px' }}>
                조회 중...
              </div>
            : <DetailTable
                rows={summaryRows}
                championKeyById={championKeyById}
                spellMap={spellMap}
                myPuuid={match.myPuuid}
              />
          }
        </div>
      )}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   메인 export
════════════════════════════════════════════════════════════════ */
export default function MatchList({ matches = [] }) {
  const [expandedMap,       setExpandedMap]      = useState({});
  const [summaryMap,        setSummaryMap]        = useState({});
  const [summaryLoadingMap, setSummaryLoadingMap] = useState({});
  const [spellMap,          setSpellMap]          = useState({});
  const [championKeyById,   setChampionKeyById]   = useState({});
  const [runeIconById,      setRuneIconById]       = useState({});
  const [styleIconById,     setStyleIconById]      = useState({});

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const loadLocale = async (locale) => {
        const [s, c, r] = await Promise.all([
          getSummonerSpellData(locale),
          getChampionData(locale),
          getRuneData(locale),
        ]);
        return [s.data, c.data, r.data];
      };
      try {
        const [sj, cj, rj] = await loadLocale('ko_KR').catch(() => loadLocale('en_US'));
        const spells = {};
        Object.values(sj.data).forEach(s => {
          const sid = Number(s.key);
          if (!isNaN(sid)) spells[sid] = s.image.full;
        });
        const champs = {};
        Object.values(cj.data).forEach(c => { champs[Number(c.key)] = c.id; });
        const { runeIconById: runeMap, styleIconById: styleMap } = buildRuneMaps(rj);
        if (!cancelled) {
          setSpellMap(spells);
          setChampionKeyById(champs);
          setRuneIconById(runeMap);
          setStyleIconById(styleMap);
        }
      } catch (e) { console.error('DDragon 로드 실패', e); }
    })();
    return () => { cancelled = true; };
  }, []);

  const toggleSummary = async (matchId) => {
    if (expandedMap[matchId]) {
      setExpandedMap(p => ({ ...p, [matchId]: false }));
      return;
    }
    setExpandedMap(p => ({ ...p, [matchId]: true }));
    if (summaryMap[matchId]) return;
    setSummaryLoadingMap(p => ({ ...p, [matchId]: true }));
    try {
      const res = await getMatchSummary(matchId);
      setSummaryMap(p => ({ ...p, [matchId]: res.data ?? [] }));
    } catch (e) {
      console.error('매치 상세 조회 실패', e);
      setExpandedMap(p => ({ ...p, [matchId]: false }));
    } finally {
      setSummaryLoadingMap(p => ({ ...p, [matchId]: false }));
    }
  };

  if (!matches.length) return (
    <div style={{
      background: T.layer, border: `1px solid ${T.border}`, borderRadius: 8,
      padding: '48px 0', textAlign: 'center', color: T.txtMuted, fontSize: 14,
    }}>
      매치 데이터가 없습니다.
    </div>
  );

  return (
    <div style={{
      fontFamily: 'Pretendard, "Apple SD Gothic Neo", -apple-system, sans-serif',
    }}>
      {matches.map(m => (
        <MatchCard
          key={m.matchId} match={m}
          championKeyById={championKeyById} spellMap={spellMap}
          runeIconById={runeIconById} styleIconById={styleIconById}
          onToggle={toggleSummary}
          isExpanded={!!expandedMap[m.matchId]}
          summaryLoading={!!summaryLoadingMap[m.matchId]}
          summaryRows={summaryMap[m.matchId] ?? []}
        />
      ))}
    </div>
  );
}
