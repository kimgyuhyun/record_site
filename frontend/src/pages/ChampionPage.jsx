import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import useChampionMeta from '../hooks/useChampionMeta';
import { getChampionDetail as getChampionStatsDetail } from '../api/champion';
import {
  getChampionDetail as getChampionDdragon,
  getSummonerSpellData,
  getRuneData,
} from '../api/ddragon';
import {
  DATA_CDN, DDRAGON_VERSION, imgChampion, imgItem, imgSpell, imgChampionSpell, imgRune,
} from '../constants/ddragon';

// 화면에 표기하는 패치 버전(예: '16.12.1' → '16.12')
const PATCH = DDRAGON_VERSION.split('.').slice(0, 2).join('.');

/*
 * 챔피언 상세 — 초상화 클릭 시 진입. 우리 매치 DB 집계(룬/스킬/아이템/스펠/카운터/장인)에
 * DDragon 정적 데이터(스킬·스킨)를 합쳐 보여준다. 통계 표본이 적으면 일부 섹션은 비어 있을 수 있다.
 */

const POSITION_LABEL = {
  TOP: '탑', JUNGLE: '정글', MIDDLE: '미드', BOTTOM: '바텀', UTILITY: '서포터',
};
const QUEUE_FILTERS = [
  { key: undefined, label: '전체' },
  { key: 'SOLO', label: '솔로랭크' },
  { key: 'FLEX', label: '자유랭크' },
];
const SKILL_KEYS = ['Q', 'W', 'E', 'R'];
const SKILL_COLOR = { Q: '#3b7dd8', W: '#1f9e63', E: '#9b46d6', R: '#d9a514' };

const pct = (r, d = 0) => `${(r * 100).toFixed(d)}%`;
const winColor = (r) => (r >= 0.55 ? '#e84057' : r >= 0.5 ? '#e8a020' : '#8fa3bd');

export default function ChampionPage() {
  const { championId: championIdParam } = useParams();
  const championId = Number(championIdParam);
  const { championKeyById, championNameById } = useChampionMeta();
  const champKey = championKeyById[championId];
  const champName = championNameById[championId];

  const [queueType, setQueueType] = useState(undefined);
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const [ddragon, setDdragon] = useState(null);          // { spells:[], passive:{}, skins:[] }
  const [spellFileById, setSpellFileById] = useState({}); // 소환사 주문 id → 아이콘 파일명
  const [runeIconById, setRuneIconById] = useState({});   // 룬/계열 id → 아이콘 경로
  const [runeTree, setRuneTree] = useState([]);           // 룬 전체 트리(계열→슬롯→룬) 구조

  // 1) 백엔드 집계
  useEffect(() => {
    if (!championId) return undefined;
    let cancelled = false;
    const load = async () => {
      setLoading(true);
      setError(false);
      try {
        const res = await getChampionStatsDetail(championId, queueType);
        if (!cancelled) setDetail(res.data);
      } catch {
        if (!cancelled) setError(true);
      } finally {
        if (!cancelled) setLoading(false);
      }
    };
    load();
    return () => { cancelled = true; };
  }, [championId, queueType]);

  // 2) DDragon 챔피언 상세(스킬/스킨) — 키가 잡힌 뒤
  useEffect(() => {
    if (!champKey) return;
    let cancelled = false;
    getChampionDdragon('ko_KR', champKey)
      .catch(() => getChampionDdragon('en_US', champKey))
      .then(res => { if (!cancelled) setDdragon(res.data.data[champKey]); })
      .catch(() => { if (!cancelled) setDdragon(null); });
    return () => { cancelled = true; };
  }, [champKey]);

  // 3) 소환사 주문 / 룬 아이콘 매핑 (1회)
  useEffect(() => {
    let cancelled = false;
    getSummonerSpellData('ko_KR').catch(() => getSummonerSpellData('en_US'))
      .then(res => {
        if (cancelled) return;
        const map = {};
        Object.values(res.data.data).forEach(s => { map[Number(s.key)] = s.image.full; });
        setSpellFileById(map);
      }).catch(() => {});

    getRuneData('ko_KR').catch(() => getRuneData('en_US'))
      .then(res => {
        if (cancelled) return;
        const map = {};
        res.data.forEach(style => {
          map[style.id] = style.icon;
          style.slots.forEach(slot => slot.runes.forEach(r => { map[r.id] = r.icon; }));
        });
        setRuneIconById(map);
        setRuneTree(res.data);
      }).catch(() => {});
    return () => { cancelled = true; };
  }, []);

  if (!championId) return <Notice>잘못된 챔피언입니다.</Notice>;

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '20px 20px 56px' }}>
      <Header
        champKey={champKey} champName={champName || detail?.championName}
        detail={detail} queueType={queueType} onQueueChange={setQueueType}
      />

      {error && <Notice>통계를 불러오지 못했습니다.</Notice>}
      {!error && loading && <Notice>불러오는 중…</Notice>}

      {!error && !loading && detail && (
        detail.games === 0 ? (
          <Notice>아직 집계된 매치가 부족합니다. 소환사 검색이 쌓이면 자동으로 채워집니다.</Notice>
        ) : (
          <div style={{ marginTop: 16 }}>
            <Runes runes={detail.runes} totalGames={detail.games}
              runeTree={runeTree} runeIconById={runeIconById} />
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16,
              alignItems: 'start', marginTop: 16 }}>
              <Abilities ddragon={ddragon} />
              <SkillOrders orders={detail.skillOrders} />
              <ItemBuilds startingItems={detail.startingItems} boots={detail.boots} />
              <CoreBuild builds={detail.coreItems} />
              <Spells spells={detail.spells} spellFileById={spellFileById} />
              <Experts experts={detail.experts} />
            </div>
          </div>
        )
      )}
    </div>
  );
}

