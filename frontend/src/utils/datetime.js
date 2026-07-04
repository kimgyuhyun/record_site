// 프로젝트 전역 시간 처리 유틸.
// 규약: 백엔드는 모든 시각을 UTC ISO-8601('...Z')로 내려주고, 프론트는 이를 각 사용자의 로컬 시각으로 변환해 표시한다.

// 서버 시각 문자열/epoch(ms)/Date → Date. 방어적으로 오프셋 표기가 없는 문자열은 UTC 로 간주해 'Z' 를 붙인다
// (구 데이터나 오프셋 없이 내려오는 경로 대비).
export function parseServerTime(value) {
  if (value == null) return null;
  if (value instanceof Date) return value;
  if (typeof value === 'number') return new Date(value);
  const s = String(value);
  const hasZone = /[zZ]$|[+-]\d{2}:?\d{2}$/.test(s);
  return new Date(hasZone ? s : `${s}Z`);
}

// 절대 시각 → "3분 전" 상대 표기. 서버 시각(UTC)·epoch(ms) 모두 받는다.
export function timeAgo(value) {
  const date = parseServerTime(value);
  if (!date || Number.isNaN(date.getTime())) return '';
  const diff = Math.max(0, Date.now() - date.getTime());
  const m = Math.floor(diff / 60000);
  if (m < 1) return '방금 전';
  if (m < 60) return `${m}분 전`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}시간 전`;
  const d = Math.floor(h / 24);
  if (d < 7) return `${d}일 전`;
  if (d < 30) return `${Math.floor(d / 7)}주 전`;
  return `${Math.floor(d / 30)}개월 전`;
}
