import React from 'react';

// 티어별 색상/아이콘 색
const TIER_COLOR = {
  CHALLENGER:  '#f0c040',
  GRANDMASTER: '#e74c3c',
  MASTER:      '#9b59b6',
  DIAMOND:     '#3498db',
  EMERALD:     '#27ae60',
  PLATINUM:    '#1abc9c',
  GOLD:        '#e67e22',
  SILVER:      '#7f8c8d',
  BRONZE:      '#a04000',
  IRON:        '#6b6b6b',
  UNRANKED:    '#4a5568',
};

function getTierColor(tier) {
  return TIER_COLOR[tier?.toUpperCase()] || TIER_COLOR.UNRANKED;
}

// 티어 아이콘 (텍스트 기반 뱃지)
function TierEmblem({ tier, size = 64 }) {
  const color = getTierColor(tier);
  const label = tier?.charAt(0) || '?';
  return (
    <div style={{
      width: size, height: size,
      borderRadius: '50%',
      background: `radial-gradient(circle at 35% 35%, ${color}33, ${color}11)`,
      border: `2px solid ${color}88`,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      flexShrink: 0,
    }}>
      <span style={{ color, fontSize: size * 0.38, fontWeight: 900, letterSpacing: '-1px' }}>
        {label}
      </span>
    </div>
  );
}

export default function RankTierBox({ title, rankData }) {
  // rankData가 없으면 언랭크 표시
  const tier     = rankData?.tier     || 'UNRANKED';
  const rank     = rankData?.rank     || '';
  const lp       = rankData?.lp       ?? '--';
  const wins     = rankData?.wins     ?? 0;
  const losses   = rankData?.losses   ?? 0;
  const total    = wins + losses;
  const winRate  = total > 0 ? Math.round((wins / total) * 100) : null;
  const rankingText = rankData?.ranking ? `랭킹 ${rankData.ranking}위` : null;

  const tierColor = getTierColor(tier);
  const isUnranked = tier === 'UNRANKED';

  return (
    <div style={{
      flex: 1, minWidth: 260,
      background: 'linear-gradient(135deg, #111c27 0%, #1a2535 100%)',
      border: `1px solid ${isUnranked ? '#2a3a4a' : tierColor + '44'}`,
      borderRadius: 10,
      padding: '20px 22px',
      boxShadow: isUnranked ? 'none' : `0 0 20px ${tierColor}11`,
    }}>
      {/* 타이틀 */}
      <div style={{ color: '#6b7a8d', fontSize: 12, fontWeight: 600, letterSpacing: '0.5px', marginBottom: 14, textTransform: 'uppercase' }}>
        {title}
      </div>

      {isUnranked ? (
        <div style={{ color: '#4a5568', fontSize: 15, fontWeight: 600, padding: '10px 0' }}>
          배치 안됨
        </div>
      ) : (
        <>
          {/* 티어 + 랭킹 */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 14 }}>
            <TierEmblem tier={tier} size={60} />
            <div>
              {rankingText && (
                <div style={{ color: '#c89b3c', fontSize: 12, fontWeight: 600, marginBottom: 2 }}>
                  {rankingText}
                </div>
              )}
              <div style={{ color: tierColor, fontSize: 20, fontWeight: 900, letterSpacing: '-0.5px' }}>
                {tier} {rank}
              </div>
              <div style={{ color: '#e8e0d0', fontSize: 14, fontWeight: 700, marginTop: 2 }}>
                {lp} LP
              </div>
            </div>
          </div>

          {/* 승패 바 */}
          {total > 0 && (
            <div style={{ marginBottom: 12 }}>
              <div style={{ height: 6, borderRadius: 3, background: '#2a3a4a', overflow: 'hidden' }}>
                <div style={{
                  height: '100%',
                  width: `${winRate}%`,
                  background: `linear-gradient(90deg, ${tierColor}, ${tierColor}cc)`,
                  borderRadius: 3,
                  transition: 'width 0.6s ease',
                }} />
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6, fontSize: 12 }}>
                <span style={{ color: '#3b82f6', fontWeight: 600 }}>{wins}승</span>
                <span style={{ color: winRate >= 50 ? tierColor : '#9ca3af', fontWeight: 700 }}>
                  {winRate}%
                </span>
                <span style={{ color: '#ef4444', fontWeight: 600 }}>{losses}패</span>
              </div>
            </div>
          )}

          {/* 전적 합계 */}
          <div style={{ color: '#6b7a8d', fontSize: 12 }}>
            {total}전 {wins}승 {losses}패
          </div>
        </>
      )}
    </div>
  );
}
