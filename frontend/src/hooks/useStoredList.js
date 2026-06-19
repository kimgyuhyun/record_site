import { useEffect, useState } from 'react';

/*
 * localStorage 배열을 React 상태로 구독하는 공통 훅.
 *  - 같은 탭 안의 변경은 기본 'storage' 이벤트로 감지되지 않으므로 커스텀 이벤트로 알린다.
 *  - 다른 탭의 변경은 브라우저 기본 'storage' 이벤트로 감지한다.
 */
const STORAGE_EVENT = 'recordsite:storage';

export function readList(key) {
  try {
    const raw = localStorage.getItem(key);
    const parsed = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function writeList(key, list) {
  try {
    localStorage.setItem(key, JSON.stringify(list));
  } catch {
    // 용량 초과 등은 무시 (즐겨찾기/최근검색은 보조 기능)
  }
  window.dispatchEvent(new CustomEvent(STORAGE_EVENT, { detail: { key } }));
}

export default function useStoredList(key) {
  const [list, setList] = useState(() => readList(key));

  useEffect(() => {
    const refresh = (e) => {
      const changedKey = e.detail?.key ?? e.key;
      if (!changedKey || changedKey === key) setList(readList(key));
    };
    window.addEventListener(STORAGE_EVENT, refresh);
    window.addEventListener('storage', refresh);
    return () => {
      window.removeEventListener(STORAGE_EVENT, refresh);
      window.removeEventListener('storage', refresh);
    };
  }, [key]);

  return list;
}
