import apiClient from './client';
import { DATA_CDN, runesReforgedUrl } from '../constants/ddragon';

// Data Dragon 정적 데이터 조회
//  - 외부 CDN을 직접 호출하므로 절대 URL을 사용한다 (axios 공통 클라이언트 재사용)

// 소환사 주문 메타데이터 (스펠 아이콘 매핑용)
export const getSummonerSpellData = (locale) =>
  apiClient.get(`${DATA_CDN}/data/${locale}/summoner.json`);

// 챔피언 메타데이터 (championId → 키 매핑용)
export const getChampionData = (locale) =>
  apiClient.get(`${DATA_CDN}/data/${locale}/champion.json`);

// 룬 메타데이터 (핵심룬/계열 아이콘 매핑용)
export const getRuneData = (locale) =>
  apiClient.get(runesReforgedUrl(locale));
