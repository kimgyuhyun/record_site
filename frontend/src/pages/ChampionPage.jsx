import React, { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { useParams } from 'react-router-dom';
import useChampionMeta from '../hooks/useChampionMeta';
import { getChampionDetail as getChampionStatsDetail } from '../api/champion';
import {
  getChampionDetail as getChampionDdragon,
  getSummonerSpellData,
  getRuneData,
  getItemData,
} from '../api/ddragon';
import {
  DDRAGON_VERSION, imgChampion, imgItem, imgSpell, imgChampionSpell, imgRune,
} from '../constants/ddragon';
import ChampionTips from '../components/champion/ChampionTips';

// 화면에 표기하는 패치 버전(예: '16.12.1' → '16.12')
const PATCH = DDRAGON_VERSION.split('.').slice(0, 2).join('.');

// 툴팁 설명용 — DDragon 설명 HTML 을 평문으로 정리(전적 빌드탭과 동일 방식).
const stripHtml = (s) => (s || '')
  .replace(/<br\s*\/?>/gi, '\n')
  .replace(/<\/?(li|p)>/gi, '\n')
  .replace(/<[^>]+>/g, '')
  .replace(/\n{2,}/g, '\n')
  .trim();

/*
 * 챔피언 상세 — 초상화 클릭 시 진입. 우리 매치 DB 집계(룬/스펠/스킬/아이템/장인)에
 * DDragon 정적 데이터(스킬 아이콘·룬 트리)를 합쳐 op.gg 빌드 탭처럼 세로 섹션으로 보여준다.
 */

const POSITION_LABEL = {
  TOP: '탑', JUNGLE: '정글', MIDDLE: '미드', BOTTOM: '바텀', UTILITY: '서포터',
};
const QUEUE_FILTERS = [
  { key: undefined, label: '전체' },
  { key: 'SOLO', label: '솔로랭크' },
  { key: 'FLEX', label: '자유랭크' },
];
// 스킬 레벨업 뱃지 글자색(Q/W/E/R)
const SKILL_COLOR = { Q: '#4d8ce0', W: '#36b37e', E: '#a772e6', R: '#e0c04d' };

// 회색 기반 팔레트 — 딥블루를 버리고 구분이 잘 되는 중성 회색으로.
const C = {
  card: '#2a2a30',       // 섹션 본문 배경
  head: '#212126',       // 섹션 제목 띠
  box: '#303037',        // 내부 셀(룬/스펠 옵션) 배경
  boxActive: '#37475e',  // 선택된 옵션 배경
  line: '#3a3a43',       // 구분선/테두리
  text: '#e6e6ea',
  sub: '#9a9aa3',
  muted: '#6c6c75',
  accent: '#5b9bd5',     // 선택 강조
};
const WIN = '#7ea9d8';   // 승률 텍스트 색(전부 동일, 빨강 안 씀)

const pct = (r, d = 2) => `${(r * 100).toFixed(d)}%`;

export default function ChampionPage() {
  const { championId: championIdParam } = useParams();
  const championId = Number(championIdParam);
  const { championKeyById, championNameById } = useChampionMeta();
  const champKey = championKeyById[championId];
  const champName = championNameById[championId] || '챔피언';

  const [queueType, setQueueType] = useState(undefined);
  const [detail, setDetail] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  const [ddragon, setDdragon] = useState(null);          // { spells:[], passive:{} }
  const [runeIconById, setRuneIconById] = useState({});   // 룬/계열 id → 아이콘 경로
  const [runeTree, setRuneTree] = useState([]);           // 룬 전체 트리(계열→슬롯→룬)
  // 툴팁/아이콘용 메타: 소환사 주문·아이템(이름/설명/가격)
  const [spellMeta, setSpellMeta] = useState({ fileById: {}, nameById: {}, descById: {} });
  const [itemMeta, setItemMeta] = useState({ nameById: {}, descById: {}, goldById: {} });

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

  // 2) DDragon 챔피언 상세(스킬 아이콘) — 키가 잡힌 뒤
  useEffect(() => {
    if (!champKey) return;
    let cancelled = false;
    getChampionDdragon('ko_KR', champKey)
      .catch(() => getChampionDdragon('en_US', champKey))
      .then(res => { if (!cancelled) setDdragon(res.data.data[champKey]); })
      .catch(() => { if (!cancelled) setDdragon(null); });
    return () => { cancelled = true; };
  }, [champKey]);

  // 3) 소환사 주문 / 룬 / 아이템 정적 데이터 (1회) — 아이콘 + 툴팁용 이름·설명·가격
  useEffect(() => {
    let cancelled = false;
    const loadLocale = (locale) => Promise.all([
      getSummonerSpellData(locale), getRuneData(locale), getItemData(locale),
    ]);
    loadLocale('ko_KR').catch(() => loadLocale('en_US'))
      .then(([spellRes, runeRes, itemRes]) => {
        if (cancelled) return;

        const fileById = {}, spellNameById = {}, spellDescById = {};
        Object.values(spellRes.data.data).forEach(s => {
          const id = Number(s.key);
          fileById[id] = s.image.full;
          spellNameById[id] = s.name;
          spellDescById[id] = stripHtml(s.description);
        });
        setSpellMeta({ fileById, nameById: spellNameById, descById: spellDescById });

        const iconById = {};
        runeRes.data.forEach(style => {
          iconById[style.id] = style.icon;
          style.slots.forEach(slot => slot.runes.forEach(r => { iconById[r.id] = r.icon; }));
        });
        setRuneIconById(iconById);
        setRuneTree(runeRes.data);

        const itemNameById = {}, itemDescById = {}, itemGoldById = {};
        Object.entries(itemRes.data.data).forEach(([id, item]) => {
          itemNameById[Number(id)] = item.name;
          itemDescById[Number(id)] = stripHtml(item.description) || item.plaintext || '';
          if (item.gold) itemGoldById[Number(id)] = { total: item.gold.total ?? 0, sell: item.gold.sell ?? 0 };
        });
        setItemMeta({ nameById: itemNameById, descById: itemDescById, goldById: itemGoldById });
      }).catch(() => {});
    return () => { cancelled = true; };
  }, []);

  if (!championId) return <Notice>잘못된 챔피언입니다.</Notice>;

  const total = detail?.games ?? 0;

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: '20px 20px 56px' }}>
      <Header
        champKey={champKey} champName={champName}
        detail={detail} queueType={queueType} onQueueChange={setQueueType}
      />

      {error && <Notice>통계를 불러오지 못했습니다.</Notice>}
      {!error && loading && <Notice>불러오는 중…</Notice>}

      {!error && !loading && detail && (
        detail.games === 0 ? (
          <Notice>아직 집계된 매치가 부족합니다. 소환사 검색이 쌓이면 자동으로 채워집니다.</Notice>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginTop: 14 }}>
            <RunesSection champName={champName} runes={detail.runes} totalGames={total}
              runeTree={runeTree} runeIconById={runeIconById} />
            <SpellsSection champName={champName} spells={detail.spells}
              spellMeta={spellMeta} totalGames={total} />
            <SkillSection champName={champName} ddragon={ddragon}
              orders={detail.skillOrders} totalGames={total} />
            <ItemSection champName={champName} startingItems={detail.startingItems}
              boots={detail.boots} coreItems={detail.coreItems}
              itemMeta={itemMeta} totalGames={total} />
          </div>
        )
      )}

      {/* 챔피언별 운영 팁 게시판(코멘트) — 매치 표본 유무와 무관하게 항상 노출 */}
      <div style={{ marginTop: 14 }}>
        <ChampionTips championId={championId} championName={champName} />
      </div>
    </div>
  );
}

