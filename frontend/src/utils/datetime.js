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
// 구간: 1분 미만=방금 전 → 1시간 미만=분 → 24시간 미만=시간 → 7일 미만=일 → 30일 미만=주일 → 그 이상=개월.
// 경계는 아래 값 '미만'까지 이전 단위를 유지한다(예: 6일 23시간 59분은 아직 6일 전, 딱 7일부터 1주일 전).
export function timeAgo(value) {
  const date = parseServerTime(value);
  if (!date || Number.isNaN(date.getTime())) return '';
  const seconds = Math.max(0, Math.floor((Date.now() - date.getTime()) / 1000));

  const minutes = Math.floor(seconds / 60);
  if (minutes < 1) return '방금 전';
  if (minutes < 60) return `${minutes}분 전`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}시간 전`;

  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}일 전`;

  const weeks = Math.floor(days / 7);
  if (days < 30) return `${weeks}주일 전`;

  const months = Math.floor(days / 30);
  return `${months}개월 전`;
}
