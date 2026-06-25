import apiClient from './client';

// 매치 목록 조회
export const getMatches = (puuid) =>
  apiClient.get('/api/matches', { params: { puuid } });

// 매치 상세 조회
export const getMatchSummary = (matchId) =>
  apiClient.get(`/api/matches/${matchId}/summary`);

// 매치 타임라인(맵/이벤트/골드 그래프)
export const getMatchTimeline = (matchId) =>
  apiClient.get(`/api/matches/${matchId}/timeline`);

// 전적 갱신
export const refreshMatches = (puuid) =>
  apiClient.post('/api/matches/refresh', null, { params: { puuid } });
