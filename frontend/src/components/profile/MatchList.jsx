import React, { useState, useEffect } from 'react';
import { getMatchSummary } from '../../api/match';
import { DATA_CDN, imgChampion, imgItem, imgSpell } from '../../constants/ddragon';

function ItemStrip({ itemIds = [], spell1, spell2, spellMap, size = 24 }) {
  const slotStyle = {
    width: size, height: size,
    borderRadius: 4, border: '1px solid #2a3a4a',
    objectFit: 'cover', background: '#111',
  };
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 3, flexWrap: 'wrap' }}>
      {[spell1, spell2].map((sid, i) =>
        sid != null && sid !== 0 && spellMap[sid] ? (
          <img key={`spell-${i}`} src={imgSpell(spellMap[sid])}
            alt="" title={`spell ${sid}`} style={slotStyle} />
        ) : null
      )}
      <span style={{ width: 4 }} />
      {itemIds.map((id, i) =>
        id > 0 ? (
          <img key={`${id}-${i}`} src={imgItem(id)}
            alt="" title={`item ${id}`} style={slotStyle} />
        ) : (
          <div key={`empty-${i}`}
            style={{ ...slotStyle, background: '#1a2535', borderStyle: 'dashed' }} />
        )
      )}
    </div>
  );
}

function ChampionFace({ championId, championKeyById, championName, size = 40 }) {
  const key = championKeyById[championId] ||
    (championName ? championName.replace(/[^a-zA-Z0-9]/g, '') : null);
  if (!key) return (
    <div style={{ width: size, height: size, borderRadius: 4, background: '#2a3a4a', border: '1px solid #3a4a5a' }} />
  );
  return (
    <img
      src={imgChampion(key)}
      alt={championName || key}
      title={championName || key}
      style={{ width: size, height: size, borderRadius: 4, objectFit: 'cover', border: '1px solid #2a3a4a', background: '#1a1a2e' }}
      onError={e => { e.target.style.visibility = 'hidden'; }}
    />
  );
}

