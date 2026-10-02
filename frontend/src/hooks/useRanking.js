import { useEffect, useState } from 'react';
import { getRanking } from '../api/ranking';

/*
 * 상위 티어 사다리 랭킹 로딩 훅.
 *  - queueType/page 변경 시 재요청. 응답은 RankingPageDto(content/page/size/totalElements/totalPages).
 *  - 프로젝트 기존 패턴(useChampionRotation)과 동일하게 수동 로딩/에러 상태로 관리한다.
 */
export default function useRanking(queueType, page, size = 50) {
  // 응답이 어느 요청의 것인지 key 로 남긴다. key 가 지금 요청과 다르면 아직 로딩 중이다.
  // 응답 전에는 직전 data 를 그대로 돌려주고, 실패하면 직전 data 를 남긴 채 isError 만 켠다.
  const key = `${queueType}|${page}|${size}`;
  const [result, setResult] = useState({ key: null, data: null, isError: false });

  useEffect(() => {
    let cancelled = false;
    getRanking(queueType, page, size)
      .then(res => { if (!cancelled) setResult({ key, data: res.data, isError: false }); })
      .catch(() => { if (!cancelled) setResult(prev => ({ key, data: prev.data, isError: true })); });
    return () => { cancelled = true; };
  }, [key, queueType, page, size]);

  const isLoading = result.key !== key;
  return { data: result.data, isLoading, isError: !isLoading && result.isError };
}
