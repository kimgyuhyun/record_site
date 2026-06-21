import apiClient from './client';

// 인게임(실시간) 정보 조회.
//  - 게임 중: 200 + 게임 정보
//  - 게임 중 아님: 204(No Content) → res.status === 204, res.data 비어있음
export const getLiveGame = (puuid) =>
  apiClient.get('/api/live-game', { params: { puuid } });
