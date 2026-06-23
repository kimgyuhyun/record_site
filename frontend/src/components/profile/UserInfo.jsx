import React from 'react';
import { useParams } from 'react-router-dom';
import { imgProfileIcon } from '../../constants/ddragon';
import useFavorites, { toggleFavorite, isFavorite } from '../../hooks/useFavorites';

// 시즌 배지 색상 매핑
const TIER_BADGE = {
  CHALLENGER:  { bg: '#f0e4a0', color: '#7a5c00', border: '#c89b3c' },
  GRANDMASTER: { bg: '#f8d0d0', color: '#8b0000', border: '#c0392b' },
  MASTER:      { bg: '#e8d4f0', color: '#6b21a8', border: '#9b59b6' },
  DIAMOND:     { bg: '#d0e8f8', color: '#1a5276', border: '#2e86c1' },
  EMERALD:     { bg: '#d0f0e0', color: '#1a6b3c', border: '#27ae60' },
  PLATINUM:    { bg: '#d0f0f0', color: '#0e6655', border: '#1abc9c' },
  GOLD:        { bg: '#fdebd0', color: '#784212', border: '#e67e22' },
  SILVER:      { bg: '#eaecee', color: '#424949', border: '#7f8c8d' },
  BRONZE:      { bg: '#e8d5c4', color: '#6e2f1a', border: '#a04000' },
  P:           { bg: '#eaecee', color: '#424949', border: '#7f8c8d' },
};

function getTierStyle(tierStr) {
  const upper = tierStr?.toUpperCase() || '';
  for (const key of Object.keys(TIER_BADGE)) {
    if (upper.includes(key)) return TIER_BADGE[key];
  }
  return { bg: '#eee', color: '#333', border: '#aaa' };
}

