import React from 'react';
import { DDRAGON_VERSION } from '../../constants/ddragon';

/*
 * 패치 노트 (큐레이션 하이라이트 + 공식 링크).
 *  - 제목 버전은 Data Dragon 버전 상수에서 메이저.마이너만 사용.
 *  - 항목은 편집형 요약이며, 전체 내용은 공식 패치노트로 연결한다.
 */
const PATCH_NOTES_URL = 'https://www.leagueoflegends.com/ko-kr/news/tags/patch-notes/';

const HIGHLIGHTS = [
  { tag: '챔피언', text: '암살자 계열 챔피언 기본 방어력 소폭 상향' },
  { tag: '아이템', text: '신화 아이템 재조정 — 마나 회복 효율 변경' },
  { tag: '시스템', text: '협곡 시야 점수 계산 방식 개선' },
  { tag: '밸런스', text: '저티어 픽률 상위 정글러 클리어 속도 너프' },
];

function patchLabel(version) {
  const [major, minor] = String(version).split('.');
  return `${major}.${minor}`;
}

export default function PatchNotes() {
  return (
    <section style={cardStyle}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '12px 14px', borderBottom: '1px solid #1a2433',
      }}>
        <h3 style={{ margin: 0, color: '#e2e8f0', fontSize: 14, fontWeight: 700 }}>
          {patchLabel(DDRAGON_VERSION)} 패치 노트
        </h3>
        <a href={PATCH_NOTES_URL} target="_blank" rel="noreferrer"
          style={{ color: '#5383e8', fontSize: 11, textDecoration: 'none' }}>
          전체 보기 →
        </a>
      </div>

      <ul style={{ listStyle: 'none', margin: 0, padding: '6px 0' }}>
        {HIGHLIGHTS.map((h, i) => (
          <li key={i} style={{
            display: 'flex', alignItems: 'center', gap: 10, padding: '7px 14px',
          }}>
            <span style={{
              flexShrink: 0, fontSize: 10, fontWeight: 700, color: '#5383e8',
              background: '#1d2c3a', padding: '3px 7px', borderRadius: 4,
              minWidth: 44, textAlign: 'center',
            }}>{h.tag}</span>
            <span style={{
              color: '#aeb9c7', fontSize: 12.5, lineHeight: 1.4,
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
            }}>{h.text}</span>
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
