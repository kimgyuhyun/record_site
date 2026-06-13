import React from 'react';

export default function HomePage() {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column',
      justifyContent: 'center', alignItems: 'center',
      minHeight: 'calc(100vh - 52px)',
      color: '#4a5568', gap: 14,
    }}>
      <div style={{ fontSize: 52, lineHeight: 1 }}>🎮</div>
      <div style={{ fontSize: 20, fontWeight: 700, color: '#6b7a8d' }}>
        소환사 이름을 검색하세요
      </div>
      <div style={{ fontSize: 13, color: '#3a4a5a' }}>
        상단 검색창에&nbsp;<span style={{ color: '#5383e8' }}>이름#태그</span>&nbsp;형식으로 입력하세요
      </div>
    </div>
  );
}
