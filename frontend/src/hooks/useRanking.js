import { useEffect, useState } from 'react';
import { getRanking } from '../api/ranking';

/*
 * 상위 티어 사다리 랭킹 로딩 훅.
 *  - queueType/page 변경 시 재요청. 응답은 RankingPageDto(content/page/size/totalElements/totalPages).
 *  - 프로젝트 기존 패턴(useChampionRotation)과 동일하게 수동 로딩/에러 상태로 관리한다.
 */
export default function useRanking(queueType, page, size = 50) {
  const [data, setData] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isError, setIsError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    setIsError(false);
    getRanking(queueType, page, size)
      .then(res => { if (!cancelled) setData(res.data); })
      .catch(() => { if (!cancelled) setIsError(true); })
      .finally(() => { if (!cancelled) setIsLoading(false); });
    return () => { cancelled = true; };
  }, [queueType, page, size]);

  return { data, isLoading, isError };
}
