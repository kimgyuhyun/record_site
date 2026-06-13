import apiClient from './client';

// 랭크 정보 조회
export const getLeagueEntries = (puuid) =>
  apiClient.get('/api/league/entries', { params: { puuid } });
