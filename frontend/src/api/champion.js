import apiClient from './client';

// 소환사가 플레이한 챔피언별 통계 조회
// queueType: undefined(전체) | 'SOLO' | 'FLEX'
export const getChampionStats = (puuid, queueType) =>
  apiClient.get('/api/champion-stats', {
    params: queueType ? { puuid, queueType } : { puuid },
  });