export default function UserInfo({ summoner, onRefresh, refreshing, cooldown = 0 }) {
  const { region } = useParams(); // /find/:region/:slug — 즐겨찾기 링크를 같은 리전으로 만든다.
  const favorites = useFavorites();

  if (!summoner) return null;

  const isCooldown  = cooldown > 0;
  const isDisabled  = refreshing || isCooldown;

  // 즐겨찾기 항목(localStorage). to 를 고유 키로 쓰고, 사이드바/최근검색과 같은 구조를 따른다.
  const regionLower = (region || 'kr').toLowerCase();
  const favoriteSlug = summoner.tagLine ? `${summoner.name}-${summoner.tagLine}` : summoner.name;
  const favoriteEntry = {
    to: `/find/${regionLower}/${encodeURIComponent(favoriteSlug)}`,
    label: `${summoner.name} #${summoner.tagLine}`,
    sub: summoner.soloTier ? `${summoner.soloTier} ${summoner.soloRank ?? ''}`.trim() : undefined,
  };
  const favored = isFavorite(favorites, favoriteEntry.to);

  const mins = Math.floor(cooldown / 60);
  const secs = String(cooldown % 60).padStart(2, '0');
  const cooldownText = mins > 0 ? `${mins}분 ${secs}초` : `${cooldown}초`;

  const profileIconSrc = summoner.profileIconId != null
    ? imgProfileIcon(summoner.profileIconId)
    : null;

  // 시즌 배지 파싱 (ex: "S15: CHALLENGER" → { season: 'S15', tier: 'CHALLENGER' })
  const seasonBadges = summoner.previousSeasonRanks || [];

  return (
    <div style={{
      background: 'linear-gradient(135deg, #0f1923 0%, #1a2535 100%)',
      borderRadius: 12,
      padding: '24px 28px',
      marginBottom: 24,
      border: '1px solid #2a3a4a',
      boxShadow: '0 4px 20px rgba(0,0,0,0.3)',
    }}>
      {/* 상단: 프로필 + 이름 + 갱신 */}
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 20, flexWrap: 'wrap' }}>
        {/* 프로필 아이콘 */}
        <div style={{ position: 'relative', flexShrink: 0 }}>
          {profileIconSrc ? (
            <img
              src={profileIconSrc}
              alt="profile"
              style={{
                width: 80, height: 80,
                borderRadius: 10,
                border: '2px solid #c89b3c',
                objectFit: 'cover',
                display: 'block',
              }}
            />
          ) : (
            <div style={{
              width: 80, height: 80, borderRadius: 10,
              background: '#2a3a4a', border: '2px solid #c89b3c',
            }} />
          )}
          <span style={{
            position: 'absolute', bottom: -8, left: '50%', transform: 'translateX(-50%)',
            background: '#0f1923', border: '1px solid #c89b3c',
            color: '#c89b3c', fontSize: 11, fontWeight: 700,
            padding: '1px 8px', borderRadius: 10, whiteSpace: 'nowrap',
          }}>
            Lv.{summoner.level}
          </span>
        </div>

        {/* 이름 + 버튼 */}
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <h2 style={{ margin: 0, color: '#e8e0d0', fontSize: 24, fontWeight: 800, letterSpacing: '-0.5px' }}>
              {summoner.name}
              <span style={{ color: '#6b7a8d', fontWeight: 500, fontSize: 18 }}>
                #{summoner.tagLine}
              </span>
            </h2>
            <button
              onClick={onRefresh}
              disabled={isDisabled}
              style={{
                padding: '6px 16px',
                background: isDisabled
                  ? '#2a3a4a'
                  : '#1a3a6b',
                color: isDisabled ? '#6b7a8d' : '#e2e8f0',
                border: 'none',
                borderRadius: 6,
                fontWeight: 700,
                fontSize: 13,
                cursor: isDisabled ? 'not-allowed' : 'pointer',
                transition: 'all 0.2s',
                flexShrink: 0,
              }}
              onMouseEnter={e => { if (!isDisabled) e.target.style.transform = 'translateY(-1px)'; }}
              onMouseLeave={e => { e.target.style.transform = 'translateY(0)'; }}
            >
              {refreshing ? '갱신 중...' : '전적 갱신'}
            </button>

            {/* 즐겨찾기 토글 — 로그인 없이 localStorage 에 저장(사이드바 즐겨찾기와 동일 저장소) */}
            <button
              onClick={() => toggleFavorite(favoriteEntry)}
              title={favored ? '즐겨찾기 해제' : '즐겨찾기 추가'}
              aria-pressed={favored}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: 6,
                padding: '6px 14px',
                background: favored ? '#3a2e10' : '#1f2a38',
                color: favored ? '#ffb454' : '#9aa7b4',
                border: `1px solid ${favored ? '#c89b3c' : '#2a3a4a'}`,
                borderRadius: 6,
                fontWeight: 700,
                fontSize: 13,
                cursor: 'pointer',
                transition: 'all 0.2s',
                flexShrink: 0,
              }}
              onMouseEnter={e => { e.currentTarget.style.transform = 'translateY(-1px)'; }}
              onMouseLeave={e => { e.currentTarget.style.transform = 'translateY(0)'; }}
            >
              <span style={{ fontSize: 14, lineHeight: 1 }}>{favored ? '★' : '☆'}</span>
              {favored ? '즐겨찾기됨' : '즐겨찾기'}
            </button>
          </div>

          {/* 쿨다운 안내 메시지 */}
          {isCooldown && !refreshing && (
            <div style={{ color: '#4a5568', fontSize: 12, marginTop: 6 }}>
              ⏱ {cooldownText} 후 재갱신 가능합니다.
            </div>
          )}

          {/* 최근 업데이트 */}
          {summoner.lastUpdated && (
            <div style={{ color: '#6b7a8d', fontSize: 12, marginTop: 6 }}>
              최근 업데이트: {summoner.lastUpdated}
            </div>
          )}

          {/* 시즌 배지 */}
          {seasonBadges.length > 0 && (
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 12 }}>
              {seasonBadges.map((badge, i) => {
                const style = getTierStyle(badge.tier || badge);
                return (
                  <span key={i} style={{
                    padding: '2px 10px',
                    borderRadius: 4,
                    fontSize: 11,
                    fontWeight: 700,
                    background: style.bg,
                    color: style.color,
                    border: `1px solid ${style.border}`,
                    letterSpacing: '0.3px',
                  }}>
                    {typeof badge === 'string' ? badge : `${badge.season}: ${badge.tier}`}
                  </span>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
