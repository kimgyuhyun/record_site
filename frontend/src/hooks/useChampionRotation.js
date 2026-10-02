import { useEffect, useState } from 'react';
import { getChampionRotation } from '../api/champion';

/*
 * 현재 무료 로테이션 챔피언 로딩 훅 (백엔드 Riot 프록시).
 *  - freeChampionIds: 전체 유저 무료 로테이션 championId(int) 목록
 *  - 로딩/에러 상태를 명시적으로 관리한다.
 */
export default function useChampionRotation() {
  // 마운트 때 한 번만 요청하므로 응답이 왔는지(done)만 보면 된다.
  const [result, setResult] = useState({ done: false, freeChampionIds: [], isError: false });

  useEffect(() => {
    let cancelled = false;
    getChampionRotation()
      .then(res => {
        if (cancelled) return;
        setResult({ done: true, freeChampionIds: res.data?.freeChampionIds ?? [], isError: false });
      })
      .catch(() => { if (!cancelled) setResult(prev => ({ ...prev, done: true, isError: true })); });
    return () => { cancelled = true; };
  }, []);

  return { freeChampionIds: result.freeChampionIds, isLoading: !result.done, isError: result.isError };
}
