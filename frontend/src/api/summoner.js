import apiClient from './client';

// 소환사 검색
export const getSummoner = (name, tagLine, region) =>
  apiClient.get('/api/summoners', {
    params: { name, tagLine: tagLine || undefined, region },
  });
