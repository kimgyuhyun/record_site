import React, { useEffect, useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import useChampionMeta from '../hooks/useChampionMeta';
import { getChampionDetail as getChampionStatsDetail } from '../api/champion';
import {
  getChampionDetail as getChampionDdragon,
  getSummonerSpellData,
  getRuneData,
} from '../api/ddragon';
import {
  DATA_CDN, imgChampion, imgItem, imgSpell, imgChampionSpell, imgRune, imgSkinLoading,
} from '../constants/ddragon';

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
      }).catch(() => {});
    return () => { cancelled = true; };
  }, []);

  const counters = useMemo(() => splitCounters(detail?.counters || []), [detail]);

  if (!championId) return <Notice>잘못된 챔피언입니다.</Notice>;

  return (
    <div style={{ maxWidth: 1100, margin: '0 auto', padding: '20px 20px 56px' }}>
      <Header
        champKey={champKey} champName={champName || detail?.championName}
        detail={detail}
      />

      <div style={{ display: 'flex', gap: 6, margin: '16px 0 18px' }}>
        {QUEUE_FILTERS.map(q => (
          <button key={q.label} onClick={() => setQueueType(q.key)} style={{
            background: q.key === queueType ? '#5383e8' : '#151d2e',
            border: `1px solid ${q.key === queueType ? '#5383e8' : '#2a3a4a'}`,
            color: q.key === queueType ? '#fff' : '#8899aa',
            fontSize: 12.5, fontWeight: 600, padding: '6px 14px', borderRadius: 6,
            cursor: 'pointer', fontFamily: 'inherit',
          }}>{q.label}</button>
        ))}
      </div>

      {error && <Notice>통계를 불러오지 못했습니다.</Notice>}
      {!error && loading && <Notice>불러오는 중…</Notice>}

      {!error && !loading && detail && (
        detail.games === 0 ? (
          <Notice>아직 집계된 매치가 부족합니다. 소환사 검색이 쌓이면 자동으로 채워집니다.</Notice>
        ) : (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, alignItems: 'start' }}>
            <Abilities ddragon={ddragon} />
            <Runes runes={detail.runes} runeIconById={runeIconById} />
            <SkillOrders orders={detail.skillOrders} />
            <Spells spells={detail.spells} spellFileById={spellFileById} />
            <ItemBuilds builds={detail.itemBuilds} />
            <Experts experts={detail.experts} />
            <CounterCard title="상대하기 어려운 챔피언" list={counters.hard}
              championKeyById={championKeyById} championNameById={championNameById} />
            <CounterCard title="상대하기 쉬운 챔피언" list={counters.easy}
              championKeyById={championKeyById} championNameById={championNameById} />
            <Skins ddragon={ddragon} champKey={champKey} />
          </div>
        )
      )}
    </div>
  );
}

/* ── 헤더(요약) ── */
function Header({ champKey, champName, detail }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 18, padding: '18px 20px',
      background: 'linear-gradient(135deg,#0f1923,#1a2535)', border: '1px solid #2a3a4a', borderRadius: 12,
    }}>
      {champKey && <img src={imgChampion(champKey)} alt={champName} width={72} height={72}
        style={{ borderRadius: 12, border: '2px solid #c89b3c' }} />}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ color: '#e8edf4', fontSize: 22, fontWeight: 800 }}>{champName || '알 수 없음'}</div>
        <div style={{ color: '#8899aa', fontSize: 13, marginTop: 2 }}>
          {detail?.primaryPosition ? POSITION_LABEL[detail.primaryPosition] : '—'} · 표본 {detail?.games ?? 0}게임
        </div>
      </div>
      {detail && (
        <div style={{ display: 'flex', gap: 22 }}>
          <Stat label="승률" value={pct(detail.winRate)} color={winColor(detail.winRate)} />
          <Stat label="픽률" value={pct(detail.pickRate, 1)} />
          <Stat label="밴율" value={pct(detail.banRate, 1)} />
        </div>
      )}
    </div>
  );
}

function Stat({ label, value, color = '#e8edf4' }) {
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{ color, fontSize: 18, fontWeight: 800 }}>{value}</div>
      <div style={{ color: '#6b7a8d', fontSize: 11, marginTop: 2 }}>{label}</div>
    </div>
  );
}

/* ── 스킬(정적) ── */
function Abilities({ ddragon }) {
  return (
    <Card title="스킬">
      {!ddragon ? <Empty /> : (
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <AbilityIcon label="P" name={ddragon.passive?.name}
            src={ddragon.passive ? `${DATA_CDN}/img/passive/${ddragon.passive.image.full}` : null} />
          {(ddragon.spells || []).map((s, i) => (
            <AbilityIcon key={s.id} label={SKILL_KEYS[i]} name={s.name}
              src={imgChampionSpell(s.image.full)} />
          ))}
        </div>
      )}
    </Card>
  );
}

