import React from 'react';
import { DDRAGON_VERSION } from '../../constants/ddragon';

/*
 * 패치 노트 — 공식 패치 페이지로 연결되는 최근 패치 목록.
 *  - Riot 은 패치 내용을 API 로 주지 않으므로, 버전 번호로 공식 패치 글 URL 을 조립한다.
 *  - 패치 글 URL 의 메이저는 Data Dragon 메이저 + 10 이다(예: 16.7 버전 → patch-26-7-notes).
 *  - DDRAGON_VERSION 만 올리면 목록이 따라 갱신된다(수동 관리 불필요).
 */
const PATCH_NOTES_INDEX_URL = 'https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/';

// 목록에 노출할 최근 패치 개수(현재 패치 포함)
const RECENT_PATCH_COUNT = 6;

// 패치 글 URL 의 메이저 = Data Dragon 메이저 + 10 (2026 시즌: 16.x → 26.x)
const PATCH_URL_MAJOR_OFFSET = 10;

function patchNotesUrl(major, minor) {
  return `https://www.leagueoflegends.com/ko-kr/news/game-updates/league-of-legends-patch-${major + PATCH_URL_MAJOR_OFFSET}-${minor}-notes/`;
}

// 현재 버전에서 같은 메이저 안의 최근 패치 목록을 만든다(현재 패치부터 .1 까지 내림차순).
function buildRecentPatches(version) {
  const [major, minor] = String(version).split('.').map(Number);
  if (!Number.isInteger(major) || !Number.isInteger(minor)) return [];

  const patches = [];
  for (let m = minor; m >= 1 && patches.length < RECENT_PATCH_COUNT; m--) {
    patches.push({ label: `${major}.${m}`, url: patchNotesUrl(major, m) });
  }
  return patches;
}

export default function PatchNotes() {
  const patches = buildRecentPatches(DDRAGON_VERSION);
  const currentLabel = patches[0]?.label ?? String(DDRAGON_VERSION);

  return (
    <section style={cardStyle}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '12px 14px', borderBottom: '1px solid #1a2433',
      }}>
        <h3 style={{ margin: 0, color: '#e2e8f0', fontSize: 14, fontWeight: 700 }}>
          {currentLabel} 패치 노트
        </h3>
        <a href={PATCH_NOTES_INDEX_URL} target="_blank" rel="noreferrer"
          style={{ color: '#5383e8', fontSize: 11, textDecoration: 'none' }}>
          전체 보기 →
        </a>
      </div>

      <ul style={{ listStyle: 'none', margin: 0, padding: '6px 0' }}>
        {patches.map((patch, i) => (
          <li key={patch.label}>
            <a href={patch.url} target="_blank" rel="noreferrer"
              style={{
                display: 'flex', alignItems: 'center', gap: 8,
                padding: '8px 14px', textDecoration: 'none',
                color: i === 0 ? '#dbe2ea' : '#aeb9c7',
                fontWeight: i === 0 ? 700 : 500,
              }}>
              <span style={{ color: '#5383e8', flexShrink: 0 }}>·</span>
              <span style={{ fontSize: 13 }}>{patch.label} 패치 노트</span>
              {i === 0 && (
                <span style={{
                  marginLeft: 'auto', flexShrink: 0, fontSize: 10, fontWeight: 700,
                  color: '#5383e8', background: '#1d2c3a',
                  padding: '2px 7px', borderRadius: 4,
                }}>최신</span>
              )}
            </a>
          </li>
        ))}
      </ul>
    </section>
  );
}

const cardStyle = {
  background: '#151d2e',
  border: '1px solid #1f2a3a',
  borderRadius: 8,
  overflow: 'hidden',
};