/* ── 헤더(요약) ── op.gg 챔피언 빌드 헤더 스타일 */
function Header({ champKey, champName, detail, queueType, onQueueChange }) {
  const position = detail?.primaryPosition ? POSITION_LABEL[detail.primaryPosition] : null;
  return (
    <div style={{
      background: 'linear-gradient(135deg,#0f1923,#1a2535)',
      border: '1px solid #2a3a4a', borderRadius: 12, overflow: 'hidden',
    }}>
      {/* 상단 필터 바 — 큐 선택 탭 + 패치 표기 */}
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 16px', borderBottom: '1px solid #1f2a3a', background: '#0d1622',
      }}>
        <div style={{ display: 'inline-flex', background: '#151d2e',
          border: '1px solid #2a3a4a', borderRadius: 8, padding: 3, gap: 2 }}>
          {QUEUE_FILTERS.map(q => {
            const active = q.key === queueType;
            return (
              <button key={q.label} onClick={() => onQueueChange(q.key)} style={{
                background: active ? '#5383e8' : 'transparent',
                color: active ? '#fff' : '#8899aa',
                border: 'none', fontSize: 12.5, fontWeight: 700,
                padding: '6px 14px', borderRadius: 6, cursor: 'pointer', fontFamily: 'inherit',
              }}>{q.label}</button>
            );
          })}
        </div>
        <span style={{ color: '#5a6b7e', fontSize: 12, fontWeight: 600 }}>패치 {PATCH}</span>
      </div>

      {/* 본문 — 초상화 + 이름/포지션 + 승률/픽률/밴율 */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 18, padding: '18px 20px' }}>
        {champKey && <img src={imgChampion(champKey)} alt={champName} width={72} height={72}
          style={{ borderRadius: 12, border: '2px solid #c89b3c' }} />}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ color: '#e8edf4', fontSize: 22, fontWeight: 800 }}>{champName || '알 수 없음'}</div>
          <div style={{ color: '#8899aa', fontSize: 13, marginTop: 4,
            display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            {position && (
              <span style={{
                color: '#cdd7e2', fontSize: 12, fontWeight: 700, background: '#22304a',
                border: '1px solid #2f4060', borderRadius: 5, padding: '2px 8px',
              }}>{position}</span>
            )}
            <span>빌드 · 패치 {PATCH}</span>
            <span style={{ color: '#5a6b7e' }}>표본 {(detail?.games ?? 0).toLocaleString()}게임</span>
          </div>
        </div>
        {detail && (
          <div style={{ display: 'flex' }}>
            <Stat label="승률" value={pct(detail.winRate, 1)} color={winColor(detail.winRate)} />
            <Stat label="픽률" value={pct(detail.pickRate, 1)} divider />
            <Stat label="밴율" value={pct(detail.banRate, 1)} divider />
          </div>
        )}
      </div>
    </div>
  );
}