/* ── 헤더(요약) ── */
function Header({ champKey, champName, detail, queueType, onQueueChange }) {
  const position = detail?.primaryPosition ? POSITION_LABEL[detail.primaryPosition] : null;
  return (
    <div style={{ background: C.card, border: `1px solid ${C.line}`, borderRadius: 8, overflow: 'hidden' }}>
      {/* 큐 선택 + 패치 */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '10px 16px', borderBottom: `1px solid ${C.line}`, background: C.head }}>
        <div style={{ display: 'inline-flex', background: C.box, border: `1px solid ${C.line}`,
          borderRadius: 8, padding: 3, gap: 2 }}>
          {QUEUE_FILTERS.map(q => {
            const active = q.key === queueType;
            return (
              <button key={q.label} onClick={() => onQueueChange(q.key)} style={{
                background: active ? C.accent : 'transparent', color: active ? '#fff' : C.sub,
                border: 'none', fontSize: 12.5, fontWeight: 700, padding: '6px 14px',
                borderRadius: 6, cursor: 'pointer', fontFamily: 'inherit',
              }}>{q.label}</button>
            );
          })}
        </div>
        <span style={{ color: C.muted, fontSize: 12, fontWeight: 600 }}>패치 {PATCH}</span>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 18, padding: '18px 20px' }}>
        {champKey && <img src={imgChampion(champKey)} alt={champName} width={72} height={72}
          style={{ borderRadius: 12, border: '2px solid #c89b3c' }} />}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ color: C.text, fontSize: 22, fontWeight: 800 }}>{champName}</div>
          <div style={{ color: C.sub, fontSize: 13, marginTop: 4, display: 'flex',
            alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
            {position && (
              <span style={{ color: C.text, fontSize: 12, fontWeight: 700, background: C.box,
                border: `1px solid ${C.line}`, borderRadius: 5, padding: '2px 8px' }}>{position}</span>
            )}
            <span>빌드 · 패치 {PATCH}</span>
            <span style={{ color: C.muted }}>표본 {(detail?.games ?? 0).toLocaleString()}게임</span>
          </div>
        </div>
        {detail && (
          <div style={{ display: 'flex' }}>
            <Stat label="승률" value={pct(detail.winRate, 1)} />
            <Stat label="픽률" value={pct(detail.pickRate, 1)} divider />
            <Stat label="밴율" value={pct(detail.banRate, 1)} divider />
          </div>
        )}
      </div>
    </div>
  );
}

