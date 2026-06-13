import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import apiClient from '../api/client';
import SummonerProfilePage from './SummonerProfilePage';

/**
 * URL: /find/:region/:slug
 *   region = "kr", "na", ...
 *   slug   = "Hide%20on%20bush-KR1"  (name-tagLine, URL 디코딩 후 마지막 "-" 기준 분리)
 *
 * 파싱 규칙: slug에서 마지막 "-"를 태그 구분자로 사용
 *   "Hide on bush-KR1"  → name="Hide on bush", tagLine="KR1"
 *   "Hide on bush"      → name="Hide on bush", tagLine=""
 */
function parseSlug(slug) {
  const decoded = decodeURIComponent(slug);
  const idx = decoded.lastIndexOf('-');
  if (idx > 0) {
    return { name: decoded.slice(0, idx), tagLine: decoded.slice(idx + 1) };
  }
  return { name: decoded, tagLine: '' };
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
      const sumRes = await apiClient.get('/api/summoners', {
        params: { name, tagLine: tagLine || undefined, region: regionUpper },
      });
      const summonerData = sumRes.data;
      setSummoner(summonerData);
      const [rankRes, matchRes] = await Promise.all([
        apiClient.get('/api/league/entries', { params: { puuid: summonerData.puuid } }),
        apiClient.get('/api/matches',         { params: { puuid: summonerData.puuid } }),
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
      await apiClient.post('/api/matches/refresh', null, {
        params: { puuid: summoner.puuid },
      });
      const [rankRes, matchRes] = await Promise.all([
        apiClient.get('/api/league/entries', { params: { puuid: summoner.puuid } }),
        apiClient.get('/api/matches',         { params: { puuid: summoner.puuid } }),
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

  /* ── 로딩 ── */
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

  /* ── 에러 ── */
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