function AbilityIcon({ label, name, src }) {
  return (
    <div style={{ width: 56, textAlign: 'center' }} title={name}>
      <div style={{ position: 'relative', width: 48, margin: '0 auto' }}>
        {src && <img src={src} alt={name} width={48} height={48}
          style={{ borderRadius: 8, border: '1px solid #2a3a4a' }} />}
        <span style={{
          position: 'absolute', bottom: -4, right: -4, background: '#0f1923',
          color: '#c89b3c', fontSize: 10, fontWeight: 700, padding: '0 4px',
          borderRadius: 4, border: '1px solid #c89b3c',
        }}>{label}</span>
      </div>
      <div style={{ color: '#9aa7b4', fontSize: 10.5, marginTop: 6, overflow: 'hidden',
        textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</div>
    </div>
  );
}

/* ── 룬 세팅 ── */
function Runes({ runes, runeIconById }) {
  return (
    <Card title="룬 세팅">
      {!runes?.length ? <Empty /> : runes.map((r, i) => (
        <BuildRow key={i} games={r.games} wins={r.wins} winRate={r.winRate}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
            <RuneIcon id={r.keystoneId} runeIconById={runeIconById} size={30} />
            {[r.primaryRune1, r.primaryRune2, r.primaryRune3].map((id, k) =>
              <RuneIcon key={`p${k}`} id={id} runeIconById={runeIconById} size={20} />)}
            <span style={{ color: '#3a4a5a', margin: '0 2px' }}>|</span>
            <RuneIcon id={r.subStyleId} runeIconById={runeIconById} size={18} />
            {[r.subRune1, r.subRune2].map((id, k) =>
              <RuneIcon key={`s${k}`} id={id} runeIconById={runeIconById} size={20} />)}
          </div>
        </BuildRow>
      ))}
    </Card>
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

/* ── 아이템 빌드 ── */
function ItemBuilds({ builds }) {
  return (
    <Card title="아이템 빌드 (코어)">
      {!builds?.length ? <Empty /> : builds.map((b, i) => (
        <BuildRow key={i} games={b.games} wins={b.wins} winRate={b.winRate}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            {b.items.map((id, k) => (
              <React.Fragment key={k}>
                {k > 0 && <span style={{ color: '#3a4a5a' }}>›</span>}
                <img src={imgItem(id)} alt="" width={32} height={32}
                  style={{ borderRadius: 6, border: '1px solid #2a3a4a' }}
                  onError={e => { e.target.style.visibility = 'hidden'; }} />
              </React.Fragment>
            ))}
          </div>
        </BuildRow>
      ))}
    </Card>
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

/* ── 카운터 ── */
function CounterCard({ title, list, championKeyById, championNameById }) {
  return (
    <Card title={title}>
      {!list.length ? <Empty hint="표본이 부족합니다" /> : list.map((c, i) => {
        const key = championKeyById[c.championId];
        const name = championNameById[c.championId] || c.championName;
        return (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '7px 0',
            borderBottom: '1px solid #1a2433' }}>
            {key && <img src={imgChampion(key)} alt={name} width={28} height={28}
              style={{ borderRadius: 6 }} />}
            <span style={{ flex: 1, color: '#dbe2ea', fontSize: 13, overflow: 'hidden',
              textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{name}</span>
            <span style={{ color: winColor(c.winRate), fontWeight: 700, fontSize: 13 }}>{pct(c.winRate)}</span>
            <span style={{ color: '#5a6b7e', fontSize: 11.5, width: 44, textAlign: 'right' }}>{c.games}판</span>
          </div>
        );
      })}
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

/* ── 스킨 갤러리(정적) ── */
function Skins({ ddragon, champKey }) {
  if (!ddragon?.skins?.length) return null;
  return (
    <div style={{ gridColumn: '1 / -1' }}>
      <Card title="스킨">
        <div style={{ display: 'flex', gap: 12, overflowX: 'auto', paddingBottom: 6 }}>
          {ddragon.skins.map(skin => (
            <div key={skin.id} style={{ flexShrink: 0, width: 108, textAlign: 'center' }}>
              <img src={imgSkinLoading(champKey, skin.num)} alt={skin.name}
                width={108} height={196} style={{ borderRadius: 8, objectFit: 'cover',
                  border: '1px solid #2a3a4a' }}
                onError={e => { e.target.style.visibility = 'hidden'; }} />
              <div style={{ color: '#9aa7b4', fontSize: 11, marginTop: 6, overflow: 'hidden',
                textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {skin.num === 0 ? '기본' : skin.name}
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
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

// 카운터를 winRate 오름차순 정렬 가정 → 앞쪽=어려운(낮은 승률), 뒤쪽=쉬운(높은 승률)
function splitCounters(counters) {
  return {
    hard: counters.slice(0, 5),
    easy: counters.slice(-5).reverse(),
  };
}