function Stat({ label, value, divider }) {
  return (
    <div style={{ textAlign: 'center', padding: '0 18px',
      borderLeft: divider ? `1px solid ${C.line}` : 'none' }}>
      <div style={{ color: C.text, fontSize: 19, fontWeight: 800, lineHeight: 1.1 }}>{value}</div>
      <div style={{ color: C.muted, fontSize: 11, marginTop: 4 }}>{label}</div>
    </div>
  );
}

/* ── 섹션 래퍼(제목 띠 + 본문) ── */
function Section({ title, children, bodyPad = '14px 16px' }) {
  return (
    <div style={{ background: C.card, border: `1px solid ${C.line}`, borderRadius: 8, overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '11px 16px',
        background: C.head, borderBottom: `1px solid ${C.line}` }}>
        <span style={{ color: C.text, fontSize: 14, fontWeight: 800 }}>{title}</span>
        <span style={{ color: C.muted, fontSize: 14 }}>›</span>
      </div>
      <div style={{ padding: bodyPad }}>{children}</div>
    </div>
  );
}

/* ── 룬 세팅 ── 상위 1·2위 페이지를 가로 2개 + 선택 페이지 전체 트리 ── */
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
// 능력치 파편 설명 — DDragon 룬 데이터에 없어 직접 정의.
const STAT_SHARD_DESC = {
  5008: '공격력 +5.4 또는 주문력 +9 (둘 중 높은 효과 적용)',
  5005: '공격 속도 +10%',
  5007: '스킬 가속 +8',
  5002: '방어력 +6',
  5003: '마법 저항력 +8',
  5001: '레벨에 비례해 체력 +10~180 (1레벨 기준 증가)',
  5011: '체력 +65',
  5013: '강인함 및 둔화 저항 +10%',
  5010: '이동 속도 +2%',
};
const STAT_SHARD_ROWS = [
  [5008, 5005, 5007],
  [5008, 5010, 5001],
  [5011, 5013, 5001],
];