function MatchRow({ match, championKeyById, spellMap, onToggle, isExpanded, summaryLoading, summaryRows }) {
  const items = [match.myItem0, match.myItem1, match.myItem2,
                 match.myItem3, match.myItem4, match.myItem5, match.myItem6];
  const winColor   = match.myWin ? '#3b82f6' : '#ef4444';
  const winBg      = match.myWin
    ? 'linear-gradient(90deg, #1a2d5a33 0%, #111c2700 100%)'
    : 'linear-gradient(90deg, #3b0a0a33 0%, #111c2700 100%)';
  const borderLeft = `3px solid ${match.myWin ? '#3b82f6' : '#ef4444'}`;

  return (
    <div style={{ marginBottom: 6, borderRadius: 8, overflow: 'hidden', border: '1px solid #1e2d3d' }}>
      {/* 매치 요약 행 */}
      <div style={{
        display: 'flex', alignItems: 'center',
        padding: '10px 14px', background: winBg,
        borderLeft, gap: 12,
      }}>
        {/* 챔피언 */}
        <div style={{ position: 'relative', flexShrink: 0 }}>
          <ChampionFace championId={match.myChampionId} championKeyById={championKeyById}
            championName={match.myChampionName} size={48} />
          <span style={{
            position: 'absolute', right: -4, bottom: -4,
            background: '#0a0e1a', color: '#c89b3c',
            fontSize: 10, fontWeight: 700, padding: '0 4px',
            borderRadius: 4, border: '1px solid #2a3a4a',
          }}>{match.myChampionLevel}</span>
        </div>

        {/* 승패 + KDA */}
        <div style={{ minWidth: 80, flexShrink: 0 }}>
          <div style={{ color: winColor, fontWeight: 800, fontSize: 13, marginBottom: 2 }}>
            {match.myWin ? '승리' : '패배'}
          </div>
          <div style={{ color: '#e8e0d0', fontSize: 13 }}>
            <span style={{ fontWeight: 700 }}>{match.myKills}</span>
            <span style={{ color: '#4a5568' }}> / </span>
            <span style={{ color: '#ef4444', fontWeight: 700 }}>{match.myDeaths}</span>
            <span style={{ color: '#4a5568' }}> / </span>
            <span style={{ fontWeight: 700 }}>{match.myAssists}</span>
          </div>
        </div>

        {/* 아이템 */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <ItemStrip itemIds={items} spell1={match.mySpell1} spell2={match.mySpell2}
            spellMap={spellMap} size={24} />
        </div>

        {/* 참가자 미니 아이콘 */}
        {match.participants?.length > 0 && (
          <div style={{ display: 'flex', gap: 2, flexShrink: 0 }}>
            {match.participants.map(p => (
              <ChampionFace key={p.puuid + p.participantId}
                championId={p.championId} championKeyById={championKeyById}
                championName={p.championName} size={24} />
            ))}
          </div>
        )}

        {/* 상세 버튼 */}
        <button
          onClick={() => onToggle(match.matchId)}
          style={{
            background: 'transparent', border: '1px solid #2a3a4a',
            color: '#6b7a8d', borderRadius: 6, padding: '5px 12px',
            fontSize: 12, cursor: 'pointer', flexShrink: 0,
            transition: 'all 0.15s',
          }}
          onMouseEnter={e => { e.currentTarget.style.borderColor = '#c89b3c'; e.currentTarget.style.color = '#c89b3c'; }}
          onMouseLeave={e => { e.currentTarget.style.borderColor = '#2a3a4a'; e.currentTarget.style.color = '#6b7a8d'; }}
        >
          {isExpanded ? '접기 ▲' : '상세 ▼'}
        </button>
      </div>

      {/* 상세 펼침 */}
      {isExpanded && (
        <div style={{ background: '#0d1520', borderTop: '1px solid #1e2d3d', padding: '12px 14px' }}>
          {summaryLoading ? (
            <div style={{ color: '#4a5568', fontSize: 13, padding: '8px 0' }}>상세 조회 중...</div>
          ) : (
            <DetailTable rows={summaryRows} championKeyById={championKeyById} spellMap={spellMap} />
          )}
        </div>
      )}
    </div>
  );
}

function DetailTable({ rows, championKeyById, spellMap }) {
  if (!rows.length) return (
    <div style={{ color: '#4a5568', fontSize: 13 }}>데이터가 없습니다.</div>
  );
  return (
    <div style={{ overflowX: 'auto' }}>
      <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 520 }}>
        <thead>
          <tr style={{ borderBottom: '1px solid #1e2d3d' }}>
            {['플레이어', '빌드', 'K/D/A', '승패', '챔딜'].map(h => (
              <th key={h} style={{
                padding: '6px 10px', fontSize: 11, fontWeight: 600,
                color: '#4a5568', textAlign: h === '챔딜' ? 'right' : 'left',
              }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => {
            const rowItems = [row.item0, row.item1, row.item2, row.item3, row.item4, row.item5, row.item6];
            return (
              <tr key={`${row.puuid}-${row.championId}`}
                style={{ borderBottom: '1px solid #111c27', background: row.win ? '#0d1f3033' : '#1f0d0d22' }}>
                <td style={{ padding: '8px 10px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <ChampionFace championId={row.championId} championKeyById={championKeyById}
                      championName={row.championName} size={36} />
                    <div>
                      <div style={{ color: '#e8e0d0', fontSize: 12, fontWeight: 600 }}>
                        {row.gameName}<span style={{ color: '#4a5568' }}>#{row.tagLine}</span>
                      </div>
                      <div style={{ color: '#4a5568', fontSize: 11 }}>
                        {row.championName} · Lv.{row.championLevel}
                      </div>
                    </div>
                  </div>
                </td>
                <td style={{ padding: '8px 10px' }}>
                  <ItemStrip itemIds={rowItems} spell1={row.spell1} spell2={row.spell2}
                    spellMap={spellMap} size={20} />
                </td>
                <td style={{ padding: '8px 10px', color: '#e8e0d0', fontSize: 12 }}>
                  {row.kills}/{row.deaths}/{row.assists}
                </td>
                <td style={{ padding: '8px 10px' }}>
                  <span style={{ color: row.win ? '#3b82f6' : '#ef4444', fontWeight: 700, fontSize: 12 }}>
                    {row.win ? 'WIN' : 'LOSE'}
                  </span>
                </td>
                <td style={{ padding: '8px 10px', textAlign: 'right', color: '#9ca3af', fontSize: 12 }}>
                  {row.totalDamageDealtToChampions?.toLocaleString?.() ?? '-'}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export default function MatchList({ matches = [] }) {
  const [expandedMap, setExpandedMap]           = useState({});
  const [summaryMap, setSummaryMap]             = useState({});
  const [summaryLoadingMap, setSummaryLoadingMap] = useState({});
  const [spellMap, setSpellMap]                 = useState({});
  const [championKeyById, setChampionKeyById]   = useState({});

  useEffect(() => {
    let cancelled = false;
    async function loadDdragon() {
      try {
        const [sumRes, champRes] = await Promise.all([
          fetch(`${DATA_CDN}/data/en_US/summoner.json`),
          fetch(`${DATA_CDN}/data/en_US/champion.json`),
        ]);
        const sumJson = await sumRes.json();
        const champJson = await champRes.json();
        const spells = {};
        Object.values(sumJson.data).forEach(s => {
          const sid = Number(s.key);
          if (!Number.isNaN(sid)) spells[sid] = s.image.full;
        });
        const champs = {};
        Object.values(champJson.data).forEach(c => { champs[Number(c.key)] = c.id; });
        if (!cancelled) { setSpellMap(spells); setChampionKeyById(champs); }
      } catch (e) { console.warn('DDragon load failed', e); }
    }
    loadDdragon();
    return () => { cancelled = true; };
  }, []);

  const toggleSummary = async (matchId) => {
    if (expandedMap[matchId]) {
      setExpandedMap(prev => ({ ...prev, [matchId]: false }));
      return;
    }
    setExpandedMap(prev => ({ ...prev, [matchId]: true }));
    if (summaryMap[matchId]) return;
    setSummaryLoadingMap(prev => ({ ...prev, [matchId]: true }));
    try {
      const res = await getMatchSummary(matchId);
      setSummaryMap(prev => ({ ...prev, [matchId]: res.data || [] }));
    } catch (e) {
      console.error(e);
      setExpandedMap(prev => ({ ...prev, [matchId]: false }));
    } finally {
      setSummaryLoadingMap(prev => ({ ...prev, [matchId]: false }));
    }
  };

  if (!matches.length) return (
    <div style={{
      background: '#111c27', border: '1px solid #1e2d3d',
      borderRadius: 10, padding: '48px 0',
      textAlign: 'center', color: '#4a5568', fontSize: 14,
    }}>
      매치 데이터가 없습니다.
    </div>
  );

  return (
    <div>
      {matches.map(match => (
        <MatchRow
          key={match.matchId}
          match={match}
          championKeyById={championKeyById}
          spellMap={spellMap}
          onToggle={toggleSummary}
          isExpanded={!!expandedMap[match.matchId]}
          summaryLoading={!!summaryLoadingMap[match.matchId]}
          summaryRows={summaryMap[match.matchId] || []}
        />
      ))}
    </div>
  );
}
