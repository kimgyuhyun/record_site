import React, { useState, useEffect, useContext, createContext, useRef } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate, useParams } from 'react-router-dom';
import { getMatchSummary } from '../../api/match';
import { imgChampion, imgItem, imgSpell, imgChampionSpell, imgObjective, imgRune, imgTier } from '../../constants/ddragon';
import { getSummonerSpellData, getChampionData, getRuneData, getItemData, getChampionDetail } from '../../api/ddragon';
import TimelineModal from './TimelineModal';

/* ═══════════════════════════════════════════════════════════════
   DDragon 메타(이름) 컨텍스트
   - 스펠/룬/아이템의 "이름"을 말단 아이콘 컴포넌트에서 툴팁으로 쓰기 위해
     중간 컴포넌트마다 prop을 내리지 않고 컨텍스트로 공급한다.
   - spellNameById: 스펠 id → 이름(점멸 등)
   - runeNameById:  핵심룬 perk id → 이름(감전 등)
   - styleNameById: 룬 계열 style id → 이름(마법/정밀 등)
   - itemNameById:  아이템 id → 이름
════════════════════════════════════════════════════════════════ */
const MetaContext = createContext({
  spellNameById: {}, runeNameById: {}, styleNameById: {}, itemNameById: {},
  spellDescById: {}, runeDescById: {}, itemDescById: {}, itemGoldById: {},
  championNameById: {},
});

/* DDragon 설명 텍스트는 <br>/<stats> 등 HTML 태그를 포함 → 툴팁용 평문으로 정리 */
const stripHtml = (s) => (s || '')
  .replace(/<br\s*\/?>/gi, '\n')
  .replace(/<\/?(li|p)>/gi, '\n')
  .replace(/<[^>]+>/g, '')
  .replace(/\n{2,}/g, '\n')
  .trim();

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
  sectionLine:  '#39414f', // 상세보기 섹션(헤더/구분바) 경계선 — 확실히 보이는 회색
  tabBg:        '#1e2024', // 상세보기 탭/탭 내용 공통 배경
  tabActive:    '#2c3340', // 활성 탭 강조 배경 (탭 바 위에서 또렷하게)

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
   호버 툴팁 — 검은 배경 + 흰 글씨 (스펠/룬/아이템 공용)
   - label이 없으면 children만 그대로 렌더(빈 슬롯 등)
   - 매치 카드는 overflow:hidden(둥근 모서리)이라 일반 absolute 툴팁은
     카드 경계에서 잘린다 → position:fixed + 포털(body)로 띄워 잘림을 피한다.
════════════════════════════════════════════════════════════════ */
function Tooltip({ label, desc, gold, children }) {
  const [pos, setPos] = useState(null); // 뷰포트 기준 {x: 대상 가로중앙, y: 대상 상단}
  const ref = useRef(null);
  if (!label) return children;
  const show = () => {
    const r = ref.current?.getBoundingClientRect();
    if (r) setPos({ x: r.left + r.width / 2, y: r.top });
  };
  const hasGold = gold && gold.total > 0;
  return (
    <span
      ref={ref}
      style={{ display: 'inline-flex', flexShrink: 0 }}
      onMouseEnter={show}
      onMouseLeave={() => setPos(null)}
    >
      {children}
      {pos && createPortal(
        <span style={{
          position: 'fixed', left: pos.x, top: pos.y - 6,
          transform: 'translate(-50%, -100%)',
          background: '#000', color: '#fff',
          fontSize: 11, lineHeight: '15px',
          padding: '6px 9px', borderRadius: 5,
          maxWidth: (desc || hasGold) ? 280 : undefined,
          whiteSpace: (desc || hasGold) ? 'pre-wrap' : 'nowrap',
          pointerEvents: 'none', zIndex: 9999,
          boxShadow: '0 4px 14px rgba(0,0,0,0.5)',
        }}>
          <span style={{ fontWeight: 700, display: 'block' }}>{label}</span>
          {desc && (
            <span style={{ display: 'block', marginTop: 4, fontWeight: 400,
              color: '#cfd6e4', fontSize: 11 }}>{desc}</span>
          )}
          {hasGold && (
            <span style={{ display: 'block', marginTop: 5, fontWeight: 700,
              color: T.gold, fontSize: 11 }}>
              가격 {gold.total.toLocaleString()}
              {gold.sell > 0 && (
                <span style={{ fontWeight: 400, color: '#cfd6e4' }}>
                  {' '}({gold.sell.toLocaleString()})
                </span>
              )}
            </span>
          )}
        </span>,
        document.body,
      )}
    </span>
  );
}

/* ═══════════════════════════════════════════════════════════════
   챔피언 아이콘
════════════════════════════════════════════════════════════════ */
function ChampionIcon({ championId, championKeyById, championName, size = 40 }) {
  const { championNameById } = useContext(MetaContext);
  const key = championKeyById[championId] ||
    (championName ? championName.replace(/[^a-zA-Z0-9]/g, '') : null);
  // 호버 시 한글 챔피언명(현지화). 메타 로딩 전이면 영문명으로 폴백.
  const koName = championNameById[championId] || championName || key || '';
  const baseStyle = {
    width: size, height: size, borderRadius: '50%', flexShrink: 0,
    objectFit: 'cover', border: '2px solid rgba(255,255,255,0.15)',
  };
  if (!key) {
    return (
      <Tooltip label={koName}>
        <div style={{ ...baseStyle, background: '#1e2535' }} />
      </Tooltip>
    );
  }
  return (
    <Tooltip label={koName}>
      <img src={imgChampion(key)} alt={koName} style={baseStyle}
        onError={e => { e.target.style.visibility = 'hidden'; }} />
    </Tooltip>
  );
}

/* ═══════════════════════════════════════════════════════════════
   소환사 주문 아이콘 (2개 세로)
════════════════════════════════════════════════════════════════ */
function SpellIcons({ spell1, spell2, spellMap }) {
  const { spellNameById, spellDescById } = useContext(MetaContext);
  const sz = { width: 20, height: 20, borderRadius: 4, flexShrink: 0,
    border: '1px solid rgba(255,255,255,0.1)' };
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, flexShrink: 0 }}>
      {[spell1, spell2].map((sid, i) =>
        sid && spellMap[sid]
          ? <Tooltip key={i} label={spellNameById[sid]} desc={spellDescById[sid]}>
              <img src={imgSpell(spellMap[sid])} alt={spellNameById[sid] || ''} style={sz} />
            </Tooltip>
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
  const styleNameById = {};
  const runeNameById = {};
  const runeDescById = {};
  (reforged || []).forEach(path => {
    styleIconById[path.id] = path.icon;
    styleNameById[path.id] = path.name;
    (path.slots || []).forEach(slot =>
      (slot.runes || []).forEach(r => {
        runeIconById[r.id] = r.icon;
        runeNameById[r.id] = r.name;
        runeDescById[r.id] = stripHtml(r.shortDesc || r.longDesc);
      })
    );
  });
  return { styleIconById, runeIconById, styleNameById, runeNameById, runeDescById };
}

/* 주룬(핵심룬)은 호버 시 룬 이름+설명, 부룬(보조 계열)은 계열 이름(마법/정밀 등)만 노출 */
function KeystoneRunes({ keystoneId, subStyleId, runeIconById, styleIconById }) {
  const { runeNameById, styleNameById, runeDescById } = useContext(MetaContext);
  const keystone = runeIconById[keystoneId];
  const subTree = styleIconById[subStyleId];
  const placeholder = (size) => ({
    width: size, height: size, borderRadius: '50%', flexShrink: 0,
    background: 'rgba(255,255,255,0.05)', border: '1px solid rgba(255,255,255,0.06)',
  });
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 2, alignItems: 'center', flexShrink: 0 }}>
      {keystone
        ? <Tooltip label={runeNameById[keystoneId]} desc={runeDescById[keystoneId]}>
            <img src={imgRune(keystone)} alt={runeNameById[keystoneId] || ''}
              style={{ width: 24, height: 24, borderRadius: '50%', background: '#0a0c14' }} />
          </Tooltip>
        : <div style={placeholder(24)} />}
      {subTree
        ? <Tooltip label={styleNameById[subStyleId]}>
            <img src={imgRune(subTree)} alt={styleNameById[subStyleId] || ''} style={{ width: 15, height: 15 }} />
          </Tooltip>
        : <div style={placeholder(15)} />}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   아이템 슬롯: 32×32, border rgba(255,255,255,0.08)
