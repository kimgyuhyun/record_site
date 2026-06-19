import useStoredList, { readList, writeList } from './useStoredList';

/*
 * 최근 검색 기록 (localStorage).
 *  - 항목 구조: { to(이동 경로), label(표시명), sub(부가정보), ts(저장 시각) }
 *  - to 를 고유 키로 중복 제거하고 최신순으로 최대 MAX 개 유지한다.
 */
const STORAGE_KEY = 'recordsite:recent-searches';
const MAX = 10;

export function addRecentSearch(entry) {
  if (!entry?.to) return;
  const next = readList(STORAGE_KEY).filter(e => e.to !== entry.to);
  next.unshift({ ...entry, ts: Date.now() });
  writeList(STORAGE_KEY, next.slice(0, MAX));
}

export function removeRecentSearch(to) {
  writeList(STORAGE_KEY, readList(STORAGE_KEY).filter(e => e.to !== to));
}

export function clearRecentSearches() {
  writeList(STORAGE_KEY, []);
}

export default function useRecentSearches() {
  return useStoredList(STORAGE_KEY);
}
