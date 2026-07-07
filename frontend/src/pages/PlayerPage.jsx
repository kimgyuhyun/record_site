import React, { useState, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getSummoner } from '../api/summoner';
import { getMatches, refreshMatches, getRefreshJob } from '../api/match';
import SummonerProfilePage from './SummonerProfilePage';
import { parseServerTime } from '../utils/datetime';

// 갱신 작업 진행 상황 폴링 주기(ms)
const REFRESH_POLL_INTERVAL_MS = 2500;

function parseSlug(slug) {
  const decoded = decodeURIComponent(slug);
  const idx = decoded.lastIndexOf('-');
  if (idx > 0) {
    const maybeTag = decoded.slice(idx + 1).trim();
    // Riot 태그는 최대 5자이며 한글·유니코드·공백도 허용된다(예: #무지의축복, #T 1).
    // 공백을 빠뜨리면 "해 태#T 1" 같은 태그가 이름에 합쳐져 검색이 깨진다.
    if (/^[\p{L}\p{N} ]{1,5}$/u.test(maybeTag)) {
      return { name: decoded.slice(0, idx).trim(), tagLine: maybeTag };
    }
  }
  return { name: decoded.trim(), tagLine: '' };
}

export default function PlayerPage() {
  const { region, slug } = useParams();

  const [summoner,   setSummoner]   = useState(null);
  const [matchList,  setMatchList]  = useState([]);
  const [loading,    setLoading]    = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error,      setError]      = useState('');
  const [cooldown,   setCooldown]   = useState(0); // 남은 쿨다운 초
  const cooldownTimer = useRef(null);
  const pollTimer     = useRef(null); // 갱신 작업 폴링 인터벌

  // 페이지 진입 시 rankUpdatedAt 기반으로 남은 쿨다운 계산
  const initCooldown = (summonerData) => {
    if (!summonerData?.rankUpdatedAt) return;
    // rankUpdatedAt 은 서버(UTC) 시각. parseServerTime 이 오프셋을 붙여 로컬 시각으로 정확히 변환한다
    // (백엔드가 'Z' 를 붙여 보내든 아니든 이중 'Z' 없이 안전하게 파싱한다).
    const updatedAt = parseServerTime(summonerData.rankUpdatedAt);
    const remaining = Math.ceil(180 - (Date.now() - updatedAt.getTime()) / 1000);
    if (remaining > 0) startCooldownTimer(remaining);
  };

  const startCooldownTimer = (seconds) => {
    if (cooldownTimer.current) clearInterval(cooldownTimer.current);
    setCooldown(seconds);
    cooldownTimer.current = setInterval(() => {
      setCooldown(prev => {
        if (prev <= 1) {
          clearInterval(cooldownTimer.current);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
  };

  useEffect(() => {
    const { name, tagLine } = parseSlug(slug);
    load(name, tagLine, region.toUpperCase());
    return () => {
      if (cooldownTimer.current) clearInterval(cooldownTimer.current);
      if (pollTimer.current) clearInterval(pollTimer.current); // 다른 소환사로 이동 시 진행 중 폴링 정리
    };
  }, [region, slug]);

  const load = async (name, tagLine, regionUpper) => {
    setLoading(true);
    setError('');
    setSummoner(null);
    setMatchList([]);
    // 다른 소환사로 이동 시 이전 소환사의 쿨다운이 남지 않도록 초기화
    if (cooldownTimer.current) clearInterval(cooldownTimer.current);
    setCooldown(0);
    try {
      // getSummoner 응답에 랭크(soloTier 등)가 이미 포함돼 있음
      const sumRes = await getSummoner(name, tagLine, regionUpper);
      setSummoner(sumRes.data);
      initCooldown(sumRes.data);

      const matchRes = await getMatches(sumRes.data.puuid);
      setMatchList(matchRes.data?.content || []);
    } catch (err) {
      console.error(err);
      setError('소환사를 찾을 수 없습니다. 이름과 태그를 확인하세요.');
    } finally {
      setLoading(false);
    }
  };

  // 갱신 요청 → 작업 큐에 투입(즉시 jobId) → 완료까지 폴링. 동기 대기 없이 진행률만 갱신한다.
  const handleRefresh = async () => {
    if (!summoner?.puuid || cooldown > 0 || refreshing) return;
    setRefreshing(true);
    setError('');
    try {
      const { data } = await refreshMatches(summoner.puuid);
      // 이미 끝난 작업(쿨다운 창에서 재요청 등)이면 바로 마무리, 아니면 폴링 시작
      if (data.status === 'DONE') {
        await finishRefresh();
      } else {
        startPolling(data.jobId);
      }
    } catch (err) {
      console.error(err);
      setError('전적 갱신 요청 중 오류가 발생했습니다.');
      stopRefreshing();
    }
  };

  // jobId 작업이 DONE/FAILED 될 때까지 일정 주기로 진행 상황을 조회한다.
  const startPolling = (jobId) => {
    if (pollTimer.current) clearInterval(pollTimer.current);
    pollTimer.current = setInterval(async () => {
      try {
        const { data } = await getRefreshJob(jobId);

        if (data.status === 'DONE') {
          clearInterval(pollTimer.current);
          await finishRefresh();
        } else if (data.status === 'FAILED') {
          clearInterval(pollTimer.current);
          setError('전적 갱신에 실패했습니다. 잠시 후 다시 시도해주세요.');
          stopRefreshing();
        }
      } catch (err) {
        console.error(err);
        clearInterval(pollTimer.current);
        setError('전적 갱신 상태 조회 중 오류가 발생했습니다.');
        stopRefreshing();
      }
    }, REFRESH_POLL_INTERVAL_MS);
  };

  // 갱신 완료 후 소환사 정보 + 매치 재조회, 쿨다운 시작.
  const finishRefresh = async () => {
    try {
      const { name, tagLine } = parseSlug(slug);
      const [sumRes, matchRes] = await Promise.all([
        getSummoner(name, tagLine, region.toUpperCase()),
        getMatches(summoner.puuid),
      ]);
      setSummoner(sumRes.data);
      setMatchList(matchRes.data?.content || []);
      startCooldownTimer(180);
    } catch (err) {
      console.error(err);
      setError('갱신 결과를 불러오는 중 오류가 발생했습니다.');
    } finally {
      stopRefreshing();
    }
  };

  const stopRefreshing = () => {
    setRefreshing(false);
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
      cooldown={cooldown}
    />
  );
}
