import React, { useState } from 'react';
import { imgChampion } from '../../constants/ddragon';

const HEADERS = [
  { key: 'champion',    label: '챔피언(S16)',   align: 'left'  },
  { key: 'games',       label: '게임 수',        align: 'center'},
  { key: 'winRate',     label: '승률',           align: 'center'},
  { key: 'kda',         label: 'KDA',            align: 'center'},
  { key: 'k',           label: 'K',              align: 'center'},
  { key: 'd',           label: 'D',              align: 'center'},
  { key: 'a',           label: 'A',              align: 'center'},
  { key: 'cs',          label: 'CS',             align: 'center'},
  { key: 'gold',        label: '골드',           align: 'center'},
  { key: 'wins',        label: '승',             align: 'center'},
  { key: 'losses',      label: '패',             align: 'center'},
];

const PAGE_SIZE = 7;

export default function ChampionStatTable({ stats = [], championKeyById = {} }) {
  const [showAll, setShowAll] = useState(false);
  const [hoverRow, setHoverRow] = useState(null);

  const displayed = showAll ? stats : stats.slice(0, PAGE_SIZE);

  function winRateColor(rate) {
    if (rate >= 60) return '#e74c3c';
    if (rate >= 55) return '#e67e22';
    return '#e8e0d0';
  }

  return (
    <div style={{
      background: '#111c27',
      border: '1px solid #2a3a4a',
      borderRadius: 10,
      overflow: 'hidden',
    }}>
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 680 }}>
          <thead>
            <tr style={{ background: '#0f1923', borderBottom: '1px solid #2a3a4a' }}>
              {HEADERS.map(h => (
                <th key={h.key} style={{
                  padding: '10px 12px',
                  fontSize: 12,
                  fontWeight: 600,
                  color: '#6b7a8d',
                  textAlign: h.align,
                  whiteSpace: 'nowrap',
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
                  textAlign: 'center', padding: '32px 0',
                  color: '#4a5568', fontSize: 14,
                }}>
                  챔피언 데이터가 없습니다.
                </td>
              </tr>
            ) : displayed.map((row, i) => {
              const champKey = championKeyById[row.championId] || row.championName?.replace(/[^a-zA-Z0-9]/g, '');
              const iconSrc = champKey ? imgChampion(champKey) : null;
              const isHover = hoverRow === i;
              const winRate = row.winRate ?? (row.wins + row.losses > 0
                ? Math.round((row.wins / (row.wins + row.losses)) * 100) : 0);

              return (
                <tr
                  key={row.championId || row.championName || i}
                  onMouseEnter={() => setHoverRow(i)}
                  onMouseLeave={() => setHoverRow(null)}
                  style={{
                    borderBottom: '1px solid #1a2535',
                    background: isHover ? '#1a2d3e' : 'transparent',
                    transition: 'background 0.15s',
                    cursor: 'default',
                  }}
                >
                  {/* 챔피언 아이콘 + 이름 */}
                  <td style={{ padding: '10px 12px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      {iconSrc ? (
                        <img src={iconSrc} alt={row.championName} style={{
                          width: 32, height: 32, borderRadius: 6,
                          border: '1px solid #2a3a4a', objectFit: 'cover',
                        }} onError={e => e.target.style.display = 'none'} />
                      ) : (
                        <div style={{ width: 32, height: 32, borderRadius: 6, background: '#2a3a4a' }} />
                      )}
                      <span style={{ color: '#e8e0d0', fontSize: 13, fontWeight: 600 }}>
                        {row.championName || '알 수 없음'}
                      </span>
                    </div>
                  </td>
                  <td style={{ textAlign: 'center', color: '#9ca3af', fontSize: 13, padding: '10px 12px' }}>{row.games ?? (row.wins + row.losses)}</td>
                  <td style={{ textAlign: 'center', padding: '10px 12px' }}>
                    <span style={{ color: winRateColor(winRate), fontWeight: 700, fontSize: 13 }}>
                      {winRate}%
                    </span>
                  </td>
                  <td style={{ textAlign: 'center', padding: '10px 12px' }}>
                    <span style={{ color: '#c89b3c', fontWeight: 700, fontSize: 13 }}>{row.kda ?? '-'}</span>
                  </td>
                  <td style={{ textAlign: 'center', color: '#e8e0d0', fontSize: 13, padding: '10px 12px' }}>{row.avgKills ?? '-'}</td>
                  <td style={{ textAlign: 'center', color: '#e8e0d0', fontSize: 13, padding: '10px 12px' }}>{row.avgDeaths ?? '-'}</td>
                  <td style={{ textAlign: 'center', color: '#e8e0d0', fontSize: 13, padding: '10px 12px' }}>{row.avgAssists ?? '-'}</td>
                  <td style={{ textAlign: 'center', color: '#9ca3af', fontSize: 13, padding: '10px 12px' }}>{row.avgCs ?? '-'}</td>
                  <td style={{ textAlign: 'center', color: '#9ca3af', fontSize: 13, padding: '10px 12px' }}>
                    {row.avgGold ? row.avgGold.toLocaleString() : '-'}
                  </td>
                  <td style={{ textAlign: 'center', color: '#3b82f6', fontWeight: 600, fontSize: 13, padding: '10px 12px' }}>{row.wins ?? '-'}</td>
                  <td style={{ textAlign: 'center', color: '#ef4444', fontWeight: 600, fontSize: 13, padding: '10px 12px' }}>{row.losses ?? '-'}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>

      {/* 더 보기 버튼 */}
      {stats.length > PAGE_SIZE && (
        <div style={{
          textAlign: 'center',
          padding: '14px 0',
          borderTop: '1px solid #1a2535',
        }}>
          <button
            onClick={() => setShowAll(v => !v)}
            style={{
              background: 'transparent',
              border: '1px solid #2a3a4a',
              color: '#6b7a8d',
              padding: '7px 32px',
              borderRadius: 6,
              fontSize: 13,
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
            onMouseEnter={e => { e.target.style.background = '#1a2535'; e.target.style.color = '#e8e0d0'; }}
            onMouseLeave={e => { e.target.style.background = 'transparent'; e.target.style.color = '#6b7a8d'; }}
          >
            {showAll ? '접기 ▲' : `더 보기 (${stats.length - PAGE_SIZE}개) ▼`}
          </button>
        </div>
      )}
    </div>
  );
}
