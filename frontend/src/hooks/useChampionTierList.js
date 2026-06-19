import { useEffect, useState } from 'react';
import { getChampionTierList } from '../api/champion';

/*
 * 전역 챔피언 티어 리스트 로딩 훅 (자체 수집 매치 DB 집계).
 *  - queueType 변경 시 재요청한다(undefined=전체 / 'SOLO' / 'FLEX').
 *  - 프로젝트 기존 패턴(useChampionRotation)과 동일하게 수동 로딩/에러 상태로 관리한다.
 */
export default function useChampionTierList(queueType) {
  const [rows, setRows] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setIsError(false);
    getChampionTierList(queueType)
      .then(res => {
        if (cancelled) return;
        setRows(res.data ?? []);
      })
      .catch(() => { if (!cancelled) setIsError(true); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, [queueType]);

  return { rows, isLoading, isError };
}