function Stat({ label, value, color = '#e8edf4', divider }) {
  return (
    <div style={{
      textAlign: 'center', padding: '0 18px',
      borderLeft: divider ? '1px solid #233143' : 'none',
    }}>
      <div style={{ color, fontSize: 19, fontWeight: 800, lineHeight: 1.1 }}>{value}</div>
      <div style={{ color: '#6b7a8d', fontSize: 11, marginTop: 4 }}>{label}</div>
    </div>
  );
}

/* ── 검은 배경/흰 글씨 호버 툴팁(스킬·룬 공용) ── */
function Tooltip({ children, title, body, width = 260 }) {
  const [show, setShow] = useState(false);
  return (
    <span
      style={{ position: 'relative', display: 'inline-block' }}
      onMouseEnter={() => setShow(true)}
      onMouseLeave={() => setShow(false)}
    >
      {children}
      {show && (title || body) && (
        <span style={{
          position: 'absolute', bottom: '100%', left: '50%', transform: 'translateX(-50%)',
          marginBottom: 9, width, zIndex: 50, pointerEvents: 'none',
          background: '#000', color: '#fff', border: '1px solid #3a3a3a', borderRadius: 8,
          padding: '10px 12px', boxShadow: '0 8px 24px rgba(0,0,0,0.65)', textAlign: 'left',
          display: 'block', whiteSpace: 'normal',
        }}>
          {title && <span style={{ display: 'block', fontSize: 12.5, fontWeight: 800,
            color: '#fff', marginBottom: body ? 6 : 0 }}>{title}</span>}
          {body && <span style={{ display: 'block', fontSize: 11.5, lineHeight: 1.55, color: '#d6d6d6' }}
            dangerouslySetInnerHTML={{ __html: body }} />}
          <span style={{
            position: 'absolute', top: '100%', left: '50%', transform: 'translateX(-50%)',
            width: 0, height: 0, borderLeft: '6px solid transparent',
            borderRight: '6px solid transparent', borderTop: '6px solid #000',
          }} />
        </span>
      )}
    </span>
  );
}

/* ── 스킬(정적) ── */
function Abilities({ ddragon }) {
  return (
    <Card title="스킬">
      {!ddragon ? <Empty /> : (
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <AbilityIcon label="P" name={ddragon.passive?.name} desc={ddragon.passive?.description}
            src={ddragon.passive ? `${DATA_CDN}/img/passive/${ddragon.passive.image.full}` : null} />
          {(ddragon.spells || []).map((s, i) => (
            <AbilityIcon key={s.id} label={SKILL_KEYS[i]} name={s.name} desc={s.description}
              src={imgChampionSpell(s.image.full)} />
          ))}
        </div>
      )}
    </Card>
  );
}

