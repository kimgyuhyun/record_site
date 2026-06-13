import React, { useState } from 'react';
import UserInfo from '../components/profile/UserInfo';
import RankTierBox from '../components/profile/RankTierBox';
import ChampionStatTable from '../components/profile/ChampionStatTable';
import TabNav from '../components/profile/TabNav';
import MatchList from '../components/profile/MatchList';

function parseRankData(entries) {
  const result = { solo: null, flex: null };
  for (const entry of entries) {
    const data = {
      tier: entry.tier, rank: entry.rank,
      lp: entry.leaguePoints, wins: entry.wins, losses: entry.losses,
    };
    if (entry.queueType === 'RANKED_SOLO_5x5') result.solo = data;
    if (entry.queueType === 'RANKED_FLEX_SR')  result.flex = data;
  }
  return result;
}

export default function SummonerProfilePage({
  summoner, rankEntries, matchList = [], champStats,
  onRefresh, refreshing, championKeyById,
}) {
  const [mainTab, setMainTab] = useState('챔피언');
  const [subTab, setSubTab]   = useState('전체');

  const rankData = rankEntries ? parseRankData(rankEntries) : { solo: null, flex: null };

  return (
    <div style={{
      maxWidth: 1000,
      margin: '0 auto',
      padding: '24px 16px',
      fontFamily: "'Pretendard', 'Apple SD Gothic Neo', 'Noto Sans KR', system-ui, sans-serif",
    }}>
      {/* 1. 유저 정보 */}
      <UserInfo summoner={summoner} onRefresh={onRefresh} refreshing={refreshing} />

      {/* 2. 랭크 박스 */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 24, flexWrap: 'wrap' }}>
        <RankTierBox title="개인 / 2인전"   rankData={rankData.solo} />
        <RankTierBox title="자유 5대5 대전" rankData={rankData.flex} />
      </div>

      {/* 3. 탭 */}
      <TabNav mainTab={mainTab} setMainTab={setMainTab} subTab={subTab} setSubTab={setSubTab} />

      {/* 4. 탭 콘텐츠 */}
      {mainTab === '챔피언' && (
        <ChampionStatTable stats={champStats || []} championKeyById={championKeyById || {}} />
      )}
      {mainTab === '게임 관전하기 - 인게임 정보' && (
        <div style={{
          background: '#111c27', border: '1px solid #2a3a4a',
          borderRadius: 10, padding: '48px 0',
          textAlign: 'center', color: '#4a5568', fontSize: 15,
        }}>
          현재 인게임 중이 아닙니다.
        </div>
      )}

      {/* 5. 매치 리스트 */}
      <div style={{ marginTop: 24 }}>
        <div style={{
          color: '#6b7a8d', fontSize: 12, fontWeight: 600,
          letterSpacing: '0.5px', marginBottom: 12, textTransform: 'uppercase',
        }}>
          최근 전적 ({matchList.length})
        </div>
        <MatchList matches={matchList} />
      </div>
    </div>
  );
}
