import React, { useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import { imgChampion } from '../../constants/ddragon';

const HEADERS = [
  { key: 'champion', label: '챔피언(S16)', align: 'left'   },
  { key: 'games',    label: '게임 수',     align: 'center' },
  { key: 'winRate',  label: '승률',        align: 'center' },
  { key: 'kda',      label: 'KDA',         align: 'center' },
  { key: 'k',        label: 'K',           align: 'center' },
  { key: 'd',        label: 'D',           align: 'center' },
  { key: 'a',        label: 'A',           align: 'center' },
  { key: 'cs',       label: 'CS',          align: 'center' },
  { key: 'gold',     label: '골드',        align: 'center' },
  { key: 'wins',     label: '승',          align: 'center' },
  { key: 'losses',   label: '패',          align: 'center' },
];

const PAGE_SIZE = 7;

function winRateColor(rate) {
  if (rate >= 60) return '#e74c3c';
  if (rate >= 55) return '#e67e22';
  return '#e8e0d0';
}

// KDA 평점 색상 (OP.GG 유사): 5+ 빨강 / 4+ 골드 / 3+ 파랑 / 2+ 그린 / 그 외 회색
function kdaColor(kda) {
  if (kda >= 5) return '#e84057';
  if (kda >= 4) return '#c89b3c';
  if (kda >= 3) return '#3b82f6';
  if (kda >= 2) return '#2bb673';
  return '#9aa4b2';
}

const num1 = v => (v == null ? '-' : Number(v).toFixed(1));

export default function ChampionStatTable({
  stats = [], championKeyById = {}, championNameById = {}, search = '',
}) {
  const navigate = useNavigate();
  const [showAll, setShowAll] = useState(false);
  const [hoverRow, setHoverRow] = useState(null);

  // 챔피언명(한글/영문) 부분일치 검색
  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return stats;
    return stats.filter(row => {
      const ko = (championNameById[row.championId] || '').toLowerCase();
      const en = (row.championName || '').toLowerCase();
      return ko.includes(q) || en.includes(q);
    });
  }, [stats, search, championNameById]);

  const displayed = showAll ? filtered : filtered.slice(0, PAGE_SIZE);

  return (
    <div style={{
      background: '#111c27',
      border: '1px solid #2a3a4a',
      borderTop: 'none',
      borderRadius: '0 0 10px 10px',
      overflow: 'hidden',
    }}>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 720 }}>
          <thead>
            <tr style={{ background: '#0f1923', borderBottom: '1px solid #2a3a4a' }}>
              {HEADERS.map(h => (
                <th key={h.key} style={{
                  padding: '10px 12px', fontSize: 12, fontWeight: 600,
                  color: '#6b7a8d', textAlign: h.align, whiteSpace: 'nowrap',
                  letterSpacing: '0.3px',
                }}>
                  {h.label}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {displayed.length === 0 ? (
              <tr>
                <td colSpan={HEADERS.length} style={{
                  textAlign: 'center', padding: '32px 0', color: '#4a5568', fontSize: 14,
                }}>
                  {stats.length === 0 ? '챔피언 데이터가 없습니다.' : '검색 결과가 없습니다.'}
                </td>
              </tr>
            ) : displayed.map((row, i) => {
              const champKey = championKeyById[row.championId] || row.championName?.replace(/[^a-zA-Z0-9]/g, '');
              const iconSrc = champKey ? imgChampion(champKey) : null;
              const displayName = championNameById[row.championId] || row.championName || '알 수 없음';
              const isHover = hoverRow === i;
              const winRate = row.winRate ?? (row.wins + row.losses > 0
                ? Math.round((row.wins / (row.wins + row.losses)) * 100) : 0);

              return (
                <tr
                  key={row.championId || row.championName || i}
                  onMouseEnter={() => setHoverRow(i)}
                  onMouseLeave={() => setHoverRow(null)}
                  onClick={() => row.championId && navigate(`/champions/${row.championId}`)}
                  style={{
                    borderBottom: '1px solid #1a2535',
                    background: isHover ? '#1a2d3e' : 'transparent',
                    transition: 'background 0.15s',
                    cursor: row.championId ? 'pointer' : 'default',
                  }}
                >
                  {/* 챔피언 아이콘 + 이름 */}
                  <td style={{ padding: '9px 12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      {iconSrc ? (
                        <img src={iconSrc} alt={displayName} style={{
                          width: 32, height: 32, borderRadius: 6,
                          border: '1px solid #2a3a4a', objectFit: 'cover', flexShrink: 0,
                        }} onError={e => { e.target.style.visibility = 'hidden'; }} />
                      ) : (
                        <div style={{ width: 32, height: 32, borderRadius: 6, background: '#2a3a4a', flexShrink: 0 }} />
                      )}
                      <span style={{ color: '#e8e0d0', fontSize: 13, fontWeight: 600,
                        whiteSpace: 'nowrap' }}>
                        {displayName}
                      </span>
                    </div>
                  </td>
                  <td style={{ textAlign: 'center', color: '#9ca3af', fontSize: 13, padding: '9px 12px' }}>
                    {row.games ?? (row.wins + row.losses)}
                  </td>
                  <td style={{ textAlign: 'center', padding: '9px 12px' }}>
                    <span style={{ color: winRateColor(winRate), fontWeight: 700, fontSize: 13 }}>{winRate}%</span>
                  </td>
                  <td style={{ textAlign: 'center', padding: '9px 12px' }}>
                    <span style={{ color: kdaColor(row.kda ?? 0), fontWeight: 700, fontSize: 13 }}>
                      {row.kda != null ? Number(row.kda).toFixed(2) : '-'}
                    </span>
                  </td>
                  <td style={{ textAlign: 'center', color: '#e8e0d0', fontSize: 13, padding: '9px 12px' }}>{num1(row.avgKills)}</td>
                  <td style={{ textAlign: 'center', color: '#e8e0d0', fontSize: 13, padding: '9px 12px' }}>{num1(row.avgDeaths)}</td>
                  <td style={{ textAlign: 'center', color: '#e8e0d0', fontSize: 13, padding: '9px 12px' }}>{num1(row.avgAssists)}</td>
                  <td style={{ textAlign: 'center', color: '#9ca3af', fontSize: 13, padding: '9px 12px' }}>{num1(row.avgCs)}</td>
                  <td style={{ textAlign: 'center', color: '#9ca3af', fontSize: 13, padding: '9px 12px' }}>
                    {row.avgGold != null ? Math.round(row.avgGold).toLocaleString() : '-'}
                  </td>
                  <td style={{ textAlign: 'center', color: '#3b82f6', fontWeight: 600, fontSize: 13, padding: '9px 12px' }}>{row.wins ?? '-'}</td>
                  <td style={{ textAlign: 'center', color: '#ef4444', fontWeight: 600, fontSize: 13, padding: '9px 12px' }}>{row.losses ?? '-'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* 더 보기 / 접기 */}
      {filtered.length > PAGE_SIZE && (
        <div style={{ textAlign: 'center', padding: '12px 0', borderTop: '1px solid #1a2535' }}>
          <button
            onClick={() => setShowAll(v => !v)}
            style={{
              background: 'transparent', border: '1px solid #2a3a4a', color: '#6b7a8d',
              padding: '7px 32px', borderRadius: 6, fontSize: 13, cursor: 'pointer',
              transition: 'all 0.2s',
            }}
            onMouseEnter={e => { e.target.style.background = '#1a2535'; e.target.style.color = '#e8e0d0'; }}
            onMouseLeave={e => { e.target.style.background = 'transparent'; e.target.style.color = '#6b7a8d'; }}
          >
            {showAll ? '접기 ▲' : `더 보기 (${filtered.length - PAGE_SIZE}개) ▼`}
          </button>
        </div>
      )}
    </div>
  );
}
