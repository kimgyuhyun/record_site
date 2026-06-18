import { useState, useEffect } from 'react';
import { getChampionData } from '../api/ddragon';

/*
 * Data Dragon 챔피언 메타데이터 로딩 훅.
 *  - championKeyById:  championId(int) → ddragon 키(영문 id, 아이콘 경로용)
 *  - championNameById: championId(int) → 현지화 이름(한글)
 * ko_KR 우선, 실패 시 en_US 폴백.
 */
export default function useChampionMeta() {
  const [championKeyById, setChampionKeyById] = useState({});
  const [championNameById, setChampionNameById] = useState({});

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const res = await getChampionData('ko_KR').catch(() => getChampionData('en_US'));
        const keyById = {};
        const nameById = {};
        Object.values(res.data.data).forEach(c => {
          const id = Number(c.key);
          keyById[id] = c.id;
          nameById[id] = c.name;
        });
        if (!cancelled) {
          setChampionKeyById(keyById);
          setChampionNameById(nameById);
        }
      } catch (e) {
        console.error('챔피언 메타데이터 로드 실패', e);
      }
    })();
    return () => { cancelled = true; };
  }, []);

  return { championKeyById, championNameById };
}
