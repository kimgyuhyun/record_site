import React from 'react';

// 상단 링크줄 구분자
const Divider = () => (
  <span style={{ color: '#2a3a4a', margin: '0 10px' }}>|</span>
);

// 아직 페이지가 없는 항목은 비활성(회색, 클릭 불가)으로 자리만 잡아둔다.
function FooterLink({ label, href }) {
  if (!href) {
    return <span style={{ color: '#4a6080', cursor: 'default' }}>{label}</span>;
  }
  return (
    <a href={href} style={{ color: '#8899aa', textDecoration: 'none' }}
      onMouseEnter={e => e.currentTarget.style.color = '#e2e8f0'}
      onMouseLeave={e => e.currentTarget.style.color = '#8899aa'}>
      {label}
    </a>
  );
}

export default function Footer() {
  return (
    <footer style={{
      background: '#0a0e1a',
      borderTop: '1px solid #1a2535',
      padding: '20px 20px 28px',
    }}>
      <div style={{
        maxWidth: 1200, margin: '0 auto',
        fontFamily: "'Pretendard', system-ui, sans-serif",
      }}>
        {/* 상단 링크줄 */}
        <div style={{
          display: 'flex', alignItems: 'center', flexWrap: 'wrap',
          fontSize: 13, color: '#8899aa', marginBottom: 12,
        }}>
          <span style={{ color: '#c8d0dc' }}>
            Copyright(c) KDA<span style={{ color: '#5383e8' }}>.gg</span>
          </span>
          <Divider />
          <FooterLink label="이용약관" />
          <Divider />
          <FooterLink label="개인정보처리방침" />
          <Divider />
          <span>문의/피드백:{' '}
            <FooterLink label="howcrazy9806@gmail.com" href="mailto:howcrazy9806@gmail.com" />
          </span>
        </div>

        {/* Riot 법적 고지 */}
        <p style={{
          margin: 0, fontSize: 11, lineHeight: 1.6, color: '#4a6080',
        }}>
          KDA.GG isn't endorsed by Riot Games and doesn't reflect the views or
          opinions of Riot Games or anyone officially involved in producing or
          managing League of Legends. League of Legends and Riot Games are
          trademarks or registered trademarks of Riot Games, Inc. League of
          Legends © Riot Games, Inc.
        </p>
      </div>
    </footer>
  );
}
