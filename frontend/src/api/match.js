import apiClient from './client';

// 매치 목록 조회 (page/size 미지정 시 백엔드 기본 page=0, size=20)
export const getMatches = (puuid, page = 0, size = 20) =>
  apiClient.get('/api/matches', { params: { puuid, page, size } });

// 매치 상세 조회
export const getMatchSummary = (matchId) =>
  apiClient.get(`/api/matches/${matchId}/summary`);

// 매치 타임라인(맵/이벤트/골드 그래프)
export const getMatchTimeline = (matchId) =>
  apiClient.get(`/api/matches/${matchId}/timeline`);

// 전적 갱신 요청 → 작업 큐에 투입하고 { jobId, status } 즉시 응답(비동기)
export const refreshMatches = (puuid) =>
  apiClient.post('/api/matches/refresh', null, { params: { puuid } });

// 갱신 작업 진행 상황 폴링 → { jobId, status, total, done }
export const getRefreshJob = (jobId) =>
  apiClient.get(`/api/matches/refresh-jobs/${jobId}`);
