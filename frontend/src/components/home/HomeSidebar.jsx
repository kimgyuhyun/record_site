import React from 'react';
import { useNavigate } from 'react-router-dom';
import useRecentSearches, { removeRecentSearch, clearRecentSearches } from '../../hooks/useRecentSearches';
import useFavorites, { toggleFavorite, isFavorite } from '../../hooks/useFavorites';

/*
 * 홈 우측 사이드바: 배너 + 즐겨찾기 + 최근 검색.
 *  - 즐겨찾기/최근검색은 localStorage 기반(서버 계정 없음).
 *  - 항목 클릭 시 저장해 둔 경로(to)로 이동, 별표로 즐겨찾기 토글, ×로 개별 삭제.
 */
export default function HomeSidebar() {
  const recent = useRecentSearches();
  const favorites = useFavorites();
  const navigate = useNavigate();

  return (
    <aside style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <Banner />

      <Panel title="즐겨찾기">
        {favorites.length === 0
          ? <EmptyText>별표한 소환사가 여기에 표시됩니다.</EmptyText>
          : favorites.map(item => (
              <Row key={item.to} item={item} favored
                onOpen={() => navigate(item.to)}
                onStar={() => toggleFavorite(item)}
                onRemove={() => toggleFavorite(item)} />
            ))}
      </Panel>

      <Panel
        title="최근 검색"
        action={recent.length > 0 && (
          <button onClick={clearRecentSearches} style={linkBtnStyle}>전체 삭제</button>
        )}>
        {recent.length === 0
          ? <EmptyText>최근 검색한 소환사가 없습니다.</EmptyText>
          : recent.map(item => (
              <Row key={item.to} item={item} favored={isFavorite(favorites, item.to)}
                onOpen={() => navigate(item.to)}
                onStar={() => toggleFavorite(item)}
                onRemove={() => removeRecentSearch(item.to)} />
            ))}
      </Panel>
    </aside>
  );
}

function Row({ item, favored, onOpen, onStar, onRemove }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px',
      borderTop: '1px solid #1a2433',
    }}>
      <button onClick={onStar} title={favored ? '즐겨찾기 해제' : '즐겨찾기'}
        style={{ ...iconBtnStyle, color: favored ? '#ffb454' : '#48566a' }}>
        {favored ? '★' : '☆'}
      </button>

      <button onClick={onOpen} style={{
        flex: 1, minWidth: 0, textAlign: 'left', background: 'transparent', border: 'none',
        cursor: 'pointer', padding: 0, fontFamily: 'inherit',
      }}>
        <div style={{ color: '#dbe2ea', fontSize: 13, fontWeight: 600,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.label}</div>
        {item.sub && <div style={{ color: '#5a6b7e', fontSize: 11 }}>{item.sub}</div>}
      </button>

      <button onClick={onRemove} title="삭제" style={{ ...iconBtnStyle, color: '#48566a', fontSize: 15 }}>
        ×
      </button>
    </div>
  );
}

function Banner() {
  return (
    <div style={{
      borderRadius: 8, overflow: 'hidden', border: '1px solid #1f2a3a',
      background: 'linear-gradient(135deg, #1b2740 0%, #10203a 60%, #0c1626 100%)',
      padding: '18px 16px',
    }}>
      <div style={{ color: '#5383e8', fontSize: 12, fontWeight: 700, letterSpacing: 0.5 }}>
        KDA.gg
      </div>
      <div style={{ color: '#e2e8f0', fontSize: 16, fontWeight: 800, marginTop: 6, lineHeight: 1.3 }}>
        소환사 전적을<br />빠르게 검색하세요
      </div>
      <div style={{ color: '#8899aa', fontSize: 12, marginTop: 6 }}>
        상단 검색창에 <span style={{ color: '#5383e8' }}>이름#태그</span> 입력
      </div>
    </div>
  );
}

function Panel({ title, action, children }) {
  return (
    <section style={{ background: '#151d2e', border: '1px solid #1f2a3a', borderRadius: 8, overflow: 'hidden' }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '11px 12px',
      }}>
        <h3 style={{ margin: 0, color: '#e2e8f0', fontSize: 13, fontWeight: 700 }}>{title}</h3>
        {action}
      </div>
      <div>{children}</div>
    </section>
  );
}

function EmptyText({ children }) {
  return (
    <div style={{ color: '#5a6b7e', fontSize: 12, padding: '10px 12px 14px', borderTop: '1px solid #1a2433' }}>
      {children}
    </div>
  );
}

const iconBtnStyle = {
  background: 'transparent', border: 'none', cursor: 'pointer',
  flexShrink: 0, padding: 2, lineHeight: 1, fontSize: 14, fontFamily: 'inherit',
};

const linkBtnStyle = {
  background: 'transparent', border: 'none', cursor: 'pointer',
  color: '#5a6b7e', fontSize: 11, padding: 0, fontFamily: 'inherit',
};
