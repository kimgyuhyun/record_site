import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getSummoner } from '../api/summoner';
import { getMatches, refreshMatches } from '../api/match';
import SummonerProfilePage from './SummonerProfilePage';

function parseSlug(slug) {
  const decoded = decodeURIComponent(slug);
  const idx = decoded.lastIndexOf('-');
  if (idx > 0) {
    const maybeTag = decoded.slice(idx + 1);
    if (/^[a-zA-Z0-9]+$/.test(maybeTag)) {
      return { name: decoded.slice(0, idx).trim(), tagLine: maybeTag.trim() };
    }
  }
  return { name: decoded.trim(), tagLine: '' };
}

export default function PlayerPage() {
  const { region, slug } = useParams();
  const navigate = useNavigate();

  const [summoner,   setSummoner]   = useState(null);
  const [matchList,  setMatchList]  = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error,      setError]      = useState('');

  useEffect(() => {
    const { name, tagLine } = parseSlug(slug);
    load(name, tagLine, region.toUpperCase());
  }, [region, slug]);

  const load = async (name, tagLine, regionUpper) => {
    setLoading(true);
    setError('');
    setSummoner(null);
    setMatchList([]);
    try {
      // getSummoner 응답에 랭크(soloTier 등)가 이미 포함돼 있음
      const sumRes = await getSummoner(name, tagLine, regionUpper);
      setSummoner(sumRes.data);

      const matchRes = await getMatches(sumRes.data.puuid);
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
      // 갱신 후 소환사 정보(랭크 포함) + 매치 재조회
      const { name, tagLine } = parseSlug(slug);
      const [sumRes, matchRes] = await Promise.all([
        getSummoner(name, tagLine, region.toUpperCase()),
        getMatches(summoner.puuid),
      ]);
      setSummoner(sumRes.data);
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
      minHeight: 'calc(100vh - 52px)',
    }}>
      <div style={{
        background: '#111c27', border: '1px solid #1e2d3d',
        borderRadius: 8, padding: '40px 60px',
        textAlign: 'center', minWidth: 320,
      }}>
        <div style={{
          fontSize: 16, color: '#e2e8f0', fontWeight: 500, marginBottom: 24,
          padding: '8px 20px', border: '1px solid #2a3a4a',
          borderRadius: 4, display: 'inline-block', minWidth: 200,
        }}>
          {decodeURIComponent(slug).replace(/-([^-]+)$/, '#$1')}
        </div>
        <div style={{ color: '#6b7a8d', fontSize: 14, lineHeight: 1.9, marginBottom: 10 }}>
          해당 이름의 소환사를 찾을 수 없습니다.<br />
          정확한 태그명(#???)과 함께 검색해 주세요.
        </div>
        <div style={{ color: '#3a4a5a', fontSize: 13 }}>
          ( 예: hideonbush#KR1 )
        </div>
      </div>
    </div>
  );

  return (
    <SummonerProfilePage
      summoner={summoner}
      matchList={matchList}
      champStats={[]}
      onRefresh={handleRefresh}
      refreshing={refreshing}
    />
  );
}
