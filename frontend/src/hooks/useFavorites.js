import useStoredList, { readList, writeList } from './useStoredList';

/*
 * 즐겨찾기 소환사 (localStorage).
 *  - 항목 구조는 최근 검색과 동일: { to, label, sub, ts }
 *  - to 를 고유 키로 사용한다.
 */
const STORAGE_KEY = 'recordsite:favorites';
const MAX = 30;

export function isFavorite(list, to) {
  return list.some(e => e.to === to);
}

export function toggleFavorite(entry) {
  if (!entry?.to) return;
  const list = readList(STORAGE_KEY);
  const exists = list.some(e => e.to === entry.to);
  const next = exists
    ? list.filter(e => e.to !== entry.to)
    : [{ ...entry, ts: Date.now() }, ...list].slice(0, MAX);
  writeList(STORAGE_KEY, next);
}

export function removeFavorite(to) {
  writeList(STORAGE_KEY, readList(STORAGE_KEY).filter(e => e.to !== to));
}

export default function useFavorites() {
  return useStoredList(STORAGE_KEY);
}