function RunesSection({ champName, runes, totalGames, runeTree, runeIconById }) {
  const [sel, setSel] = useState(0);
  if (!runes?.length) return <Section title={`${champName} 룬 세팅`}><Empty /></Section>;

  const top = runes.slice(0, 2);
  const idx = Math.min(sel, top.length - 1);
  const build = top[idx];
  const primary = runeTree.find(s => s.id === build.primaryStyleId);
  const secondary = runeTree.find(s => s.id === build.subStyleId);
  const primaryPicks = new Set([build.keystoneId, build.primaryRune1, build.primaryRune2, build.primaryRune3]);
  const secondaryPicks = new Set([build.subRune1, build.subRune2]);
  const shards = [build.statOffense, build.statFlex, build.statDefense];

  return (
    <Section title={`${champName} 룬 세팅`}>
      <div style={{ display: 'flex', gap: 10, marginBottom: 18 }}>
        {top.map((r, i) => {
          const active = i === idx;
          return (
            <button key={i} onClick={() => setSel(i)} style={{
              flex: 1, display: 'flex', alignItems: 'center', gap: 12, padding: '10px 14px',
              background: active ? C.boxActive : C.box,
              border: `1px solid ${active ? C.accent : C.line}`,
              borderRadius: 8, cursor: 'pointer', textAlign: 'left', fontFamily: 'inherit',
            }}>
              <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                <RuneIcon id={r.primaryStyleId} runeIconById={runeIconById} size={20} />
                <RuneIcon id={r.keystoneId} runeIconById={runeIconById} size={30} />
                <RuneIcon id={r.subStyleId} runeIconById={runeIconById} size={20} />
              </span>
              <FreqGames freq={totalGames > 0 ? r.games / totalGames : 0} games={r.games} />
              <WinPct value={r.winRate} style={{ marginLeft: 'auto' }} />
            </button>
          );
        })}
      </div>

      <div style={{ display: 'flex', alignItems: 'stretch' }}>
        <RuneTreeColumn style={primary} picks={primaryPicks} keystone flex={1.25} />
        <VLine />
        <RuneTreeColumn style={secondary} picks={secondaryPicks} flex={1} />
        <VLine />
        <ShardColumn shards={shards} flex={1} />
      </div>
    </Section>
  );
}

