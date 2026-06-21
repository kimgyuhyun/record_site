import React from 'react';

// 메인 탭 — 티어 박스 윗줄에 가운데 정렬로 배치 (챔피언 / 인게임 정보)
export function MainTabNav({ mainTab, setMainTab }) {
  const mainTabs = ['챔피언', '게임 관전하기 - 인게임 정보'];
  return (
    <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginBottom: 14 }}>
      {mainTabs.map(t => {
        const active = mainTab === t;
        return (
          <button
            key={t}
            onClick={() => setMainTab(t)}
            style={{
              padding: '8px 22px',
              background: active ? '#5383e8' : '#15202e',
              border: `1px solid ${active ? '#5383e8' : '#2a3a4a'}`,
              borderRadius: 8,
              color: active ? '#ffffff' : '#8899aa',
              fontWeight: active ? 700 : 600,
              fontSize: 14,
              cursor: 'pointer',
              transition: 'all 0.15s',
              whiteSpace: 'nowrap',
              fontFamily: 'inherit',
            }}
            onMouseEnter={e => { if (!active) e.currentTarget.style.color = '#c8d0e0'; }}
            onMouseLeave={e => { if (!active) e.currentTarget.style.color = '#8899aa'; }}
          >
            {t}
          </button>
        );
      })}
    </div>
  );
}

// 서브 탭 — 챔피언 통계 표 바로 위 (전체 / 솔로랭크 / 자유랭크)
export function SubTabNav({ subTab, setSubTab }) {
  const subTabs = ['전체', '솔로랭크', '자유랭크'];
  return (
    <div style={{
      display: 'flex',
      background: '#0f1923',
      paddingLeft: 12,
      gap: 4,
      border: '1px solid #2a3a4a',
      borderBottom: '1px solid #2a3a4a',
      borderRadius: '10px 10px 0 0',
    }}>
      {subTabs.map(t => {
        const active = subTab === t;
        return (
          <button
            key={t}
            onClick={() => setSubTab(t)}
            style={{
              padding: '10px 20px',
              background: 'transparent',
              border: 'none',
              borderBottom: active ? '2px solid #c89b3c' : '2px solid transparent',
              color: active ? '#c89b3c' : '#6b7a8d',
              fontWeight: active ? 700 : 500,
              fontSize: 14,
              cursor: 'pointer',
              transition: 'all 0.15s',
              whiteSpace: 'nowrap',
              fontFamily: 'inherit',
            }}
            onMouseEnter={e => { if (!active) e.currentTarget.style.color = '#a0aec0'; }}
            onMouseLeave={e => { if (!active) e.currentTarget.style.color = '#6b7a8d'; }}
          >
            {t}
          </button>
        );
      })}
    </div>
  );
}
