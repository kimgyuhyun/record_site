import apiClient from './client';

// 챔피언 팁(코멘트) API — 챔피언 상세 페이지 하단 팁 게시판에서 사용.

// 목록. sort: 'popular'(기본) | 'recent'. language/patchVersion 지정 시 해당 값만 필터.
export const getChampionTips = (championId, { sort = 'popular', language, patchVersion, page = 0, size = 20 } = {}) =>
  apiClient.get('/api/champion-tips', {
    params: {
      championId, sort, page, size,
      ...(language ? { language } : {}),
      ...(patchVersion ? { patchVersion } : {}),
    },
  });

// 작성. { championId, nickname, content, password, language } — password 는 삭제/수정용 키
export const createChampionTip = ({ championId, nickname, content, password, language }) =>
  apiClient.post('/api/champion-tips', { championId, nickname, content, password, language });

// 수정. 작성 시 정한 비밀번호가 일치해야 내용을 바꿀 수 있다.
export const updateChampionTip = (tipId, { password, content }) =>
  apiClient.put(`/api/champion-tips/${tipId}`, { password, content });

// 삭제. 작성 시 정한 비밀번호가 일치해야 한다.
export const deleteChampionTip = (tipId, password) =>
  apiClient.delete(`/api/champion-tips/${tipId}`, { data: { password } });

// 추천/비추천. direction: 'UP' | 'DOWN'
export const voteChampionTip = (tipId, direction) =>
  apiClient.post(`/api/champion-tips/${tipId}/vote`, null, { params: { direction } });

// 신고(누적 시 자동 숨김)
export const reportChampionTip = (tipId) =>
  apiClient.post(`/api/champion-tips/${tipId}/report`);