function RuneTreeColumn({ style, picks, keystone, flex }) {
  if (!style) return <div style={{ flex }}><Empty /></div>;
  const slots = keystone ? style.slots : style.slots.slice(1);
  return (
    <div style={{ flex, display: 'flex', flexDirection: 'column', minWidth: 0 }}>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 14, flex: 1 }}>
        {slots.map((slot, si) => {
          const big = keystone && si === 0;
          return (
            <div key={si} style={{ display: 'flex', gap: 12, justifyContent: 'center', alignItems: 'center',
              ...(big ? { paddingBottom: 14, borderBottom: `1px solid ${C.line}` } : {}) }}>
              {slot.runes.map(rune => {
                const on = picks.has(rune.id);
                const size = big ? 40 : 28;
                return (
                  <Tooltip key={rune.id} label={rune.name} desc={stripHtml(rune.shortDesc)}>
                    <span style={{ display: 'inline-block', cursor: 'help',
                      opacity: on ? 1 : 0.3, filter: on ? 'none' : 'grayscale(1)' }}>
                      <img src={imgRune(rune.icon)} alt={rune.name} width={size} height={size}
                        style={{ borderRadius: '50%', display: 'block', background: '#1c1c20',
                          border: on ? `2px solid ${C.accent}` : '2px solid transparent', boxSizing: 'border-box' }} />
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
                <Tooltip key={ci} label={STAT_SHARD_LABEL[id] || '능력치 파편'} desc={STAT_SHARD_DESC[id]}>
                  <span style={{ display: 'inline-block', cursor: 'help',
                    opacity: on ? 1 : 0.3, filter: on ? 'none' : 'grayscale(1)' }}>
                    {icon
                      ? <img src={imgRune(icon)} alt="" width={26} height={26}
                          style={{ borderRadius: '50%', display: 'block', background: '#1c1c20',
                            border: on ? `2px solid ${C.accent}` : '2px solid transparent', boxSizing: 'border-box' }} />
                      : <span style={{ width: 26, height: 26, display: 'block', borderRadius: '50%',
                          background: '#1c1c20', border: `1px solid ${C.line}` }} />}
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
    <div style={{ textAlign: 'center', color: C.sub, fontSize: 12, fontWeight: 700,
      marginTop: 14, paddingTop: 12, borderTop: `1px solid ${C.line}` }}>{children}</div>
  );
}

function RuneIcon({ id, runeIconById, size }) {
  const src = id != null ? imgRune(runeIconById[id]) : null;
  if (!src) return <span style={{ width: size, height: size, display: 'inline-block' }} />;
  return <img src={src} alt="" width={size} height={size} style={{ borderRadius: '50%' }} />;
}

/* ── 소환사 주문 ── 상위 1·2위 가로 2개 ── */
function SpellsSection({ champName, spells, spellMeta, totalGames }) {
  return (
    <Section title={`${champName} 소환사 주문`} bodyPad="6px 16px">
      {!spells?.length ? <Empty /> : (
        <div style={{ display: 'flex' }}>
          {spells.slice(0, 2).map((s, i) => (
            <div key={i} style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 12,
              padding: '12px 14px', borderLeft: i > 0 ? `1px solid ${C.line}` : 'none' }}>
              <span style={{ display: 'flex', gap: 4 }}>
                <SpellIcon id={s.spell1} spellMeta={spellMeta} />
                <SpellIcon id={s.spell2} spellMeta={spellMeta} />
              </span>
              <FreqGames freq={totalGames > 0 ? s.games / totalGames : 0} games={s.games} />
              <WinPct value={s.winRate} style={{ marginLeft: 'auto' }} />
            </div>
          ))}
        </div>
      )}
    </Section>
  );
}

function SpellIcon({ id, spellMeta }) {
  const file = spellMeta.fileById[id];
  if (!file) return <span style={{ width: 34, height: 34, background: C.box, borderRadius: 7,
    display: 'inline-block' }} />;
  return (
    <Tooltip label={spellMeta.nameById[id]} desc={spellMeta.descById[id]}>
      <img src={imgSpell(file)} alt={spellMeta.nameById[id] || ''} width={34} height={34}
        style={{ borderRadius: 7, cursor: 'help' }} />
    </Tooltip>
  );
}

/* ── 스킬 빌드 ── 선마 순서 아이콘 + 레벨업 순서 한 줄 ── */
function SkillSection({ champName, ddragon, orders, totalGames }) {
  const top = orders?.[0];
  return (
    <Section title={`${champName} 스킬 빌드`}>
      {!top ? <Empty /> : (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {top.order.split('>').map((letter, i) => (
                <React.Fragment key={i}>
                  {i > 0 && <Chevron />}
                  <SkillIcon ddragon={ddragon} letter={letter} />
                </React.Fragment>
              ))}
            </div>
            <FreqGames freq={totalGames > 0 ? top.games / totalGames : 0} games={top.games}
              style={{ width: 96, marginLeft: 'auto' }} />
            <WinPct value={top.winRate} style={{ width: 56, textAlign: 'right' }} />
          </div>

          {top.levelOrder && (
            <div style={{ display: 'flex', gap: 5, flexWrap: 'wrap', marginTop: 16,
              paddingTop: 14, borderTop: `1px solid ${C.line}` }}>
              {top.levelOrder.split('').map((letter, i) => <LevelBadge key={i} letter={letter} />)}
            </div>
          )}
        </>
      )}
    </Section>
  );
}

function SkillIcon({ ddragon, letter }) {
  const idx = { Q: 0, W: 1, E: 2, R: 3 }[letter];
  const spell = ddragon?.spells?.[idx];
  const src = spell ? imgChampionSpell(spell.image.full) : null;
  return (
    <Tooltip label={spell?.name} desc={stripHtml(spell?.description)}>
      <span style={{ position: 'relative', display: 'inline-block', cursor: 'help' }}>
        {src
          ? <img src={src} alt={spell?.name} width={44} height={44}
              style={{ borderRadius: 8, border: `1px solid ${C.line}`, display: 'block' }} />
          : <span style={{ width: 44, height: 44, background: C.box, borderRadius: 8, display: 'block' }} />}
        <span style={{ position: 'absolute', bottom: -4, right: -4, background: C.head,
          color: SKILL_COLOR[letter], fontSize: 11, fontWeight: 800, padding: '0 5px',
          borderRadius: 4, border: `1px solid ${C.line}` }}>{letter}</span>
      </span>
    </Tooltip>
  );
}

function LevelBadge({ letter }) {
  const isR = letter === 'R';
  return (
    <span style={{ width: 26, height: 26, borderRadius: 4, display: 'inline-flex',
      alignItems: 'center', justifyContent: 'center', fontSize: 12, fontWeight: 800,
      background: isR ? C.accent : C.box, color: isR ? '#fff' : (SKILL_COLOR[letter] || C.sub),
      border: `1px solid ${isR ? C.accent : C.line}` }}>{letter}</span>
  );
}

/* ── 아이템 빌드 ── 시작 아이템 | 신발 + 핵심 빌드 ── */
function ItemSection({ champName, startingItems, boots, coreItems, itemMeta, totalGames }) {
  return (
    <Section title={`${champName} 아이템 빌드`}>
      <div style={{ display: 'flex' }}>
        <div style={{ flex: 1, paddingRight: 16 }}>
          <SubLabel>시작 아이템</SubLabel>
          {!startingItems?.length ? <Empty /> : startingItems.map((b, i) => (
            <ItemStatRow key={i} first={i === 0} freq={totalGames > 0 ? b.games / totalGames : 0}
              games={b.games} winRate={b.winRate}>
              <ItemIcons ids={b.items} itemMeta={itemMeta} />
            </ItemStatRow>
          ))}
        </div>
        <div style={{ flex: 1, paddingLeft: 16, borderLeft: `1px solid ${C.line}` }}>
          <SubLabel>신발</SubLabel>
          {!boots?.length ? <Empty /> : boots.map((b, i) => (
            <ItemStatRow key={i} first={i === 0} freq={totalGames > 0 ? b.games / totalGames : 0}
              games={b.games} winRate={b.winRate}>
              <ItemIcons ids={b.items} itemMeta={itemMeta} />
            </ItemStatRow>
          ))}
        </div>
      </div>

      <div style={{ marginTop: 16, paddingTop: 14, borderTop: `1px solid ${C.line}` }}>
        <SubLabel>핵심 빌드</SubLabel>
        {!coreItems?.length ? <Empty /> : coreItems.map((b, i) => (
          <ItemStatRow key={i} first={i === 0} freq={totalGames > 0 ? b.games / totalGames : 0}
            games={b.games} winRate={b.winRate}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              {b.items.map((id, k) => (
                <React.Fragment key={k}>
                  {k > 0 && <Chevron />}
                  <span style={{ textAlign: 'center' }}>
                    <ItemImg id={id} itemMeta={itemMeta} size={34} />
                    <span style={{ display: 'block', color: C.muted, fontSize: 9.5, marginTop: 3 }}>{k + 1}코어</span>
                  </span>
                </React.Fragment>
              ))}
            </div>
          </ItemStatRow>
        ))}
      </div>
    </Section>
  );
}

function ItemIcons({ ids, itemMeta }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
      {ids.map((id, k) => <ItemImg key={k} id={id} itemMeta={itemMeta} size={32} />)}
    </div>
  );
}

function ItemImg({ id, itemMeta, size }) {
  return (
    <Tooltip label={itemMeta.nameById[id]} desc={itemMeta.descById[id]} gold={itemMeta.goldById[id]}>
      <img src={imgItem(id)} alt={itemMeta.nameById[id] || ''} width={size} height={size}
        style={{ borderRadius: 6, border: `1px solid ${C.line}`, display: 'block', cursor: 'help' }}
        onError={e => { e.target.style.visibility = 'hidden'; }} />
    </Tooltip>
  );
}

function ItemStatRow({ children, freq, games, winRate, first }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '9px 0',
      borderTop: first ? 'none' : `1px solid ${C.line}` }}>
      <div style={{ flex: 1, minWidth: 0 }}>{children}</div>
      <FreqGames freq={freq} games={games} style={{ width: 96 }} />
      <WinPct value={winRate} style={{ width: 56, textAlign: 'right' }} />
    </div>
  );
}

