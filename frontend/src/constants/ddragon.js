/**
 * Data Dragon 이미지 상수
 *
 * 개발(Vite): public/cdn/... → /cdn/... 로 자동 서빙
 * 배포(Docker): Nginx가 /cdn/... 정적 파일 서빙
 *
 * 버전 바꿀 때 VERSION 하나만 수정하면 됩니다.
 */
export const DDRAGON_VERSION = '16.13.1';

// 이미지 (로컬 static, 패치 버전에 종속)
export const IMG_BASE = `/cdn/${DDRAGON_VERSION}`;

// rank/tier/objective는 ddragon 패치 자산이 아니라 직접 받아둔 고정 이미지라서
// 버전 폴더 밖의 경로에 둔다 (버전 올려도 깨지지 않게)
export const STATIC_IMG_BASE = '/static';

// JSON 데이터는 외부 CDN 사용 (용량 문제로 로컬 저장 X)
export const DATA_CDN = `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}`;

// 이미지 URL 헬퍼
export const imgChampion    = (key)  => `${IMG_BASE}/img/champion/${key}.png`;
export const imgItem        = (id)   => `${IMG_BASE}/img/item/${id}.png`;
export const imgSpell       = (file) => `${IMG_BASE}/img/spell/${file}`;
// 챔피언 스킬(Q/W/E/R) 아이콘은 로컬 static에 받아두지 않아 외부 CDN에서 직접 받는다
export const imgChampionSpell = (file) => `${DATA_CDN}/img/spell/${file}`;
// profileicon은 로컬에 없으므로 외부 CDN 사용
export const imgProfileIcon = (id)   => `${DATA_CDN}/img/profileicon/${id}.png`;

// 랭크 이미지 (tier: 'GOLD', 'SILVER' 등 대소문자 무관)
export const imgRank = (tier) =>
  `${STATIC_IMG_BASE}/rank/${(tier || 'unranked').toLowerCase()}.png`;

// 평균 티어 엠블럼 (전적 카드 전용 - rank 폴더와 별개 이미지 세트, .webp)
//  - 파일명: iron.webp · bronze.webp · silver.webp · gold.webp · platinum.webp
//    emerald.webp · diamond.webp · master.webp · grandmaster.webp · challenger.webp
export const imgTier = (tier) =>
  `${STATIC_IMG_BASE}/tier/${(tier || 'unranked').toLowerCase()}.webp`;

// 룬 이미지 (runesReforged.json의 icon 경로는 버전 없는 cdn/img/ 하위에서 서빙)
//  - icon 예: 'perk-images/Styles/Domination/Electrocute/Electrocute.png'
export const RUNE_IMG_BASE = 'https://ddragon.leagueoflegends.com/cdn/img/';
export const imgRune = (icon) => (icon ? `${RUNE_IMG_BASE}${icon}` : null);

// 스킨 일러스트(세로 카드용 로딩 화면, 308x560) — 버전 없는 cdn/img/ 하위에서 서빙
//  - key 는 ddragon 영문 id(예: 'Ahri'), num=0 은 기본 스킨
export const imgSkinLoading = (key, num = 0) =>
  `${RUNE_IMG_BASE}champion/loading/${key}_${num}.jpg`;

// 룬 메타데이터 JSON (핵심룬/계열 아이콘 매핑용)
export const runesReforgedUrl = (locale) =>
  `${DATA_CDN}/data/${locale}/runesReforged.json`;

// 오브젝트 이미지 (teamId: 100=블루, 200=레드)
// key: 'dragon' | 'baron' | 'tower' | 'inhibitor' | 'herald' | 'horde'
const OBJ_FILE_MAP = { voidGrub: 'horde', herald: 'herald', dragon: 'dragon', baron: 'baron', tower: 'tower', inhibitor: 'inhibitor' };
export const imgObjective = (key, teamId) =>
  `${STATIC_IMG_BASE}/objective/${OBJ_FILE_MAP[key] ?? key}-${teamId}.png`;
