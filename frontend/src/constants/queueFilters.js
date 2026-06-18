/*
 * 매치 큐 필터 정의.
 *  - 메인 탭 3종(솔로/자유/총력전)은 정확히 매핑, 그 외 모드는 '큐 타입' 드롭다운에 모음.
 *  - queues: null 이면 전체, 아니면 해당 queueId 집합만 통과.
 * (UI 컴포넌트와 분리 — Fast Refresh 규칙 및 역할별 폴더 분리)
 */
export const QUEUE_FILTERS = {
  ALL:  { label: '전체',              queues: null },
  SOLO: { label: '개인/2인 랭크 게임', queues: [420] },
  FLEX: { label: '자유 랭크 게임',     queues: [440] },
  ARAM: { label: '무작위 총력전',      queues: [450] },
};

// '큐 타입' 드롭다운에 들어가는 나머지 모드들
//  - modes: queueId가 바뀌어도 안전하게 잡기 위한 gameMode 매칭(예: 아레나 = CHERRY)
export const QUEUE_TYPE_OPTIONS = [
  { key: 'NORMAL',  label: '일반',          queues: [400, 430, 490] },
  { key: 'BOT',     label: 'AI 상대 대전',   queues: [830, 840, 850, 870, 880, 890] },
  { key: 'URF',     label: 'U.R.F.',        queues: [900, 1010, 1900], modes: ['URF', 'ARURF'] },
  { key: 'CLASH',   label: '격전',          queues: [700, 720] },
  { key: 'ARENA',   label: '아레나',         queues: [1700, 1701, 1710], modes: ['CHERRY'] },
  { key: 'NEXUS',   label: '돌격! 넥서스',   queues: [1300], modes: ['NEXUSBLITZ'] },
  { key: 'DOOM',    label: '초토화 봇',      queues: [950, 960], modes: ['DOOMBOTSTEEMO'] },
  { key: 'SPECIAL', label: '특별 게임 모드',  queues: [1200, 1400, 610, 1810, 1820, 1830, 1840] },
];

export const ALL_QUEUE_FILTERS = {
  ...QUEUE_FILTERS,
  ...Object.fromEntries(QUEUE_TYPE_OPTIONS.map(o => [o.key, { label: o.label, queues: o.queues, modes: o.modes }])),
};

export const DROPDOWN_FILTER_KEYS = new Set(QUEUE_TYPE_OPTIONS.map(o => o.key));

// 선택된 필터 key로 매치 목록 필터링 (queueId 또는 gameMode 매칭)
export function filterMatchesByQueue(matches, key) {
  const def = ALL_QUEUE_FILTERS[key];
  if (!def || (def.queues == null && def.modes == null)) return matches;
  const queueSet = new Set(def.queues || []);
  const modeSet = new Set(def.modes || []);
  return matches.filter(m => queueSet.has(m.queueId) || modeSet.has(m.gameMode));
}