/* ── 검은 배경/흰 글씨 호버 툴팁 ── 전적 빌드탭과 동일하게 body 로 포털 렌더(섹션 overflow 에 안 잘림) */
function Tooltip({ children, label, desc, gold }) {
  const [pos, setPos] = useState(null); // 뷰포트 기준 {x: 대상 가로중앙, y: 대상 상단}
  const ref = useRef(null);
  if (!label && !desc) return children;
  const show = () => {
    const r = ref.current?.getBoundingClientRect();
    if (r) setPos({ x: r.left + r.width / 2, y: r.top });
  };
  const hasGold = gold && gold.total > 0;
  return (
    <span ref={ref} style={{ display: 'inline-flex', flexShrink: 0 }}
      onMouseEnter={show} onMouseLeave={() => setPos(null)}>
      {children}
      {pos && createPortal(
        <span style={{
          position: 'fixed', left: pos.x, top: pos.y - 8, transform: 'translate(-50%, -100%)',
          background: '#000', color: '#fff', fontSize: 11.5, lineHeight: '16px',
          padding: '8px 11px', borderRadius: 6, maxWidth: 300, whiteSpace: 'pre-wrap',
          pointerEvents: 'none', zIndex: 9999, boxShadow: '0 6px 20px rgba(0,0,0,0.6)',
        }}>
          {label && <span style={{ display: 'block', fontWeight: 800 }}>{label}</span>}
          {desc && <span style={{ display: 'block', marginTop: 4, fontWeight: 400, color: '#cfd6e4' }}>{desc}</span>}
          {hasGold && (
            <span style={{ display: 'block', marginTop: 5, fontWeight: 700, color: '#c89b3c' }}>
              가격 {gold.total.toLocaleString()}
              {gold.sell > 0 && <span style={{ fontWeight: 400, color: '#cfd6e4' }}> ({gold.sell.toLocaleString()})</span>}
            </span>
          )}
        </span>,
        document.body,
      )}
    </span>
  );
}

