import apiClient from './client';

// 단건 정확히 조회 (이름+태그, DB 미스 시 Riot API 호출)
export const getSummoner = (name, tagLine, region) =>
  apiClient.get('/api/summoners', {
    params: { name, tagLine: tagLine || undefined, region },
  });

// 자동완성용 — 내 DB에서 name 포함 검색 (Riot API 호출 없음)
export const searchSummonerByName = (name) =>
  apiClient.get('/api/summoners/search', { params: { name } });
