import React, { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { searchSummonerByName } from '../api/summoner';
import { imgProfileIcon } from '../constants/ddragon';

export default function SearchResultPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const query  = searchParams.get('name') || '';
  const region = searchParams.get('region') || 'kr';

  const [results,  setResults]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [notFound, setNotFound] = useState(false);

  useEffect(() => {
    if (!query) { navigate('/'); return; }
    setLoading(true);
    searchSummonerByName(query)
      .then(res => {
        const list = res.data || [];
        if (list.length === 1) {
          // 1건이면 바로 프로필로 이동
          const s = list[0];
          navigate(`/find/${region}/${encodeURIComponent(`${s.name}-${s.tagLine}`)}`, { replace: true });
          return;
        }
        setResults(list);
        setNotFound(list.length === 0);
      })
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [query, region]);

  const handleSelect = (s) => {
    navigate(`/find/${region}/${encodeURIComponent(`${s.name}-${s.tagLine}`)}`);
  };

  /* 로딩 */
  if (loading) return (
    <div style={{
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      minHeight: 'calc(100vh - 52px)', flexDirection: 'column', gap: 16,
    }}>
      <div style={{
        width: 40, height: 40, border: '3px solid #1e2d3d',
        borderTop: '3px solid #5383e8', borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
      }} />
      <div style={{ color: '#4a6080', fontSize: 14 }}>검색 중...</div>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );

  /* DB에 없음 */
  if (notFound) return (
    <div style={{
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      minHeight: 'calc(100vh - 52px)',
    }}>
      <div style={{
        background: '#111c27', border: '1px solid #1e2d3d',
        borderRadius: 8, padding: '40px 60px',
        textAlign: 'center', minWidth: 320,
      }}>
        {/* 검색어 */}
        <div style={{
          fontSize: 16, color: '#e2e8f0', fontWeight: 500, marginBottom: 24,
          padding: '8px 20px', border: '1px solid #2a3a4a',
          borderRadius: 4, display: 'inline-block', minWidth: 200,
        }}>
          {query}
        </div>
        <div style={{ color: '#6b7a8d', fontSize: 14, lineHeight: 1.9, marginBottom: 10 }}>
          해당 이름의 소환사를 찾을 수 없습니다.<br />
          정확한 태그명(#???)과 함께 검색해 주세요.
        </div>
        <div style={{ color: '#3a4a5a', fontSize: 13 }}>
          ( 예: {query}#KR1 )
        </div>
      </div>
    </div>
  );

  /* 결과 목록 */
  return (
    <div style={{
      maxWidth: 680, margin: '32px auto', padding: '0 16px',
      fontFamily: "'Pretendard', system-ui, sans-serif",
    }}>
      {/* 결과 수 */}
      <div style={{ color: '#6b7a8d', fontSize: 13, marginBottom: 12 }}>
        <span style={{ color: '#e2e8f0', fontWeight: 600 }}>{results.length}</span>개의 결과
        &nbsp;·&nbsp;태그(#)를 포함하여 검색하면 바로 이동합니다.
      </div>

      {/* 카드 목록 */}
      {results.map((s, i) => (
        <div
          key={`${s.name}-${s.tagLine}-${i}`}
          onClick={() => handleSelect(s)}
          style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '12px 16px', marginBottom: 6,
            background: '#111c27', border: '1px solid #1e2d3d',
            borderRadius: 8, cursor: 'pointer', transition: 'all 0.15s',
          }}
          onMouseEnter={e => {
            e.currentTarget.style.background = '#1a2d3e';
            e.currentTarget.style.borderColor = '#2a4a6a';
          }}
          onMouseLeave={e => {
            e.currentTarget.style.background = '#111c27';
            e.currentTarget.style.borderColor = '#1e2d3d';
          }}
        >
          <img
            src={imgProfileIcon(s.profileIconId)}
            alt=""
            style={{
              width: 44, height: 44, borderRadius: 8,
              border: '2px solid #2a3a4a', flexShrink: 0,
            }}
            onError={e => { e.target.style.visibility = 'hidden'; }}
          />
          <div style={{ flex: 1, minWidth: 0 }}>
            <span style={{ color: '#e2e8f0', fontSize: 15, fontWeight: 700 }}>
              {s.name}
            </span>
            <span style={{ color: '#4a6080', fontSize: 14, fontWeight: 500 }}>
              #{s.tagLine}
            </span>
          </div>
          <div style={{ color: '#3a4a5a', fontSize: 12, flexShrink: 0 }}>
            Lv.{s.level}
          </div>
          <div style={{ color: '#2a3a4a', fontSize: 16, flexShrink: 0 }}>›</div>
        </div>
      ))}
    </div>
  );
}