/* ── 공통 UI ── 통계 표시는 어디서나 같은 폭/정렬로 컬럼이 맞게 ── */
function FreqGames({ freq, games, style }) {
  return (
    <span style={{ display: 'inline-block', lineHeight: 1.25, ...style }}>
      <span style={{ display: 'block', color: C.text, fontSize: 15, fontWeight: 800 }}>{pct(freq, 2)}</span>
      <span style={{ display: 'block', color: C.muted, fontSize: 11 }}>{games.toLocaleString()} 게임</span>
    </span>
  );
}

function WinPct({ value, style }) {
  return <span style={{ display: 'inline-block', color: WIN, fontSize: 14, fontWeight: 800, ...style }}>{pct(value, 2)}</span>;
}

function SubLabel({ children }) {
  return <div style={{ color: C.sub, fontSize: 11.5, fontWeight: 700, marginBottom: 4 }}>{children}</div>;
}

function VLine() {
  return <span style={{ width: 1, background: C.line, margin: '0 6px', alignSelf: 'stretch' }} />;
}

function Chevron() {
  return <span style={{ color: C.muted, fontSize: 16 }}>›</span>;
}

function Empty({ hint = '데이터 없음' }) {
  return <div style={{ color: C.muted, fontSize: 12.5, padding: '14px 0' }}>{hint}</div>;
}

function Notice({ children }) {
  return <div style={{ color: C.sub, fontSize: 14, padding: '60px 0', textAlign: 'center' }}>{children}</div>;
}
