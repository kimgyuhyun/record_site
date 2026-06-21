import React, { useState, useEffect, useMemo } from 'react';
import UserInfo from '../components/profile/UserInfo';
import RankTierBox from '../components/profile/RankTierBox';
import RecentGamesSummary from '../components/profile/RecentGamesSummary';
import ChampionStatTable from '../components/profile/ChampionStatTable';
import ChampionMasterySection from '../components/profile/ChampionMasterySection';
import TabNav from '../components/profile/TabNav';
import MatchFilterBar from '../components/profile/MatchFilterBar';
import MatchList from '../components/profile/MatchList';
import LiveGamePanel from '../components/profile/LiveGamePanel';
import useChampionMeta from '../hooks/useChampionMeta';
import { getChampionStats, getChampionMastery } from '../api/champion';
import { filterMatchesByQueue } from '../constants/queueFilters';

// summoner 객체에서 직접 solo/flex 랭크 추출
function parseRankFromSummoner(summoner) {
  if (!summoner) return { solo: null, flex: null };

  const solo = summoner.soloTier ? {
    tier:   summoner.soloTier,
    rank:   summoner.soloRank,
    lp:     summoner.soloLp,
    wins:   summoner.soloWins,
    losses: summoner.soloLosses,
  } : null;

  const flex = summoner.flexTier ? {
    tier:   summoner.flexTier,
    rank:   summoner.flexRank,
    lp:     summoner.flexLp,
    wins:   summoner.flexWins,
    losses: summoner.flexLosses,
  } : null;

  return { solo, flex };
}

// 챔피언 통계 서브탭(전체/솔로/자유) → 백엔드 queueType 파라미터
const QUEUE_TYPE_BY_SUBTAB = { '전체': undefined, '솔로랭크': 'SOLO', '자유랭크': 'FLEX' };

export default function SummonerProfilePage({
  summoner, matchList = [],
  onRefresh, refreshing, cooldown = 0,
}) {
  const [mainTab, setMainTab] = useState('챔피언');
  const [subTab, setSubTab]   = useState('전체');
  const [champSearch, setChampSearch]   = useState('');
  const [queueFilter, setQueueFilter]   = useState('ALL');

  const [champStats, setChampStats]     = useState([]);
  const [champLoading, setChampLoading] = useState(false);

  const [mastery, setMastery]           = useState([]);
  const [masteryLoading, setMasteryLoading] = useState(false);

  const { championKeyById, championNameById } = useChampionMeta();
  const { solo, flex } = parseRankFromSummoner(summoner);

  // 챔피언 통계 조회 (puuid·서브탭·매치목록 변경 시). 챔피언 탭일 때만.
  useEffect(() => {
    const puuid = summoner?.puuid;
    if (!puuid || mainTab !== '챔피언') return;
    let cancelled = false;

    const loadChampionStats = async () => {
      setChampLoading(true);
      try {
        const res = await getChampionStats(puuid, QUEUE_TYPE_BY_SUBTAB[subTab]);
        if (!cancelled) setChampStats(res.data || []);
      } catch (e) {
        console.error('챔피언 통계 조회 실패', e);
        if (!cancelled) setChampStats([]);
      } finally {
        if (!cancelled) setChampLoading(false);
      }
    };

    loadChampionStats();
    return () => { cancelled = true; };
  }, [summoner?.puuid, subTab, mainTab, matchList]);

  // 챔피언 숙련도 조회 (puuid 변경 시 라이브 조회)
  useEffect(() => {
    const puuid = summoner?.puuid;
    if (!puuid) return;
    let cancelled = false;

    const loadMastery = async () => {
      setMasteryLoading(true);
      try {
        const res = await getChampionMastery(puuid);
        if (!cancelled) setMastery(res.data || []);
      } catch (e) {
        console.error('챔피언 숙련도 조회 실패', e);
        if (!cancelled) setMastery([]);
      } finally {
        if (!cancelled) setMasteryLoading(false);
      }
    };

    loadMastery();
    return () => { cancelled = true; };
  }, [summoner?.puuid]);

  // 큐 필터 적용된 매치 목록
  const filteredMatches = useMemo(
    () => filterMatchesByQueue(matchList, queueFilter),
    [matchList, queueFilter],
  );

  return (
    <div style={{
      maxWidth: 1000, margin: '0 auto', padding: '24px 16px',
      fontFamily: "'Pretendard', 'Apple SD Gothic Neo', 'Noto Sans KR', system-ui, sans-serif",
    }}>
      {/* 1. 유저 정보 */}
      <UserInfo summoner={summoner} onRefresh={onRefresh} refreshing={refreshing} cooldown={cooldown} />

      {/* 2. 랭크 박스 */}
      <div style={{ display: 'flex', gap: 16, marginBottom: 16, flexWrap: 'wrap' }}>
        <RankTierBox title="개인 / 2인전"   rankData={solo} />
        <RankTierBox title="자유 5대5 대전" rankData={flex} />
      </div>

      {/* 3. 탭 */}
      <TabNav mainTab={mainTab} setMainTab={setMainTab} subTab={subTab} setSubTab={setSubTab} />

      {/* 4. 탭 콘텐츠 (챔피언 통계) */}
      {mainTab === '챔피언' && (
        champLoading && champStats.length === 0 ? (
          <div style={{
            background: '#111c27', border: '1px solid #2a3a4a', borderTop: 'none',
            borderRadius: '0 0 10px 10px', padding: '32px 0',
            textAlign: 'center', color: '#4a5568', fontSize: 14,
          }}>
            챔피언 통계 조회 중...
          </div>
        ) : (
          <ChampionStatTable
            stats={champStats}
            championKeyById={championKeyById}
            championNameById={championNameById}
            search={champSearch}
          />
        )
      )}
      {mainTab === '게임 관전하기 - 인게임 정보' && (
        <LiveGamePanel puuid={summoner?.puuid} championKeyById={championKeyById} />
      )}

      {/* 5. 챔피언 숙련도 */}
      <div style={{ marginTop: 16 }}>
        <ChampionMasterySection
          mastery={mastery}
          championKeyById={championKeyById}
          championNameById={championNameById}
          loading={masteryLoading}
        />
      </div>

      {/* 6. 최근 게임 요약 */}
      <RecentGamesSummary
        matches={matchList}
        championKeyById={championKeyById}
        search={champSearch}
        onSearchChange={setChampSearch}
      />

      {/* 6. 매치 큐 필터 (최근 전적 바로 위) */}
      <div style={{ marginTop: 8 }}>
        <MatchFilterBar value={queueFilter} onChange={setQueueFilter} />
      </div>

      {/* 7. 최근 전적 */}
      <div style={{ marginTop: 16 }}>
        <div style={{
          color: '#6b7a8d', fontSize: 12, fontWeight: 600,
          letterSpacing: '0.5px', marginBottom: 12, textTransform: 'uppercase',
        }}>
          최근 전적 ({filteredMatches.length})
        </div>
        <MatchList matches={filteredMatches} />
      </div>
    </div>
  );
}
