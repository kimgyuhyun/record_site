import { useEffect, useState } from 'react';
import { getChampionRotation } from '../api/champion';

/*
 * 현재 무료 로테이션 챔피언 로딩 훅 (백엔드 Riot 프록시).
 *  - freeChampionIds: 전체 유저 무료 로테이션 championId(int) 목록
 *  - 로딩/에러 상태를 명시적으로 관리한다.
 */
export default function useChampionRotation() {
  const [freeChampionIds, setFreeChampionIds] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setIsError(false);
    getChampionRotation()
      .then(res => {
        if (cancelled) return;
        setFreeChampionIds(res.data?.freeChampionIds ?? []);
      })
      .catch(() => { if (!cancelled) setIsError(true); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, []);

  return { freeChampionIds, isLoading, isError };
}
