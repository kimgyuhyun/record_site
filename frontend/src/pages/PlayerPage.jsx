import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getSummoner } from '../api/summoner';
import { getLeagueEntries } from '../api/league';
import { getMatches, refreshMatches } from '../api/match';
import SummonerProfilePage from './SummonerProfilePage';

/**
 * URL 패턴: /find/:region/:slug
 *   slug = "Hide%20on%20bush-KR1"
 *          → decodeURIComponent → "Hide on bush-KR1"
 *          → lastIndexOf('-')   → name="Hide on bush", tagLine="KR1"
 */
function parseSlug(slug) {
  const decoded = decodeURIComponent(slug);
  const idx = decoded.lastIndexOf('-');
  if (idx > 0) {
    return { name: decoded.slice(0, idx).trim(), tagLine: decoded.slice(idx + 1).trim() };
  }
  return { name: decoded.trim(), tagLine: '' };
}

export default function PlayerPage() {
  const { region, slug } = useParams();
  const navigate = useNavigate();

  const [summoner, setSummoner]       = useState(null);
  const [rankEntries, setRankEntries] = useState([]);
  const [matchList, setMatchList]     = useState([]);
  const [loading, setLoading]         = useState(true);
  const [refreshing, setRefreshing]   = useState(false);
  const [error, setError]             = useState('');

  useEffect(() => {
    const { name, tagLine } = parseSlug(slug);
    load(name, tagLine, region.toUpperCase());
  }, [region, slug]);

  const load = async (name, tagLine, regionUpper) => {
    setLoading(true);
    setError('');
    setSummoner(null);
    setRankEntries([]);
    setMatchList([]);
    try {
      const sumRes = await getSummoner(name, tagLine, regionUpper);
      const summonerData = sumRes.data;
      setSummoner(summonerData);

      const [rankRes, matchRes] = await Promise.all([
        getLeagueEntries(summonerData.puuid),
        getMatches(summonerData.puuid),
      ]);
      setRankEntries(rankRes.data || []);
      setMatchList(matchRes.data?.content || []);
    } catch (err) {
      console.error(err);
      setError('소환사를 찾을 수 없습니다. 이름과 태그를 확인하세요.');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    if (!summoner?.puuid) return;
    setRefreshing(true);
    setError('');
    try {
      await refreshMatches(summoner.puuid);
      const [rankRes, matchRes] = await Promise.all([
        getLeagueEntries(summoner.puuid),
        getMatches(summoner.puuid),
      ]);
      setRankEntries(rankRes.data || []);
      setMatchList(matchRes.data?.content || []);
    } catch (err) {
      console.error(err);
      setError('전적 갱신 중 오류가 발생했습니다.');
    } finally {
      setRefreshing(false);
    }
  };

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
      <div style={{ color: '#4a6080', fontSize: 14 }}>조회 중...</div>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );

  if (error) return (
    <div style={{
      display: 'flex', justifyContent: 'center', alignItems: 'center',
      minHeight: 'calc(100vh - 52px)', flexDirection: 'column', gap: 12,
    }}>
      <div style={{ fontSize: 40 }}>😞</div>
      <div style={{ color: '#ef9a9a', fontSize: 16, fontWeight: 600 }}>{error}</div>
      <button onClick={() => navigate('/')} style={{
        background: 'transparent', border: '1px solid #2a3a4a',
        color: '#6b7a8d', borderRadius: 6, padding: '8px 20px',
        fontSize: 13, cursor: 'pointer', marginTop: 4,
      }}>
        홈으로 돌아가기
      </button>
    </div>
  );

  return (
    <SummonerProfilePage
      summoner={summoner}
      rankEntries={rankEntries}
      matchList={matchList}
      champStats={[]}
      onRefresh={handleRefresh}
      refreshing={refreshing}
    />
  );
}
