import apiClient from './client';

// 소환사가 플레이한 챔피언별 통계 조회
// queueType: undefined(전체) | 'SOLO' | 'FLEX'
export const getChampionStats = (puuid, queueType) =>
  apiClient.get('/api/champion-stats', {
    params: queueType ? { puuid, queueType } : { puuid },
  });

// 챔피언 숙련도 상위 N개 조회 (Riot 라이브 데이터)
export const getChampionMastery = (puuid, limit = 12) =>
  apiClient.get('/api/champion-mastery', { params: { puuid, limit } });

// 현재 무료 로테이션 챔피언 조회 (Riot 라이브 데이터)
export const getChampionRotation = () =>
  apiClient.get('/api/champion-rotation');

// 전역 챔피언 티어 리스트 조회 (자체 수집 매치 DB 집계)
// queueType: undefined(전체) | 'SOLO' | 'FLEX'
export const getChampionTierList = (queueType) =>
  apiClient.get('/api/champion-stats/tier-list', {
    params: queueType ? { queueType } : {},
  });
