import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

const REGIONS = ['KR', 'NA', 'EUW', 'EUNE', 'JP', 'BR', 'LAN', 'LAS', 'OCE', 'TR', 'RU'];

// FOW.LOL 원본과 유사한 SVG 돋보기
function SearchIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
      xmlns="http://www.w3.org/2000/svg" style={{ display: 'block' }}>
      <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2.2" />
      <line x1="16.5" y1="16.5" x2="22" y2="22"
        stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" />
    </svg>
  );
}

export default function Header({ region, setRegion }) {
  const navigate  = useNavigate();
  const [query, setQuery]     = useState('');  // "Hide on bush#KR1" 형식
  const [focused, setFocused] = useState(false);
  const inputRef = useRef(null);

  // "Name#Tag" → { name, tagLine } 파싱
  const parseQuery = (raw) => {
    const idx = raw.lastIndexOf('#');
    if (idx > 0) {
      return { name: raw.slice(0, idx).trim(), tagLine: raw.slice(idx + 1).trim() };
    }
    return { name: raw.trim(), tagLine: '' };
  };

  const handleSearch = () => {
    const { name, tagLine } = parseQuery(query);
    if (!name) return;
    // tagLine 없으면 region을 기본 태그로 사용 (ex: KR → KR1)
    const tag = tagLine || region;
    const slug = `${name}-${tag}`;
    navigate(`/find/${region.toLowerCase()}/${encodeURIComponent(slug)}`);
    inputRef.current?.blur();
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') handleSearch();
  };

  return (
    <header style={{
      position: 'fixed', top: 0, left: 0, right: 0, zIndex: 1000,
      background: '#0a0e1a',
      borderBottom: '1px solid #1a2535',
      boxShadow: '0 1px 12px rgba(0,0,0,0.6)',
    }}>
      {/* 메인 헤더 바 */}
      <div style={{
        maxWidth: 1200, margin: '0 auto', padding: '0 20px',
        height: 52, display: 'flex', alignItems: 'center', gap: 12,
      }}>
        {/* 로고 */}
        <a href="/" onClick={e => { e.preventDefault(); navigate('/'); }}
          style={{ textDecoration: 'none', flexShrink: 0 }}>
          <span style={{
            fontSize: 22, fontWeight: 900, letterSpacing: '-0.5px',
            color: '#ffffff',
            fontFamily: "'Arial Black', 'Pretendard', system-ui, sans-serif",
          }}>
            FOW<span style={{ color: '#5383e8' }}>.LOL</span>
          </span>
        </a>

        {/* 리전 드롭다운 */}
        <div style={{ position: 'relative', flexShrink: 0 }}>
          <select
            value={region}
            onChange={e => setRegion(e.target.value)}
            style={{
              background: '#151d2e', border: '1px solid #2a3a4a',
              borderRadius: 6, color: '#c8d0dc',
              fontSize: 13, fontWeight: 700,
              padding: '5px 26px 5px 10px',
              cursor: 'pointer', outline: 'none',
              appearance: 'none', WebkitAppearance: 'none',
              height: 34,
            }}
          >
            {REGIONS.map(r => (
              <option key={r} value={r} style={{ background: '#151d2e' }}>{r}</option>
            ))}
          </select>
          <span style={{
            position: 'absolute', right: 7, top: '50%', transform: 'translateY(-50%)',
            color: '#4a6080', fontSize: 9, pointerEvents: 'none',
          }}>▼</span>
        </div>

        {/* 검색창 — 단일 인풋 "플레이어 검색 (플레이어#KR1)" */}
        <div style={{
          flex: 1, maxWidth: 480, display: 'flex', alignItems: 'center',
          background: '#151d2e',
          border: `1px solid ${focused ? '#5383e8' : '#2a3a4a'}`,
          borderRadius: 6,
          transition: 'border-color 0.15s',
          height: 36, overflow: 'hidden',
        }}>
          <input
            ref={inputRef}
            type="text"
            placeholder="플레이어 검색 (플레이어#KR1)"
            value={query}
            onChange={e => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            style={{
              flex: 1, background: 'transparent', border: 'none', outline: 'none',
              color: '#e2e8f0', fontSize: 13, padding: '0 12px',
              fontFamily: 'inherit',
            }}
          />
          <button
            onClick={handleSearch}
            style={{
              background: '#5383e8', border: 'none',
              width: 44, height: 36, flexShrink: 0,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              cursor: 'pointer', color: '#ffffff',
              transition: 'background 0.15s',
              borderRadius: 0,
            }}
            onMouseEnter={e => e.currentTarget.style.background = '#4070d4'}
            onMouseLeave={e => e.currentTarget.style.background = '#5383e8'}
          >
            <SearchIcon />
          </button>
        </div>

        {/* 네비 */}
        <nav style={{ display: 'flex', gap: 2, marginLeft: 'auto', flexShrink: 0 }}>
          {['홈', '챔피언 분석', '랭킹', '장인 랭킹', '멀티서치'].map(item => (
            <button key={item} style={{
              background: 'transparent', border: 'none',
              color: '#8899aa', fontSize: 13, padding: '6px 10px',
              cursor: 'pointer', borderRadius: 4, fontFamily: 'inherit',
              whiteSpace: 'nowrap', transition: 'color 0.15s',
            }}
            onMouseEnter={e => e.currentTarget.style.color = '#e2e8f0'}
            onMouseLeave={e => e.currentTarget.style.color = '#8899aa'}
            >
              {item}
            </button>
          ))}
        </nav>
      </div>
    </header>
  );
}