function AbilityIcon({ label, name, desc, src }) {
  return (
    <Tooltip title={name} body={desc}>
      <span style={{ display: 'inline-block', width: 56, textAlign: 'center', cursor: 'help' }}>
        <span style={{ position: 'relative', display: 'block', width: 48, margin: '0 auto' }}>
          {src && <img src={src} alt={name} width={48} height={48}
            style={{ borderRadius: 8, border: '1px solid #2a3a4a', display: 'block' }} />}
          <span style={{
            position: 'absolute', bottom: -4, right: -4, background: '#0f1923',
            color: '#c89b3c', fontSize: 10, fontWeight: 700, padding: '0 4px',
            borderRadius: 4, border: '1px solid #c89b3c',
          }}>{label}</span>
        </span>
        <span style={{ display: 'block', color: '#9aa7b4', fontSize: 10.5, marginTop: 6,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</span>
      </span>
    </Tooltip>
  );
}

/* ── 룬 세팅 ── op.gg 룬 상세(전체 트리) 스타일
 * 왼쪽: 집계된 룬 페이지 후보(승률순) — 선택 가능.
 * 본문: 선택한 페이지의 주룬 계열 전체 트리 + 보조 계열 + 능력치 파편.
 * (룬 트리 구조는 DDragon, 강조되는 룬은 우리 DB 집계 결과)
 */

// 능력치 파편 — DDragon 룬 트리에는 없어 고정 정의. 빌드의 statOffense/Flex/Defense id 로 강조.
const STAT_SHARD_ICON = {
  5008: 'perk-images/StatMods/StatModsAdaptiveForceIcon.png',
  5005: 'perk-images/StatMods/StatModsAttackSpeedIcon.png',
  5007: 'perk-images/StatMods/StatModsCDRScalingIcon.png',
  5002: 'perk-images/StatMods/StatModsArmorIcon.png',
  5003: 'perk-images/StatMods/StatModsMagicResIcon.png',
  5001: 'perk-images/StatMods/StatModsHealthScalingIcon.png',
  5011: 'perk-images/StatMods/StatModsHealthPlusIcon.png',
  5013: 'perk-images/StatMods/StatModsTenacityIcon.png',
  5010: 'perk-images/StatMods/StatModsMovementSpeedIcon.png',
};
const STAT_SHARD_LABEL = {
  5008: '적응형 능력치', 5005: '공격 속도', 5007: '스킬 가속',
  5002: '방어력', 5003: '마법 저항력', 5001: '체력(성장)',
  5011: '체력', 5013: '강인함 및 둔화 저항', 5010: '이동 속도',
};
// 능력치 파편 3x3 고정 그리드(공격/유연/방어). 빌드의 선택 id 와 일치하는 칸만 강조한다.
const STAT_SHARD_ROWS = [
  [5008, 5005, 5007],
  [5008, 5010, 5001],
  [5011, 5013, 5001],
];

function Runes({ runes, totalGames, runeTree, runeIconById }) {
  const [sel, setSel] = useState(0);
  if (!runes?.length) return <Card title="룬 세팅"><Empty /></Card>;

  const top = runes.slice(0, 2);           // 상위 2개 룬 페이지만 가로로 노출
  const idx = Math.min(sel, top.length - 1);
  const build = top[idx];
  const primary = runeTree.find(s => s.id === build.primaryStyleId);
  const secondary = runeTree.find(s => s.id === build.subStyleId);

  const primaryPicks = new Set([build.keystoneId, build.primaryRune1, build.primaryRune2, build.primaryRune3]);
  const secondaryPicks = new Set([build.subRune1, build.subRune2]);
  const shards = [build.statOffense, build.statFlex, build.statDefense];

  return (
    <Card title="룬 세팅">
      {/* 빈도 상위 1·2위 룬 페이지를 가로로 2개 */}
      <div style={{ display: 'flex', gap: 10, marginBottom: 18 }}>
        {top.map((r, i) => {
          const active = i === idx;
          const freq = totalGames > 0 ? r.games / totalGames : 0;
          return (
            <button key={i} onClick={() => setSel(i)} style={{
              flex: 1, display: 'flex', alignItems: 'center', gap: 12, padding: '10px 14px',
              background: active ? '#11203a' : '#101826',
              border: `1px solid ${active ? '#3d6fd6' : '#1f2a3a'}`,
              borderRadius: 8, cursor: 'pointer', textAlign: 'left', fontFamily: 'inherit',
            }}>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                <RuneIcon id={r.primaryStyleId} runeIconById={runeIconById} size={20} />
                <RuneIcon id={r.keystoneId} runeIconById={runeIconById} size={30} />
                <RuneIcon id={r.subStyleId} runeIconById={runeIconById} size={20} />
              </span>
              <span style={{ lineHeight: 1.25 }}>
                <span style={{ display: 'block', color: '#e8edf4', fontSize: 16, fontWeight: 800 }}>
                  {pct(freq, 1)}
                </span>
                <span style={{ display: 'block', color: '#5a6b7e', fontSize: 11 }}>
                  {r.games.toLocaleString()} 게임
                </span>
              </span>
              <span style={{ marginLeft: 'auto', color: winColor(r.winRate), fontSize: 14, fontWeight: 800 }}>
                {pct(r.winRate, 1)}
              </span>
            </button>
          );
        })}
      </div>

      {/* 선택한 페이지의 전체 트리: 주룬 | 보조 | 능력치 파편 */}
      <div style={{ display: 'flex', alignItems: 'stretch' }}>
        <RuneTreeColumn style={primary} picks={primaryPicks} keystone flex={1.25} />
        <Divider />
        <RuneTreeColumn style={secondary} picks={secondaryPicks} flex={1} />
        <Divider />
        <ShardColumn shards={shards} flex={1} />
      </div>
    </Card>
  );
}

function Divider() {
  return <span style={{ width: 1, background: '#1a2433', margin: '0 6px', alignSelf: 'stretch' }} />;
}

// 한 계열(주룬 또는 보조)의 트리. keystone=true 면 첫 슬롯(핵심 룬)부터, 아니면 핵심 룬 슬롯 제외.
function RuneTreeColumn({ style, picks, keystone, flex }) {
  if (!style) return <div style={{ flex }}><Empty /></div>;
  const slots = keystone ? style.slots : style.slots.slice(1);
  return (
    <div style={{ flex, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14, flex: 1 }}>
        {slots.map((slot, si) => {
          const big = keystone && si === 0;
          return (
            <div key={si} style={{ display: 'flex', gap: 12, justifyContent: 'center',
              alignItems: 'center',
              ...(big ? { paddingBottom: 14, borderBottom: '1px solid #1a2433' } : {}) }}>
              {slot.runes.map(rune => {
                const on = picks.has(rune.id);
                const size = big ? 40 : 28;
                return (
                  <Tooltip key={rune.id} title={rune.name} body={rune.shortDesc} width={250}>
                    <span style={{ display: 'inline-block', cursor: 'help',
                      opacity: on ? 1 : 0.3, filter: on ? 'none' : 'grayscale(1)' }}>
                      <img src={imgRune(rune.icon)} alt={rune.name} width={size} height={size}
                        style={{
                          borderRadius: '50%', display: 'block', background: '#0d1520',
                          border: on ? '2px solid #c89b3c' : '2px solid transparent',
                          boxSizing: 'border-box',
                        }} />
                    </span>
                  </Tooltip>
                );
              })}
            </div>
          );
        })}
      </div>
      <TreeLabel>{style.name}</TreeLabel>
    </div>
  );
}

// 능력치 파편 3x3 그리드 — 선택된 칸만 강조.
function ShardColumn({ shards, flex }) {
  return (
    <div style={{ flex, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14, flex: 1 }}>
        {STAT_SHARD_ROWS.map((row, ri) => (
          <div key={ri} style={{ display: 'flex', gap: 12, justifyContent: 'center', alignItems: 'center' }}>
            {row.map((id, ci) => {
              const on = shards[ri] === id;
              const icon = STAT_SHARD_ICON[id];
              return (
                <Tooltip key={ci} title={STAT_SHARD_LABEL[id] || '능력치 파편'} width={160}>
                  <span style={{ display: 'inline-block', cursor: 'help',
                    opacity: on ? 1 : 0.3, filter: on ? 'none' : 'grayscale(1)' }}>
                    {icon
                      ? <img src={imgRune(icon)} alt="" width={26} height={26}
                          style={{ borderRadius: '50%', display: 'block', background: '#0d1520',
                            border: on ? '2px solid #c89b3c' : '2px solid transparent', boxSizing: 'border-box' }} />
                      : <span style={{ width: 26, height: 26, display: 'block', borderRadius: '50%',
                          background: '#0d1520', border: '1px solid #2a3a4a' }} />}
                  </span>
                </Tooltip>
              );
            })}
          </div>
        ))}
      </div>
      <TreeLabel>능력치 파편</TreeLabel>
    </div>
  );
}

function TreeLabel({ children }) {
  return (
    <div style={{ textAlign: 'center', color: '#aeb9c7', fontSize: 12, fontWeight: 700,
      marginTop: 14, paddingTop: 12, borderTop: '1px solid #1a2433' }}>
      {children}
    </div>
  );
}

function RuneIcon({ id, runeIconById, size }) {
  const src = id != null ? imgRune(runeIconById[id]) : null;
  if (!src) return <span style={{ width: size, height: size, display: 'inline-block' }} />;
  return <img src={src} alt="" width={size} height={size} style={{ borderRadius: '50%' }} />;
}

/* ── 스킬 빌드(선마 순서) ── */
function SkillOrders({ orders }) {
  return (
    <Card title="스킬 빌드 (선마 순서)">
      {!orders?.length ? <Empty /> : orders.map((o, i) => (
        <BuildRow key={i} games={o.games} wins={o.wins} winRate={o.winRate}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            {o.order.split('>').map((s, k) => (
              <React.Fragment key={k}>
                {k > 0 && <span style={{ color: '#3a4a5a' }}>›</span>}
                <span style={{
                  width: 24, height: 24, borderRadius: 5, background: SKILL_COLOR[s] || '#444',
                  color: '#fff', fontWeight: 800, fontSize: 13, display: 'flex',
                  alignItems: 'center', justifyContent: 'center',
                }}>{s}</span>
              </React.Fragment>
            ))}
          </div>
        </BuildRow>
      ))}
    </Card>
  );
}

/* ── 아이템 빌드 (시작 아이템 + 신발) ── */
function ItemBuilds({ startingItems, boots }) {
  return (
    <Card title="아이템 빌드">
      <SubLabel>시작 아이템</SubLabel>
      {!startingItems?.length ? <Empty /> : startingItems.map((b, i) => (
        <BuildRow key={i} games={b.games} wins={b.wins} winRate={b.winRate}>
          <ItemRow ids={b.items} />
        </BuildRow>
      ))}

      <SubLabel style={{ marginTop: 14 }}>신발</SubLabel>
      {!boots?.length ? <Empty /> : boots.map((b, i) => (
        <BuildRow key={i} games={b.games} wins={b.wins} winRate={b.winRate}>
          <ItemRow ids={b.items} />
        </BuildRow>
      ))}
    </Card>
  );
}

/* ── 핵심 빌드 (1·2·3 코어) ── */
function CoreBuild({ builds }) {
  return (
    <Card title="핵심 빌드">
      {!builds?.length ? <Empty /> : builds.map((b, i) => (
        <BuildRow key={i} games={b.games} wins={b.wins} winRate={b.winRate}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {b.items.map((id, k) => (
              <React.Fragment key={k}>
                {k > 0 && <span style={{ color: '#3a4a5a', fontSize: 16 }}>›</span>}
                <span style={{ textAlign: 'center' }}>
                  <img src={imgItem(id)} alt="" width={34} height={34}
                    style={{ borderRadius: 6, border: '1px solid #2a3a4a', display: 'block' }}
                    onError={e => { e.target.style.visibility = 'hidden'; }} />
                  <span style={{ display: 'block', color: '#5a6b7e', fontSize: 9.5, marginTop: 3 }}>
                    {k + 1}코어
                  </span>
                </span>
              </React.Fragment>
            ))}
          </div>
        </BuildRow>
      ))}
    </Card>
  );
}

function ItemRow({ ids }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      {ids.map((id, k) => (
        <img key={k} src={imgItem(id)} alt="" width={30} height={30}
          style={{ borderRadius: 6, border: '1px solid #2a3a4a' }}
          onError={e => { e.target.style.visibility = 'hidden'; }} />
      ))}
    </div>
  );
}

function SubLabel({ children, style }) {
  return (
    <div style={{ color: '#7d8ba0', fontSize: 11.5, fontWeight: 700, marginBottom: 6, ...style }}>
      {children}
    </div>
  );
}

/* ── 소환사 주문 ── */
function Spells({ spells, spellFileById }) {
  return (
    <Card title="소환사 주문">
      {!spells?.length ? <Empty /> : spells.map((s, i) => (
        <BuildRow key={i} games={s.games} wins={s.wins} winRate={s.winRate}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            {[s.spell1, s.spell2].map((id, k) => {
              const file = spellFileById[id];
              return file
                ? <img key={k} src={imgSpell(file)} alt="" width={28} height={28}
                    style={{ borderRadius: 6 }} />
                : <span key={k} style={{ width: 28, height: 28, background: '#2a3a4a', borderRadius: 6,
                    display: 'inline-block' }} />;
            })}
          </div>
        </BuildRow>
      ))}
    </Card>
  );
}

/* ── 장인 랭킹 ── */
function Experts({ experts }) {
  return (
    <Card title="장인 랭킹 (우리 DB 기준)">
      {!experts?.length ? <Empty /> : experts.map((e, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '7px 0',
          borderBottom: '1px solid #1a2433' }}>
          <span style={{ width: 18, color: i < 3 ? '#5383e8' : '#5a6b7e', fontWeight: 700,
            fontSize: 12 }}>{i + 1}</span>
          <span style={{ flex: 1, color: '#dbe2ea', fontSize: 13, overflow: 'hidden',
            textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {e.gameName}{e.tagLine ? <span style={{ color: '#5a6b7e' }}> #{e.tagLine}</span> : null}
          </span>
          <span style={{ color: winColor(e.winRate), fontWeight: 700, fontSize: 12.5 }}>{pct(e.winRate)}</span>
          <span style={{ color: '#5a6b7e', fontSize: 11.5, width: 44, textAlign: 'right' }}>{e.games}판</span>
        </div>
      ))}
    </Card>
  );
}

/* ── 공통 UI ── */
function Card({ title, children }) {
  return (
    <div style={{ background: '#151d2e', border: '1px solid #1f2a3a', borderRadius: 10, padding: '14px 16px' }}>
      <div style={{ color: '#aeb9c7', fontSize: 13, fontWeight: 700, marginBottom: 10 }}>{title}</div>
      {children}
    </div>
  );
}

function BuildRow({ children, games, wins, winRate }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 0',
      borderBottom: '1px solid #1a2433' }}>
      <div style={{ flex: 1, minWidth: 0 }}>{children}</div>
      <span style={{ color: winColor(winRate), fontWeight: 700, fontSize: 13 }}>{pct(winRate)}</span>
      <span style={{ color: '#5a6b7e', fontSize: 11.5, width: 64, textAlign: 'right' }}>
        {games}판 {wins}승
      </span>
    </div>
  );
}

function Empty({ hint = '데이터 없음' }) {
  return <div style={{ color: '#4a5568', fontSize: 12.5, padding: '14px 0' }}>{hint}</div>;
}

function Notice({ children }) {
  return <div style={{ color: '#5a6b7e', fontSize: 14, padding: '60px 0', textAlign: 'center' }}>{children}</div>;
}
