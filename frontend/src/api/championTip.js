import apiClient from './client';

// 챔피언 팁(코멘트) API — 챔피언 상세 페이지 하단 팁 게시판에서 사용.

// 목록. sort: 'popular'(기본) | 'recent'
export const getChampionTips = (championId, { sort = 'popular', page = 0, size = 20 } = {}) =>
  apiClient.get('/api/champion-tips', { params: { championId, sort, page, size } });

// 작성. { championId, nickname, content }
export const createChampionTip = ({ championId, nickname, content }) =>
  apiClient.post('/api/champion-tips', { championId, nickname, content });

// 추천/비추천. direction: 'UP' | 'DOWN'
export const voteChampionTip = (tipId, direction) =>
  apiClient.post(`/api/champion-tips/${tipId}/vote`, null, { params: { direction } });

// 신고(누적 시 자동 숨김)
export const reportChampionTip = (tipId) =>
  apiClient.post(`/api/champion-tips/${tipId}/report`);
