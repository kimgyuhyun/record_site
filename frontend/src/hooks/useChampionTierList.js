import { useEffect, useState } from 'react';
import { getChampionTierList } from '../api/champion';

/*
 * 전역 챔피언 티어 리스트 로딩 훅 (자체 수집 매치 DB 집계).
 *  - queueType 변경 시 재요청한다(undefined=전체 / 'SOLO' / 'FLEX').
 *  - 프로젝트 기존 패턴(useChampionRotation)과 동일하게 수동 로딩/에러 상태로 관리한다.
 */
export default function useChampionTierList(queueType) {
  // 응답이 어느 요청의 것인지 key 로 남긴다. key 가 지금 요청과 다르면 아직 로딩 중이다.
  // 응답 전에는 직전 rows 를 그대로 돌려주고, 실패하면 직전 rows 를 남긴 채 isError 만 켠다.
  const key = queueType ?? '';
  const [result, setResult] = useState({ key: null, rows: [], isError: false });

  useEffect(() => {
    let cancelled = false;
    getChampionTierList(queueType)
      .then(res => {
        if (cancelled) return;
        setResult({ key, rows: res.data ?? [], isError: false });
      })
      .catch(() => { if (!cancelled) setResult(prev => ({ key, rows: prev.rows, isError: true })); });
    return () => { cancelled = true; };
  }, [key, queueType]);

  const isLoading = result.key !== key;
  return { rows: result.rows, isLoading, isError: !isLoading && result.isError };
}
