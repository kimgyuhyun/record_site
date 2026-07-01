import apiClient from './client';

// 챔피언 팁(코멘트) API — 챔피언 상세 페이지 하단 팁 게시판에서 사용.

// 목록. sort: 'popular'(기본) | 'recent'
export const getChampionTips = (championId, { sort = 'popular', page = 0, size = 20 } = {}) =>
  apiClient.get('/api/champion-tips', { params: { championId, sort, page, size } });

// 작성. { championId, nickname, content, password } — password 는 삭제용 키
export const createChampionTip = ({ championId, nickname, content, password }) =>
  apiClient.post('/api/champion-tips', { championId, nickname, content, password });

// 삭제. 작성 시 정한 비밀번호가 일치해야 한다.
export const deleteChampionTip = (tipId, password) =>
  apiClient.delete(`/api/champion-tips/${tipId}`, { data: { password } });

// 추천/비추천. direction: 'UP' | 'DOWN'
export const voteChampionTip = (tipId, direction) =>
  apiClient.post(`/api/champion-tips/${tipId}/vote`, null, { params: { direction } });

// 신고(누적 시 자동 숨김)
export const reportChampionTip = (tipId) =>
  apiClient.post(`/api/champion-tips/${tipId}/report`);
