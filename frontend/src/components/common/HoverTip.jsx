import React, { useState, useRef } from 'react';
import { createPortal } from 'react-dom';

/*
 * 공용 호버 툴팁 (검은 배경 + 흰 글씨, 라벨 전용).
 *  - 전적 상세의 Tooltip과 동일한 룩을 메인 페이지 등 다른 화면에서도 쓰기 위한 공용 버전.
 *  - position:fixed + 포털(body)로 띄워 부모 overflow:hidden에 잘리지 않게 한다.
 *  - label이 없으면 children만 그대로 렌더.
 */
export default function HoverTip({ label, children }) {
  const [pos, setPos] = useState(null); // 뷰포트 기준 {x: 대상 가로중앙, y: 대상 상단}
  const ref = useRef(null);
  if (!label) return children;

  const show = () => {
    const r = ref.current?.getBoundingClientRect();
    if (r) setPos({ x: r.left + r.width / 2, y: r.top });
  };

  return (
    <span
      ref={ref}
      style={{ display: 'inline-flex', flexShrink: 0 }}
      onMouseEnter={show}
      onMouseLeave={() => setPos(null)}
    >
      {children}
      {pos && createPortal(
        <span style={{
          position: 'fixed', left: pos.x, top: pos.y - 6,
          transform: 'translate(-50%, -100%)',
          background: '#000', color: '#fff',
          fontSize: 11, lineHeight: '15px',
          padding: '6px 9px', borderRadius: 5,
          whiteSpace: 'nowrap', fontWeight: 700,
          pointerEvents: 'none', zIndex: 9999,
          boxShadow: '0 4px 14px rgba(0,0,0,0.5)',
        }}>{label}</span>,
        document.body,
      )}
    </span>
  );
}
