import React from 'react';

function Tab({ label, active, onClick }) {
  return (
    <button
      onClick={onClick}
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
      }}
      onMouseEnter={e => { if (!active) e.target.style.color = '#a0aec0'; }}
      onMouseLeave={e => { if (!active) e.target.style.color = '#6b7a8d'; }}
    >
      {label}
    </button>
  );
}

export default function TabNav({ mainTab, setMainTab, subTab, setSubTab }) {
  const mainTabs = ['챔피언', '게임 관전하기 - 인게임 정보'];
  const subTabs  = ['전체', '솔로랭크', '자유랭크'];

  return (
    <div style={{ marginBottom: 16 }}>
      {/* 메인 탭 */}
      <div style={{
        display: 'flex',
        borderBottom: '1px solid #2a3a4a',
        background: '#111c27',
        borderRadius: '10px 10px 0 0',
        paddingLeft: 8,
        gap: 4,
      }}>
        {mainTabs.map(t => (
          <Tab key={t} label={t} active={mainTab === t} onClick={() => setMainTab(t)} />
        ))}
      </div>

      {/* 서브 탭 */}
      <div style={{
        display: 'flex',
        background: '#0f1923',
        paddingLeft: 12,
        gap: 4,
        borderBottom: '1px solid #2a3a4a',
      }}>
        {subTabs.map(t => (
          <Tab key={t} label={t} active={subTab === t} onClick={() => setSubTab(t)} />
        ))}
      </div>
    </div>
  );
}
