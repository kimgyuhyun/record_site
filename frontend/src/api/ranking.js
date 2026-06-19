import apiClient from './client';

// 상위 티어 사다리 랭킹 조회 (주기 갱신된 스냅샷)
// queueType: 'SOLO' | 'FLEX'
export const getRanking = (queueType = 'SOLO', page = 0, size = 50) =>
  apiClient.get('/api/ranking', { params: { queueType, page, size } });