════════════════════════════════════════════════════════════════ */
function ItemSlots({ itemIds = [] }) {
  const { itemNameById, itemDescById, itemGoldById } = useContext(MetaContext);
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'nowrap' }}>
      {itemIds.map((id, i) => {
        const slotStyle = {
          width: 32, height: 32, borderRadius: 4, flexShrink: 0,
          border: '1px solid rgba(255,255,255,0.08)',
          objectFit: 'cover',
        };
        return id > 0
          ? <Tooltip key={i} label={itemNameById[id]} desc={itemDescById[id]} gold={itemGoldById[id]}>
              <img src={imgItem(id)} alt={itemNameById[id] || ''} style={slotStyle} />
            </Tooltip>
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
   평균 티어 뱃지 (해당 큐 기준 소환사 랭크 엠블럼 + 호버 툴팁)
   - 마스터+ 는 단계(rank) 표기 없음
   - 엠블럼은 static/tier 폴더의 이미지 사용 (imgTier)
   - 호버 시 검정 배경/흰 글씨 "평균 티어" 툴팁 노출
   - 참가자별 티어 데이터가 없어 현재는 소환사 본인 티어를 평균값으로 사용
════════════════════════════════════════════════════════════════ */
const TIER_APEX = new Set(['MASTER', 'GRANDMASTER', 'CHALLENGER']);
function AverageTierBadge({ tier, rank }) {
  const [hover, setHover] = useState(false);
  if (!tier) return null;
  const name = tier.charAt(0) + tier.slice(1).toLowerCase();
  const text = TIER_APEX.has(tier) ? name : `${name} ${rank ?? ''}`.trim();
  return (
    <span
      style={{ position: 'relative', display: 'inline-flex', alignItems: 'center',
        gap: 4, marginTop: 3, cursor: 'default' }}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
    >
      <img src={imgTier(tier)} alt={name}
        style={{ width: 18, height: 18, objectFit: 'contain', flexShrink: 0 }}
        onError={e => { e.target.style.display = 'none'; }} />
      <span style={{ fontSize: 11, fontWeight: 700, color: T.gold }}>{text}</span>

      {hover && (
        <span style={{
          position: 'absolute', bottom: 'calc(100% + 6px)', left: 0,
          background: '#000', color: '#fff',
          fontSize: 11, fontWeight: 600, lineHeight: '14px',
          padding: '4px 8px', borderRadius: 4, whiteSpace: 'nowrap',
          pointerEvents: 'none', zIndex: 20,
        }}>평균 티어</span>
      )}
    </span>
  );
}

/* ═══════════════════════════════════════════════════════════════
   피해량 그래프
   - 딜(빨강) + 탱(파랑) 두 줄
   - 수치: 좌측 수치 표시, 바는 flex 비율
════════════════════════════════════════════════════════════════ */
function DamageGraph({ dealt, taken, maxDealt, maxTaken }) {
  // 한 행에 좌(가한 피해=빨강) / 우(받은 피해=회색)를 나란히. 각 숫자 아래 비율 바.
  const dp = maxDealt > 0 ? Math.max(3, Math.round((dealt / maxDealt) * 100)) : 0;
  const tp = maxTaken > 0 ? Math.max(3, Math.round((taken / maxTaken) * 100)) : 0;

  const half = (value, pct, barColor, textColor, alignRight) => (
    <div style={{ flex: 1, minWidth: 0 }}>
      <Tooltip label={alignRight ? '챔피언에게 가한 피해량' : '챔피언에게 받은 피해량'}>
        <div style={{
          fontSize: 12, fontWeight: alignRight ? 700 : 500, color: textColor,
          textAlign: 'right', fontVariantNumeric: 'tabular-nums', whiteSpace: 'nowrap',
        }}>{value.toLocaleString()}</div>
      </Tooltip>
      <div style={{ height: 5, background: 'rgba(255,255,255,0.05)', borderRadius: 3, overflow: 'hidden', marginTop: 3 }}>
        <div style={{ width: `${pct}%`, height: '100%', background: barColor, borderRadius: 3 }} />
      </div>
    </div>
  );

  return (
    <div style={{ width: '100%', display: 'flex', alignItems: 'flex-start', gap: 10 }}>
      {half(dealt, dp, 'linear-gradient(90deg,#c8203a,#e84057)', T.txtPrimary, true)}
      {half(taken, tp, '#5a6678', T.txtSub, false)}
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
// queueId가 매핑에 없을 때 gameMode를 한글로 치환 (예: 아레나 CHERRY)
const GAME_MODE_NAME = {
  CHERRY: '아레나', ARAM: '칼바람', URF: 'URF', ARURF: 'URF',
  NEXUSBLITZ: '돌격! 넥서스', ONEFORALL: '단일 챔피언', ULTBOOK: '궁극기 주문서',
  CLASSIC: '일반',
};
const getQueueName = (queueId, gameMode) =>
  QUEUE_NAME[queueId] || GAME_MODE_NAME[gameMode] || gameMode || '일반';

/* ═══════════════════════════════════════════════════════════════
   아레나(CHERRY) 등수 헬퍼
   - 아레나는 블루/레드 2팀이 아니라 2인 듀오 4팀 → 등수(1~4위)로 표시한다.
   - subteamPlacement = 듀오 등수, playerSubteamId = 듀오 식별자(같으면 한 팀).
   - 등수 데이터가 없는(마이그레이션 이전 수집) 옛 매치는 기존 팀 표시로 폴백한다.
════════════════════════════════════════════════════════════════ */
const ARENA_QUEUE_IDS = new Set([1700, 1701, 1710]);
const isArenaMatch = (queueId, gameMode) =>
  ARENA_QUEUE_IDS.has(queueId) || gameMode === 'CHERRY';

// 1위 금 / 2위 은 / 3위 동 / 그 외 뮤트
const PLACEMENT_COLORS = { 1: '#f0a800', 2: '#9aa7b4', 3: '#c0845a' };
const placementColor = (p) => PLACEMENT_COLORS[p] || '#6b7a8d';

// playerSubteamId → 아레나 팀 마스코트(한국어). 마스코트는 듀오 슬롯마다 고정이라 id로 역산한다.
// (CommunityDragon 표준 순서 기준 — 실제 게임에서 이름이 어긋나면 이 표만 고치면 됨)
const ARENA_TEAM_NAMES = {
  1: '포로', 2: '미니언', 3: '집게발', 4: '돌거북',
  5: '칼날부리', 6: '파수꾼', 7: '늑대', 8: '두꺼비',
};
const arenaTeamName = (subteamId) => ARENA_TEAM_NAMES[subteamId] || null;

// 참가자들을 듀오(playerSubteamId)로 묶고 등수(subteamPlacement) 오름차순 정렬
function groupArenaSubteams(parts) {
  const byTeam = new Map();
  for (const r of parts) {
    const key = r.playerSubteamId ?? r.teamId ?? 0;
    if (!byTeam.has(key)) byTeam.set(key, []);
    byTeam.get(key).push(r);
  }
  return [...byTeam.values()].sort(
    (a, b) => (a[0].subteamPlacement ?? 99) - (b[0].subteamPlacement ?? 99),
  );
}

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
    <div style={{ background:'#1e2024', padding:'10px 16px',
      borderTop:`1px solid ${T.sectionLine}`, borderBottom:`1px solid ${T.sectionLine}` }}>

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
      // 헤더 밴드에 배경색 + 또렷한 경계선으로 본문 행과 확실히 구분
      background: '#1a1f2b',
      borderTop: `1px solid ${T.sectionLine}`,
      borderBottom: `1px solid ${T.sectionLine}`,
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
function PlayerRow({ row, championKeyById, spellMap, runeIconById, styleIconById,
  onSummonerClick, maxDealt, maxTaken, teamSide, isMe, gameDuration, isWin, isArena }) {

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

  /* 배경 — 승리/패배 기준, 내 행은 살짝 더 진하게.
     기준 색은 요약 카드(cardBg)와 동일한 파랑(83,131,243)/빨강(232,64,87)으로 통일하고
     적용 강도(투명도)만 행별로 다르게 둔다. */
  const rowBg = isMe
    ? (isWin ? 'rgba(83,131,243,0.20)' : 'rgba(232,64,87,0.18)')
    : (isWin ? 'rgba(83,131,243,0.08)' : 'rgba(232,64,87,0.08)');

  return (
    <div style={{
      display: 'grid', gridTemplateColumns: GRID,
      alignItems: 'center', minHeight: 62,
      padding: '0 16px',
      background: rowBg,
      borderBottom: `1px solid ${T.border}`,
    }}>

      {/* ① 플레이어: 챔프 + 스펠 + 룬 + 이름/챔피언명 */}
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

        {/* 룬 (spell1 옆 핵심룬=주룬, spell2 옆 보조 계열=부룬)
            — 아레나는 룬 개념이 없어 빈 동그라미가 떠서 숨긴다 */}
        {!isArena && (
          <KeystoneRunes
            keystoneId={row.keystoneId}
            subStyleId={row.subStyleId}
            runeIconById={runeIconById}
            styleIconById={styleIconById}
          />
        )}

        {/* 이름 + 챔피언 (이름 클릭 시 해당 소환사 페이지로 이동) */}
        <div style={{ minWidth: 0 }}>
          <div
            onClick={() => onSummonerClick?.(row.gameName, row.tagLine)}
            title={`${row.gameName}#${row.tagLine}`}
            style={{
              color: T.txtName, fontSize: 13, fontWeight: isMe ? 700 : 500,
              whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
              cursor: 'pointer', display: 'inline-block', maxWidth: '100%',
            }}
            onMouseEnter={e => { e.currentTarget.style.textDecoration = 'underline'; }}
            onMouseLeave={e => { e.currentTarget.style.textDecoration = 'none'; }}
          >
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

      {/* ④ 와드 — 셀 전체에 하나의 호버 툴팁(제어/설치/제거를 줄바꿈으로 한번에) */}
      <div style={{ textAlign: 'center' }}>
        <Tooltip label="와드"
          desc={`제어 와드: ${row.visionWardsBoughtInGame ?? 0}\n와드 설치: ${row.wardsPlaced ?? 0}\n와드 제거: ${row.wardsKilled ?? 0}`}>
          <span style={{ display: 'inline-block', lineHeight: 1.35, cursor: 'default' }}>
            <span style={{ display: 'block', color: T.txtPrimary, fontSize: 12, fontWeight: 700 }}>
              {row.visionWardsBoughtInGame ?? 0}
            </span>
            <span style={{ display: 'block', fontSize: 11, marginTop: 1, color: T.txtSub }}>
              {row.wardsPlaced ?? 0} <span style={{ color: T.txtMuted }}>/</span> {row.wardsKilled ?? 0}
            </span>
          </span>
        </Tooltip>
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
function TeamSection({ rows, championKeyById, spellMap, runeIconById, styleIconById,
  onSummonerClick, maxDealt, maxTaken, isWin, isRemake, teamSide, myPuuid, gameDuration }) {

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
      ? 'rgba(83,131,243,0.05)'
      : 'rgba(232,64,87,0.05)';

  return (
    <div style={{ background: teamBg }}>
      <TeamHeader label={label} accentColor={finalAccent} teamSide={teamSide} />
      {rows.map(row => (
        <PlayerRow
          key={row.puuid + (row.participantId ?? '')}
          row={row}
          championKeyById={championKeyById}
          spellMap={spellMap}
          runeIconById={runeIconById}
          styleIconById={styleIconById}
          onSummonerClick={onSummonerClick}
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
   아레나 상세 — 듀오 4팀을 등수(1~4위) 순으로 쌓아 보여준다.
════════════════════════════════════════════════════════════════ */
function ArenaDetail({ rows, championKeyById, spellMap, runeIconById, styleIconById, onSummonerClick, myPuuid }) {
  const maxDealt = Math.max(1, ...rows.map(r => r.totalDamageDealtToChampions ?? 0));
  const maxTaken = Math.max(1, ...rows.map(r => r.totalDamageTaken ?? 0));
  const gameDur  = rows[0]?.gameDuration ?? 0;
  const subteams = groupArenaSubteams(rows);

  return (
    <div style={{
      overflowX: 'auto', minWidth: 820,
      fontFamily: 'Pretendard, "Apple SD Gothic Neo", -apple-system, sans-serif',
      background: T.tabBg,
    }}>
      <TeamHeader label="순위" />
      {subteams.map((duo, i) => {
        const placement = duo[0].subteamPlacement ?? duo[0].placement;
        const teamName  = arenaTeamName(duo[0].playerSubteamId);
        const mine  = duo.some(r => r.puuid === myPuuid);
        const color = placementColor(placement);
        return (
          <div key={duo[0].playerSubteamId ?? i}>
            {/* 등수 + 팀 이름 라벨 바 (예: #1 돌거북팀) */}
            <div style={{
              padding: '5px 16px', background: `${color}1a`,
              borderBottom: `1px solid ${T.border}`,
              display: 'flex', alignItems: 'center', gap: 8,
            }}>
              <span style={{ color, fontWeight: 800, fontSize: 13 }}>#{placement}</span>
              {teamName && <span style={{ color: '#c8d0e0', fontWeight: 700, fontSize: 13 }}>{teamName}팀</span>}
              {mine && <span style={{ color: T.txtSub, fontSize: 11, fontWeight: 600 }}>내 팀</span>}
            </div>
            {duo.map(row => (
              <PlayerRow
                key={row.puuid + (row.participantId ?? '')}
                row={row}
                championKeyById={championKeyById}
                spellMap={spellMap}
                runeIconById={runeIconById}
                styleIconById={styleIconById}
                onSummonerClick={onSummonerClick}
                maxDealt={maxDealt} maxTaken={maxTaken}
                teamSide={placement <= 3 ? 'blue' : 'red'}
                isMe={row.puuid === myPuuid}
                gameDuration={gameDur}
                isWin={placement <= 3}
                isArena
              />
            ))}
          </div>
        );
      })}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   상세 테이블  DetailTable
════════════════════════════════════════════════════════════════ */
function DetailTable({ rows, championKeyById, spellMap, runeIconById, styleIconById, onSummonerClick, myPuuid }) {
  if (!rows?.length) return (
    <div style={{ color: T.txtMuted, fontSize: 13, padding: '20px 16px' }}>
      데이터가 없습니다.
    </div>
  );

  // 아레나는 듀오 4팀을 등수로 표시한다. 등수 데이터가 있을 때만 전용 레이아웃 사용.
  const arenaByMode  = isArenaMatch(rows[0].queueId, rows[0].gameMode);
  const hasPlacement = rows.some(r => r.subteamPlacement != null);
  if (arenaByMode && hasPlacement) {
    return (
      <ArenaDetail
        rows={rows} championKeyById={championKeyById} spellMap={spellMap}
        runeIconById={runeIconById} styleIconById={styleIconById}
        onSummonerClick={onSummonerClick} myPuuid={myPuuid}
      />
    );
  }

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
      background: T.tabBg,
    }}>
      <TeamSection
        rows={blueWin ? blue : red} championKeyById={championKeyById} spellMap={spellMap}
        runeIconById={runeIconById} styleIconById={styleIconById}
        onSummonerClick={onSummonerClick}
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
        runeIconById={runeIconById} styleIconById={styleIconById}
        onSummonerClick={onSummonerClick}
        maxDealt={maxDealt} maxTaken={maxTaken}
        isWin={false} isRemake={isRemake}
        teamSide={blueWin ? 'red' : 'blue'} myPuuid={myPuuid} gameDuration={gameDur}
      />
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   아레나 미니 목록 (요약 행 우측)
   - 16명(8듀오) 전체를 늘어놓으면 가로로 잘려 보이므로,
     요약 행에서는 "내 듀오" 같은 팀 2명만 이름과 함께 보여준다.
   - 전체 등수(1~8위)는 카드를 펼친 상세보기(ArenaDetail)에서 확인한다.
════════════════════════════════════════════════════════════════ */
function ArenaMiniList({ parts, championKeyById, myPuuid, onSummonerClick }) {
  const mySubteamId = parts.find(r => r.puuid === myPuuid)?.playerSubteamId;
  const myDuo = parts.filter(r => r.playerSubteamId === mySubteamId);
  if (!myDuo.length) return null;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 3, flexShrink: 0 }}>
      {myDuo.map(r => (
        <div key={r.puuid} style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <ChampionIcon championId={r.championId} championKeyById={championKeyById}
            championName={r.championName} size={16} />
          <span
            onClick={(e) => { e.stopPropagation(); onSummonerClick?.(r.gameName, r.tagLine); }}
            title={`${r.gameName}#${r.tagLine}`}
            style={{
              fontSize: 11, color: r.puuid === myPuuid ? T.txtName : T.txtSub,
              fontWeight: r.puuid === myPuuid ? 700 : 400,
              whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
              maxWidth: 66, cursor: 'pointer',
            }}
            onMouseEnter={e => { e.currentTarget.style.textDecoration = 'underline'; }}
            onMouseLeave={e => { e.currentTarget.style.textDecoration = 'none'; }}
          >{r.gameName}</span>
        </div>
      ))}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   상세 탭 바 (종합 / 팀 분석 / 빌드)
════════════════════════════════════════════════════════════════ */
const DETAIL_TABS = ['종합', '팀 분석', '빌드'];

function DetailTabs({ tabs = DETAIL_TABS, active, onChange }) {
  return (
    // 탭 바는 본문(#13131b)보다 어두운 톤 + 탭 사이 구분선. 활성 탭은 배경 강조 + 파란 밑줄.
    <div style={{ display: 'flex', background: T.tabBg, borderBottom: `1px solid ${T.borderStrong}` }}>
      {tabs.map((tab, i) => {
        const on = tab === active;
        return (
          <button key={tab} onClick={() => onChange(tab)}
            style={{
              flex: 1, padding: '10px 4px', cursor: 'pointer', border: 'none',
              borderRight: i < tabs.length - 1 ? '1px solid rgba(255,255,255,0.06)' : 'none',
              background: on ? T.tabActive : 'transparent',
              color: on ? T.txtName : T.txtSub,
              fontWeight: on ? 700 : 500, fontSize: 13,
              borderBottom: on ? `2px solid ${T.blue}` : '2px solid transparent',
              fontFamily: 'inherit',
            }}>
            {tab}
          </button>
        );
      })}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   빌드 탭 — 내 챔피언의 아이템 빌드(분 단위 묶음) / 스킬 빌드 / 룬
   - itemBuildOrder: "아이템id:구매초,..." → 분 단위 묶음 + 같은 묶음 내 동일 아이템 개수 누적
   - skillBuildOrder: "QWEQ..." → 선마 순서(5레벨 도달 순) + 레벨별 시퀀스
════════════════════════════════════════════════════════════════ */
const SKILL_COLOR = { Q: '#3b7dd8', W: '#1f9e63', E: '#9b46d6', R: '#d9a514' };
const SKILL_SLOT_INDEX = { Q: 0, W: 1, E: 2, R: 3 }; // DDragon spells 배열 순서

// 능력치 파편 id → DDragon StatMods 아이콘 경로 (perk-images 하위, 버전 무관)
const STAT_SHARD_ICON = {
  5008: 'perk-images/StatMods/StatModsAdaptiveForceIcon.png',
  5005: 'perk-images/StatMods/StatModsAttackSpeedIcon.png',
  5007: 'perk-images/StatMods/StatModsCDRScalingIcon.png',
  5001: 'perk-images/StatMods/StatModsHealthScalingIcon.png',
  5011: 'perk-images/StatMods/StatModsHealthPlusIcon.png',
  5013: 'perk-images/StatMods/StatModsTenacityIcon.png',
  5010: 'perk-images/StatMods/StatModsMovementSpeedIcon.png',
};

// 능력치 파편 3행(공격/유연/방어) 고정 레이아웃. 선택된 칸만 강조하고 나머지는 흐리게.
const STAT_SHARDS = [
  [{ id: 5008, n: '적응형 능력치' }, { id: 5005, n: '공격 속도' }, { id: 5007, n: '스킬 가속' }],
  [{ id: 5008, n: '적응형 능력치' }, { id: 5010, n: '이동 속도' }, { id: 5001, n: '체력(레벨 비례)' }],
  [{ id: 5011, n: '체력' }, { id: 5013, n: '강인함 및 둔화 저항' }, { id: 5001, n: '체력(레벨 비례)' }],
];

// 능력치 파편 설명 — 룬과 달리 DDragon이 설명을 안 줘서 직접 명시(툴팁용)
const STAT_SHARD_DESC = {
  5008: '+5.4 공격력 또는 +9 주문력 (적응형)',
  5005: '+10% 공격 속도',
  5007: '+8 스킬 가속',
  5010: '+2% 이동 속도',
  5001: '레벨에 비례해 최대 +15~140 체력',
  5011: '+65 체력',
  5013: '+10% 강인함 및 둔화 저항',
};

// 챔피언 스킬 상세는 매치 펼칠 때마다 다시 받지 않도록 모듈 캐시에 보관 (championName → spells[Q,W,E,R])
const championSpellCache = {};

function useChampionSpells(championName) {
  const [spells, setSpells] = useState(() => (championName ? championSpellCache[championName] : null) || null);
  useEffect(() => {
    if (!championName) return;
    if (championSpellCache[championName]) { setSpells(championSpellCache[championName]); return; }
    let cancelled = false;
    (async () => {
      try {
        const res = await getChampionDetail('ko_KR', championName)
          .catch(() => getChampionDetail('en_US', championName));
        const arr = res?.data?.data?.[championName]?.spells || [];
        championSpellCache[championName] = arr;
        if (!cancelled) setSpells(arr);
      } catch { if (!cancelled) setSpells([]); }
    })();
    return () => { cancelled = true; };
  }, [championName]);
  return spells;
}

// "id:sec,id:sec" → [{ minute, items: [{ id, count }] }] (분이 바뀌면 새 묶음)
function parseItemBuild(itemBuildOrder) {
  if (!itemBuildOrder) return [];
  const groups = [];
  itemBuildOrder.split(',').forEach(tok => {
    const [idStr, secStr] = tok.split(':');
    const id = Number(idStr);
    if (!id || id <= 0) return;
    const minute = Math.floor((Number(secStr) || 0) / 60);
    let g = groups[groups.length - 1];
    if (!g || g.minute !== minute) { g = { minute, items: [] }; groups.push(g); }
    const last = g.items[g.items.length - 1];
    if (last && last.id === id) last.count += 1;   // 같은 묶음의 연속 동일 아이템 누적(예: 물약 ×2)
    else g.items.push({ id, count: 1 });
  });
  return groups;
}

// "QWEQ..." → Q/W/E 가 5레벨에 먼저 도달한 순서(선마 순서). 5레벨 미도달은 현재 레벨 높은 순으로 뒤에.
function masterSkillOrder(skillBuildOrder) {
  if (!skillBuildOrder) return [];
  const count = { Q: 0, W: 0, E: 0 };
  const order = [];
  for (const ch of skillBuildOrder) {
    if (count[ch] === undefined) continue; // R 등 무시
    count[ch] += 1;
    if (count[ch] === 5 && !order.includes(ch)) order.push(ch);
  }
  ['Q', 'W', 'E'].sort((a, b) => count[b] - count[a])
    .forEach(s => { if (!order.includes(s)) order.push(s); });
  return order;
}

function SkillBadge({ letter, size = 24 }) {
  const color = SKILL_COLOR[letter] || '#3e4a5a';
  return (
    <span style={{
      width: size, height: size, borderRadius: 4,
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      background: color, color: '#fff', fontWeight: 800,
      fontSize: size <= 22 ? 11 : 13, flexShrink: 0,
      border: letter === 'R' ? '1px solid #f5d97a' : '1px solid rgba(0,0,0,0.25)',
    }}>{letter}</span>
  );
}

// 선마 순서 한 칸 — 챔피언 스킬 아이콘 + Q/W/E 라벨, 호버 시 스킬명/쿨다운/사거리/설명 툴팁.
function SkillMasterStep({ letter, spell }) {
  const color = SKILL_COLOR[letter] || '#3e4a5a';
  if (!spell || !spell.image) return <SkillBadge letter={letter} size={42} />;

  const lines = [];
  if (spell.cooldownBurn && spell.cooldownBurn !== '0') lines.push(`재사용 대기시간: ${spell.cooldownBurn}초`);
  if (spell.rangeBurn && spell.rangeBurn !== '0') lines.push(`사거리: ${spell.rangeBurn}`);
  const head = lines.join('\n');
  const body = stripHtml(spell.description);
  const desc = head ? `${head}\n\n${body}` : body;

  return (
    <Tooltip label={spell.name} desc={desc}>
      <span style={{ position: 'relative', display: 'inline-block' }}>
        <img src={imgChampionSpell(spell.image.full)} alt={spell.name}
          style={{ width: 42, height: 42, borderRadius: 6, border: '1px solid rgba(255,255,255,0.12)' }} />
        <span style={{
          position: 'absolute', right: -3, bottom: -3, background: color, color: '#fff',
          fontSize: 10, fontWeight: 800, padding: '0 4px', borderRadius: 3, lineHeight: '15px',
          border: '1px solid #0a0c14',
        }}>{letter}</span>
      </span>
    </Tooltip>
  );
}

function BuildArrow({ centered }) {
  return <span style={{ color: T.txtMuted, fontSize: 16, alignSelf: centered ? 'center' : 'flex-start', marginTop: centered ? 0 : 12 }}>›</span>;
}

// 전폭(full-width) 어두운 헤더 바 + 본문. 섹션마다 밴드로 나뉘어 경계가 또렷하게 보인다.
function BuildSection({ title, children }) {
  return (
    <>
      <div style={{
        color: T.txtSub, fontSize: 13, fontWeight: 700, padding: '9px 16px',
        // 본문 배경(#1e2024)보다 살짝 어둡게 해서 은은하게 구분
        background: '#181a1e', borderTop: `1px solid ${T.border}`, borderBottom: `1px solid ${T.border}`,
      }}>{title}</div>
      <div style={{ padding: '18px 16px' }}>{children}</div>
    </>
  );
}

// 룬/파편 동그란 아이콘 + 선택 강조 + 호버 툴팁
function RuneCircle({ icon, name, desc, selected, size }) {
  return (
    <Tooltip label={name} desc={desc}>
      {icon
        ? <img src={imgRune(icon)} alt={name || ''} style={{
            width: size, height: size, borderRadius: '50%', background: '#0a0c14',
            // 미선택 룬은 회색(흑백) + 흐리게 처리해 선택 룬과 확실히 구분
            opacity: selected ? 1 : 0.45,
            filter: selected ? 'none' : 'grayscale(100%) brightness(0.7)',
            border: selected ? '2px solid #c8aa6e' : '2px solid transparent',
          }} />
        : <span style={{ width: size, height: size, borderRadius: '50%', background: 'rgba(255,255,255,0.05)', display: 'inline-block' }} />}
    </Tooltip>
  );
}

// 룬 페이지 전체(주 트리 + 보조 트리 + 능력치 파편). 선택 룬은 밝게, 나머지는 흐리게.
function RunePage({ row, runeTree }) {
  const { runeNameById, runeDescById } = useContext(MetaContext);
  const primaryPath = runeTree.find(p => p.id === row.primaryStyleId);
  const secondaryPath = runeTree.find(p => p.id === row.subStyleId);
  const selected = new Set(
    [row.keystoneId, row.primaryRune1, row.primaryRune2, row.primaryRune3, row.subRune1, row.subRune2]
      .filter(Boolean)
  );

  const renderSlot = (runes, big, key) => (
    <div key={key} style={{ display: 'flex', justifyContent: 'center', gap: big ? 12 : 16, marginBottom: 14 }}>
      {runes.map(r => (
        <RuneCircle key={r.id} icon={r.icon} name={runeNameById[r.id] || r.name}
          desc={runeDescById[r.id]} selected={selected.has(r.id)} size={big ? 38 : 26} />
      ))}
    </div>
  );

  const columnStyle = { padding: '0 22px', display: 'flex', flexDirection: 'column', alignItems: 'center' };
  const pathIcon = (path) => (
    <div style={{ height: 26, marginBottom: 14 }}>
      {path && <img src={imgRune(path.icon)} alt={path.name} style={{ width: 24, height: 24 }} />}
    </div>
  );

  return (
    <div style={{ display: 'flex', justifyContent: 'center', flexWrap: 'wrap', rowGap: 16 }}>
      {/* 주 룬 트리 (키스톤 줄 포함) */}
      {primaryPath && (
        <div style={{ ...columnStyle, borderRight: `1px solid ${T.border}` }}>
          {pathIcon(primaryPath)}
          {primaryPath.slots.map((slot, si) => renderSlot(slot.runes, si === 0, si))}
        </div>
      )}
      {/* 보조 룬 트리 (키스톤 줄 제외) */}
      {secondaryPath && (
        <div style={{ ...columnStyle, borderRight: `1px solid ${T.border}` }}>
          {pathIcon(secondaryPath)}
          {secondaryPath.slots.slice(1).map((slot, si) => renderSlot(slot.runes, false, si))}
        </div>
      )}
      {/* 능력치 파편 */}
      <div style={columnStyle}>
        <div style={{ height: 26, marginBottom: 14, color: T.txtMuted, fontSize: 11, lineHeight: '26px' }}>능력치 파편</div>
        {STAT_SHARDS.map((shardRow, ri) => {
          const selectedId = [row.statPerkOffense, row.statPerkFlex, row.statPerkDefense][ri];
          return (
            <div key={ri} style={{ display: 'flex', justifyContent: 'center', gap: 16, marginBottom: 14 }}>
              {shardRow.map(sh => (
                <RuneCircle key={sh.id} icon={STAT_SHARD_ICON[sh.id]} name={sh.n}
                  desc={STAT_SHARD_DESC[sh.id]} selected={sh.id === selectedId} size={24} />
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function BuildView({ row, championKeyById, runeIconById, styleIconById, runeTree }) {
  const { itemNameById, itemDescById, itemGoldById } = useContext(MetaContext);
  const spells = useChampionSpells(row ? championKeyById[row.championId] : null);

  if (!row) {
    return <div style={{ color: T.txtMuted, fontSize: 13, padding: '20px 16px' }}>데이터가 없습니다.</div>;
  }

  const itemGroups = parseItemBuild(row.itemBuildOrder);
  const skillSeq = row.skillBuildOrder || '';
  const master = masterSkillOrder(skillSeq);
  const hasBuild = itemGroups.length > 0 || skillSeq.length > 0;

  if (!hasBuild) {
    return (
      <div style={{ color: T.txtMuted, fontSize: 13, padding: '20px 16px' }}>
        빌드 데이터가 없습니다. (타임라인 미수집 매치 — 갱신 후 새로 적재된 매치부터 표시됩니다.)
      </div>
    );
  }

  const spellOf = (letter) => (spells ? spells[SKILL_SLOT_INDEX[letter]] : null);
  const hasRunes = (runeTree.length > 0) && (row.primaryStyleId || row.subStyleId);

  return (
    <div style={{ background: T.tabBg }}>
      {/* ── 아이템 빌드 (좌측 정렬, 분 단위 묶음) ── */}
      <BuildSection title="아이템 빌드">
      {itemGroups.length === 0
        ? <div style={{ color: T.txtMuted, fontSize: 12 }}>아이템 구매 기록이 없습니다.</div>
        : (
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'flex-start', gap: 6, rowGap: 16 }}>
            {itemGroups.map((g, gi) => (
              <React.Fragment key={gi}>
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
                  <div style={{
                    display: 'flex', gap: 3, padding: '5px 6px',
                    background: 'rgba(255,255,255,0.03)',
                    border: `1px solid ${T.border}`, borderRadius: 6,
                  }}>
                    {g.items.map((it, ii) => (
                      <Tooltip key={ii} label={itemNameById[it.id]} desc={itemDescById[it.id]} gold={itemGoldById[it.id]}>
                        <span style={{ position: 'relative', display: 'inline-block' }}>
                          <img src={imgItem(it.id)} alt={itemNameById[it.id] || ''}
                            style={{ width: 42, height: 42, borderRadius: 4, border: '1px solid rgba(255,255,255,0.08)' }} />
                          {it.count > 1 && (
                            <span style={{
                              position: 'absolute', right: -3, bottom: -3,
                              background: '#0a0c14', color: '#fff', fontSize: 10, fontWeight: 800,
                              padding: '0 3px', borderRadius: 3, border: '1px solid rgba(255,255,255,0.2)',
                              lineHeight: '14px',
                            }}>{it.count}</span>
                          )}
                        </span>
                      </Tooltip>
                    ))}
                  </div>
                  <span style={{ fontSize: 11, color: T.txtMuted }}>{g.minute}분</span>
                </div>
                {gi < itemGroups.length - 1 && <BuildArrow />}
              </React.Fragment>
            ))}
          </div>
        )}
      </BuildSection>

      {/* ── 스킬 빌드 (가운데 정렬) ── */}
      <BuildSection title="스킬 빌드">
      {skillSeq.length === 0
        ? <div style={{ color: T.txtMuted, fontSize: 12 }}>스킬 레벨업 기록이 없습니다.</div>
        : (
          <div>
            {master.length > 0 && (
              <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 10, marginBottom: 16 }}>
                {master.map((s, i) => (
                  <React.Fragment key={s}>
                    <SkillMasterStep letter={s} spell={spellOf(s)} />
                    {i < master.length - 1 && <span style={{ color: T.txtMuted, fontSize: 18 }}>›</span>}
                  </React.Fragment>
                ))}
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'center', gap: 4, flexWrap: 'wrap' }}>
              {[...skillSeq].map((ch, i) => <SkillBadge key={i} letter={ch} size={26} />)}
            </div>
          </div>
        )}
      </BuildSection>

      {/* ── 룬 (가운데 정렬, 전체 트리) ── */}
      <BuildSection title="룬">
      {hasRunes
        ? <RunePage row={row} runeTree={runeTree} />
        : <div style={{ color: T.txtMuted, fontSize: 12 }}>룬 정보가 없습니다.</div>}
      </BuildSection>
    </div>
  );
}


/* ═══════════════════════════════════════════════════════════════
   팀 분석 탭 — 지표별 양 팀 비교 (승리팀=파랑 / 패배팀=빨강)
   - 6개 지표: 챔피언 처치 / 골드 / 가한 피해 / 와드 설치 / 받은 피해 / CS
   - 각 패널: 좌(승리팀 5명 막대) + 중앙 도넛(팀 합계 비율) + 우(패배팀 5명 막대)
════════════════════════════════════════════════════════════════ */
const TEAM_STATS = [
  { title: '챔피언 처치',           valueOf: r => r.kills ?? 0 },
  { title: '골드 획득량',           valueOf: r => r.goldEarned ?? 0 },
  { title: '챔피언에게 가한 피해량', valueOf: r => r.totalDamageDealtToChampions ?? 0 },
  { title: '와드 설치',             valueOf: r => r.wardsPlaced ?? 0 },
  { title: '받은 피해량',           valueOf: r => r.totalDamageTaken ?? 0 },
  { title: 'CS',                   valueOf: r => (r.totalMinionsKilled ?? 0) + (r.neutralMinionsKilled ?? 0) },
];

function StatComparePanel({ title, valueOf, winRows, loseRows, championKeyById }) {
  const winVals = winRows.map(valueOf);
  const loseVals = loseRows.map(valueOf);
  const winTotal = winVals.reduce((a, b) => a + Number(b || 0), 0);
  const loseTotal = loseVals.reduce((a, b) => a + Number(b || 0), 0);
  const maxVal = Math.max(1, ...winVals, ...loseVals);
  const grand = winTotal + loseTotal;
  const winPct = grand > 0 ? (winTotal / grand) * 100 : 50;

  // 막대 안에 수치를 오른쪽 정렬로 겹쳐 넣는다(막대 밖으로 숫자가 튀지 않게).
  const teamBars = (rows, color) => (
    <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 7, minWidth: 0 }}>
      {rows.map((r, i) => {
        const v = Number(valueOf(r) || 0);
        const w = Math.max(2, Math.round((v / maxVal) * 100));
        return (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <ChampionIcon championId={r.championId} championKeyById={championKeyById}
              championName={r.championName} size={22} />
            <div style={{ flex: 1, height: 18, background: 'rgba(255,255,255,0.05)', borderRadius: 3, overflow: 'hidden', position: 'relative' }}>
              <div style={{ width: `${w}%`, height: '100%', background: color, borderRadius: 3 }} />
              <span style={{
                position: 'absolute', right: 6, top: 0, lineHeight: '18px',
                fontSize: 11, fontWeight: 700, color: '#fff', fontVariantNumeric: 'tabular-nums',
                textShadow: '0 1px 2px rgba(0,0,0,0.65)',
              }}>{v.toLocaleString()}</span>
            </div>
          </div>
        );
      })}
    </div>
  );

  return (
    <div style={{ padding: '4px 6px' }}>
      <div style={{ textAlign: 'center', color: T.txtSub, fontSize: 13, fontWeight: 700, marginBottom: 12 }}>{title}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {teamBars(winRows, T.blue)}
        {/* 중앙 도넛 — 팀 합계 비율 */}
        <div style={{
          flexShrink: 0, width: 84, height: 84, borderRadius: '50%',
          background: `conic-gradient(${T.blue} 0 ${winPct}%, ${T.red} ${winPct}% 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{
            width: 60, height: 60, borderRadius: '50%', background: T.bg,
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          }}>
            <span style={{ color: T.blue, fontWeight: 800, fontSize: 13 }}>{winTotal.toLocaleString()}</span>
            <div style={{ width: 26, height: 1, background: T.border, margin: '2px 0' }} />
            <span style={{ color: T.red, fontWeight: 800, fontSize: 13 }}>{loseTotal.toLocaleString()}</span>
          </div>
        </div>
        {teamBars(loseRows, T.red)}
      </div>
    </div>
  );
}

function TeamAnalysis({ rows, championKeyById, championNameById, matchId }) {
  const [subTab, setSubTab] = useState('경기 분석');
  const [timelineOpen, setTimelineOpen] = useState(false);
  const winRows = rows.filter(r => r.win);
  const loseRows = rows.filter(r => !r.win);
  const winTeamId = winRows[0]?.teamId ?? null;

  const SUBS = ['경기 분석', '타임라인'];

  return (
    <div style={{ background: T.tabBg }}>
      {/* 서브탭도 메인 탭과 동일하게 구분선 + 활성 배경 강조 */}
      <div style={{ display: 'flex', background: T.tabBg, borderBottom: `1px solid ${T.sectionLine}` }}>
        {SUBS.map((label, i) => {
          const on = subTab === label && !(label === '타임라인');
          return (
            <button key={label}
              onClick={() => (label === '타임라인' ? setTimelineOpen(true) : setSubTab(label))}
              style={{
                flex: 1, padding: '10px 4px', cursor: 'pointer', border: 'none',
                borderRight: i < SUBS.length - 1 ? '1px solid rgba(255,255,255,0.06)' : 'none',
                background: on ? T.tabActive : 'transparent',
                color: on ? T.blue : T.txtSub, fontWeight: on ? 700 : 500, fontSize: 13,
                borderBottom: on ? `2px solid ${T.blue}` : '2px solid transparent', fontFamily: 'inherit',
              }}>{label}</button>
          );
        })}
      </div>

      {subTab === '경기 분석' ? (
        <div style={{ padding: '16px' }}>
          {/* 범례 */}
          <div style={{ display: 'flex', justifyContent: 'center', gap: 20, marginBottom: 18, fontSize: 12 }}>
            <span style={{ color: T.txtSub }}>
              <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', background: T.blue, marginRight: 5 }} />승리팀
            </span>
            <span style={{ color: T.txtSub }}>
              <span style={{ display: 'inline-block', width: 8, height: 8, borderRadius: '50%', background: T.red, marginRight: 5 }} />패배팀
            </span>
          </div>
          {/* 2열 패널 그리드 */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(430px, 1fr))', gap: '26px 34px' }}>
            {TEAM_STATS.map(s => (
              <StatComparePanel key={s.title} title={s.title} valueOf={s.valueOf}
                winRows={winRows} loseRows={loseRows} championKeyById={championKeyById} />
            ))}
          </div>
        </div>
      ) : null}

      {timelineOpen && (
        <TimelineModal
          matchId={matchId}
          winTeamId={winTeamId}
          championKeyById={championKeyById}
          championNameById={championNameById}
          onClose={() => setTimelineOpen(false)}
        />
      )}
    </div>
  );
}

/* ═══════════════════════════════════════════════════════════════
   매치 카드 (요약 행 + 드롭다운)
════════════════════════════════════════════════════════════════ */
function MatchCard({ match, championKeyById, championNameById, spellMap, runeIconById, styleIconById, runeTree,
  onSummonerClick, onToggle, isExpanded, summaryLoading, summaryRows }) {

  const [detailTab, setDetailTab] = useState('종합');

  const isRemake    = match.gameEndedInEarlySurrender;
  /* 아레나면 승/패 대신 등수(N위)로 표시 */
  const arenaPlacement = match.mySubteamPlacement ?? match.myPlacement;
  const isArena     = isArenaMatch(match.queueId, match.gameMode) && arenaPlacement != null;
  /* 아레나는 1~3위를 승리, 4위 이하를 패배 스타일로 표시한다 */
  const arenaWin    = isArena && arenaPlacement <= 3;
  /* 승/패 스타일 공통 플래그 (아레나는 등수 기준, 그 외는 myWin) */
  const win         = isArena ? arenaWin : match.myWin;
  // 내 듀오 팀 이름 — 미니 목록(participantSummaryDtos)에서 내 playerSubteamId로 역산
  const myArenaTeam = isArena
    ? arenaTeamName(match.participantSummaryDtos?.find(r => r.puuid === match.myPuuid)?.playerSubteamId)
    : null;

  const resultText  = isRemake ? '다시하기'
    : isArena ? `${arenaPlacement}위`
    : match.myWin ? '승리' : '패배';
  const accent      = isRemake ? '#5a6270'
    : win ? T.blue : T.red;
  const cardBg      = isRemake ? 'rgba(255,255,255,0.04)'
    : win ? 'rgba(83,131,243,0.20)' : 'rgba(232,64,87,0.20)';
  /* 토글 스트립 배경 (승/패 색의 옅은 톤) */
  const accentSoft      = isRemake ? 'rgba(255,255,255,0.04)'
    : win ? 'rgba(83,131,243,0.14)' : 'rgba(232,64,87,0.14)';
  const accentSoftHover = isRemake ? 'rgba(255,255,255,0.09)'
    : win ? 'rgba(83,131,243,0.24)' : 'rgba(232,64,87,0.24)';

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
        padding: '10px 14px', gap: 10,
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
          {isArena && myArenaTeam && (
            <div style={{ color: accent, fontSize: 10, fontWeight: 700, marginTop: 1 }}>{myArenaTeam}팀</div>
          )}
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

        {/* 룬 (핵심룬 + 보조 계열) — 아레나는 룬이 없어 빈 동그라미가 떠서 숨긴다 */}
        {!isArena && (
          <KeystoneRunes
            keystoneId={match.myKeystoneId}
            subStyleId={match.mySubStyleId}
            runeIconById={runeIconById}
            styleIconById={styleIconById}
          />
        )}

        {/* KDA (가운데 정렬, 좁은 컬럼) */}
        <div style={{ flexShrink: 0, width: 96, textAlign: 'center' }}>
          <div style={{ fontSize: 16, fontWeight: 700, whiteSpace: 'nowrap' }}>
            <span style={{ color: T.txtKills }}>{match.myKills}</span>
            <span style={{ color: T.txtMuted }}> / </span>
            <span style={{ color: T.txtDeaths }}>{match.myDeaths}</span>
            <span style={{ color: T.txtMuted }}> / </span>
            <span style={{ color: T.txtAssists }}>{match.myAssists}</span>
          </div>
          <div style={{ marginTop: 3, fontSize: 12 }}>
            <span style={{ color: mKdaColor, fontWeight: 700 }}>{mKdaStr}</span>
            <span style={{ color: T.txtMuted, fontSize: 11 }}> 평점</span>
          </div>
        </div>

        {/* 스탯: 킬관여 / CS / 평균티어 (KDA와 분리해 가로 폭을 채움) */}
        <div style={{ flexShrink: 0, width: 122, display: 'flex',
          flexDirection: 'column', gap: 3, fontSize: 11, color: T.txtSub }}>
          <div>킬관여 <span style={{ color: T.txtPrimary, fontWeight: 600 }}>{myKillPct}%</span></div>
          <div>
            CS <span style={{ color: T.txtPrimary, fontWeight: 600 }}>{myCs}</span>
            {myCsPerMin && <span style={{ color: T.txtMuted }}> ({myCsPerMin})</span>}
          </div>
          {/* 평균 티어 (랭크 큐일 때만) */}
          <AverageTierBadge tier={match.myTier} rank={match.myRank} />
        </div>

        {/* 아이템 */}
        <div style={{ flexShrink: 0 }}>
          <ItemSlots itemIds={items} />
        </div>

        {/* 신축 스페이서 — 내 정보(좌)와 팀 목록/버튼(우) 사이 잔여 공간만 흡수(작게) */}
        <div style={{ flex: 1, minWidth: 16 }} />

        {/* 미니 참가자 목록 — 아레나는 등수별 듀오, 그 외는 블루/레드 */}
        {match.participantSummaryDtos?.length > 0 && (
          isArena ? (
            <ArenaMiniList
              parts={match.participantSummaryDtos}
              championKeyById={championKeyById}
              myPuuid={match.myPuuid}
              onSummonerClick={onSummonerClick}
            />
          ) : (
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
                    <span
                      onClick={(e) => { e.stopPropagation(); onSummonerClick?.(r.gameName, r.tagLine); }}
                      title={`${r.gameName}#${r.tagLine}`}
                      style={{
                        fontSize: 11, color: r.puuid === match.myPuuid ? T.txtName : T.txtSub,
                        fontWeight: r.puuid === match.myPuuid ? 700 : 400,
                        whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis',
                        maxWidth: 66, cursor: 'pointer',
                      }}
                      onMouseEnter={e => { e.currentTarget.style.textDecoration = 'underline'; }}
                      onMouseLeave={e => { e.currentTarget.style.textDecoration = 'none'; }}
                    >{r.gameName}</span>
                  </div>
                ))}
              </div>
            ))}
          </div>
          )
        )}

        {/* 토글 버튼 — 우측 끝 전체 높이 스트립 + 승/패 색 체브론 */}
        <button onClick={() => onToggle(match.matchId)}
          aria-label={isExpanded ? '접기' : '상세 보기'}
          style={{
            alignSelf: 'stretch',
            margin: '-10px -14px -10px 0',   // 요약 행 패딩 상쇄 → 카드 우측 끝까지 채움
            padding: '0 14px',
            background: accentSoft,
            border: 'none', borderLeft: `1px solid ${T.border}`,
            cursor: 'pointer', flexShrink: 0,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            transition: 'background 0.15s',
          }}
          onMouseEnter={e => { e.currentTarget.style.background = accentSoftHover; }}
          onMouseLeave={e => { e.currentTarget.style.background = accentSoft; }}
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
            style={{ transform: isExpanded ? 'rotate(180deg)' : 'none',
              transition: 'transform 0.2s' }}>
            <path d="M6 9l6 6 6-6" stroke={accent} strokeWidth="2.5"
              strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </button>
      </div>

      {/* ── 상세 드롭다운 ── */}
      {isExpanded && (
        <div style={{ borderTop: `1px solid ${T.border}` }}>
          {summaryLoading
            ? <div style={{ color: T.txtMuted, fontSize: 13, padding: '18px 16px' }}>
                조회 중...
              </div>
            : isArena
              // 아레나는 표준 탭(종합/팀 분석/빌드)이 맞지 않아 탭 없이 스코어보드만 보여준다.
              ? <DetailTable
                  rows={summaryRows}
                  championKeyById={championKeyById}
                  spellMap={spellMap}
                  runeIconById={runeIconById}
                  styleIconById={styleIconById}
                  onSummonerClick={onSummonerClick}
                  myPuuid={match.myPuuid}
                />
              : (
                <>
                  <DetailTabs active={detailTab} onChange={setDetailTab} />
                  {detailTab === '종합' && (
                    <DetailTable
                      rows={summaryRows}
                      championKeyById={championKeyById}
                      spellMap={spellMap}
                      runeIconById={runeIconById}
                      styleIconById={styleIconById}
                      onSummonerClick={onSummonerClick}
                      myPuuid={match.myPuuid}
                    />
                  )}
                  {detailTab === '빌드' && (
                    <BuildView
                      row={summaryRows.find(r => r.puuid === match.myPuuid)}
                      championKeyById={championKeyById}
                      runeIconById={runeIconById}
                      styleIconById={styleIconById}
                      runeTree={runeTree}
                    />
                  )}
                  {detailTab === '팀 분석' && (
                    <TeamAnalysis rows={summaryRows} championKeyById={championKeyById}
                      championNameById={championNameById} matchId={match.matchId} />
                  )}
                </>
              )
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
  const navigate = useNavigate();
  const { region } = useParams(); // 현재 소환사 페이지의 리전(/find/:region/:slug). 같은 리전으로 이동시킨다.
  const regionLower = (region || 'kr').toLowerCase();

  // 참가자 이름 클릭 → 해당 소환사 페이지로 이동. DB에 없으면 백엔드가 Riot에서 기본 정보를 받아 저장한다.
  const goToSummoner = (gameName, tagLine) => {
    if (!gameName) return;
    const slug = tagLine ? `${gameName}-${tagLine}` : gameName;
    navigate(`/find/${regionLower}/${encodeURIComponent(slug)}`);
  };

  const [expandedMap,       setExpandedMap]      = useState({});
  const [summaryMap,        setSummaryMap]        = useState({});
  const [summaryLoadingMap, setSummaryLoadingMap] = useState({});
  const [spellMap,          setSpellMap]          = useState({});
  const [championKeyById,   setChampionKeyById]   = useState({});
  const [championNameById,  setChampionNameById]  = useState({});
  const [runeIconById,      setRuneIconById]       = useState({});
  const [styleIconById,     setStyleIconById]      = useState({});
  /* 룬 트리 원본 구조 (빌드 탭 룬 페이지 전체 렌더용) */
  const [runeTree,          setRuneTree]           = useState([]);
  /* 툴팁용 이름 맵 (스펠/룬/계열/아이템 → 이름) */
  const [spellNameById,     setSpellNameById]      = useState({});
  const [runeNameById,      setRuneNameById]       = useState({});
  const [styleNameById,     setStyleNameById]      = useState({});
  const [itemNameById,      setItemNameById]       = useState({});
  /* 툴팁용 설명 맵 (스펠/룬/아이템 → 설명, 부룬 계열은 이름만이라 제외) */
  const [spellDescById,     setSpellDescById]      = useState({});
  const [runeDescById,      setRuneDescById]       = useState({});
  const [itemDescById,      setItemDescById]       = useState({});
  /* 아이템 가격 맵 (id → { total: 구매가, sell: 되팔기가 }) */
  const [itemGoldById,      setItemGoldById]       = useState({});

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const loadLocale = async (locale) => {
        const [s, c, r, it] = await Promise.all([
          getSummonerSpellData(locale),
          getChampionData(locale),
          getRuneData(locale),
          getItemData(locale),
        ]);
        return [s.data, c.data, r.data, it.data];
      };
      try {
        const [sj, cj, rj, ij] = await loadLocale('ko_KR').catch(() => loadLocale('en_US'));
        const spells = {};
        const spellNames = {};
        const spellDescs = {};
        Object.values(sj.data).forEach(s => {
          const sid = Number(s.key);
          if (!isNaN(sid)) {
            spells[sid] = s.image.full;
            spellNames[sid] = s.name;
            spellDescs[sid] = stripHtml(s.description);
          }
        });
        const champs = {};
        const champNames = {}; // championId → 현지화(한글) 이름. champion.json을 ko_KR로 받으므로 c.name이 한글.
        Object.values(cj.data).forEach(c => {
          champs[Number(c.key)] = c.id;
          champNames[Number(c.key)] = c.name;
        });
        const { runeIconById: runeMap, styleIconById: styleMap,
          runeNameById: runeNames, styleNameById: styleNames,
          runeDescById: runeDescs } = buildRuneMaps(rj);
        const itemNames = {};
        const itemDescs = {};
        const itemGolds = {};
        Object.entries(ij.data).forEach(([id, item]) => {
          itemNames[Number(id)] = item.name;
          itemDescs[Number(id)] = stripHtml(item.description) || item.plaintext || '';
          if (item.gold) {
            itemGolds[Number(id)] = { total: item.gold.total ?? 0, sell: item.gold.sell ?? 0 };
          }
        });
        if (!cancelled) {
          setSpellMap(spells);
          setChampionKeyById(champs);
          setChampionNameById(champNames);
          setRuneIconById(runeMap);
          setStyleIconById(styleMap);
          setRuneTree(rj);
          setSpellNameById(spellNames);
          setRuneNameById(runeNames);
          setStyleNameById(styleNames);
          setItemNameById(itemNames);
          setSpellDescById(spellDescs);
          setRuneDescById(runeDescs);
          setItemDescById(itemDescs);
          setItemGoldById(itemGolds);
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
    <MetaContext.Provider value={{
      spellNameById, runeNameById, styleNameById, itemNameById,
      spellDescById, runeDescById, itemDescById, itemGoldById,
      championNameById,
    }}>
      <div style={{
        fontFamily: 'Pretendard, "Apple SD Gothic Neo", -apple-system, sans-serif',
      }}>
        {matches.map(m => (
          <MatchCard
            key={m.matchId} match={m}
            championKeyById={championKeyById} championNameById={championNameById} spellMap={spellMap}
            runeIconById={runeIconById} styleIconById={styleIconById}
            runeTree={runeTree}
            onSummonerClick={goToSummoner}
            onToggle={toggleSummary}
            isExpanded={!!expandedMap[m.matchId]}
            summaryLoading={!!summaryLoadingMap[m.matchId]}
            summaryRows={summaryMap[m.matchId] ?? []}
          />
        ))}
      </div>
    </MetaContext.Provider>
  );
}
