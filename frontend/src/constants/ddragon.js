/**
 * Data Dragon 이미지 상수
 *
 * 개발(Vite): public/cdn/... → /cdn/... 로 자동 서빙
 * 배포(Docker): Nginx가 /cdn/... 정적 파일 서빙
 *
 * 버전 바꿀 때 VERSION 하나만 수정하면 됩니다.
 */
export const DDRAGON_VERSION = '16.5.1';

// 이미지 (로컬 static)
export const IMG_BASE = `/cdn/${DDRAGON_VERSION}`;

// JSON 데이터는 외부 CDN 사용 (용량 문제로 로컬 저장 X)
export const DATA_CDN = `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}`;

// 이미지 URL 헬퍼
export const imgChampion    = (key)  => `${IMG_BASE}/img/champion/${key}.png`;
export const imgItem        = (id)   => `${IMG_BASE}/img/item/${id}.png`;
export const imgSpell       = (file) => `${IMG_BASE}/img/spell/${file}`;
// profileicon은 로컬에 없으므로 외부 CDN 사용
export const imgProfileIcon = (id)   => `${DATA_CDN}/img/profileicon/${id}.png`;
