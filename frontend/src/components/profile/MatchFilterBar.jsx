import React, { useState } from 'react';
import {
  QUEUE_FILTERS, QUEUE_TYPE_OPTIONS, ALL_QUEUE_FILTERS, DROPDOWN_FILTER_KEYS,
} from '../../constants/queueFilters';

/* ───────── 사이트 공통 테마 토큰 ───────── */
const C = {
  panel:       '#111c27',
  panelBorder: '#2a3a4a',
  divider:     '#1a2535',
  gold:        '#c89b3c',
  text:        '#e8e0d0',
  sub:         '#6b7a8d',
};

function TabButton({ label, active, onClick, children }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 5,
        padding: '8px 14px', background: 'transparent', border: 'none',
        color: active ? C.gold : C.sub,
        fontWeight: active ? 700 : 500, fontSize: 13, cursor: 'pointer',
        whiteSpace: 'nowrap', borderRadius: 6, transition: 'color 0.15s',
      }}
      onMouseEnter={e => { if (!active) e.currentTarget.style.color = C.text; }}
      onMouseLeave={e => { if (!active) e.currentTarget.style.color = C.sub; }}
    >
      {label}{children}
    </button>
  );
}

export default function MatchFilterBar({ value = 'ALL', onChange }) {
  const [open, setOpen] = useState(false);
  const dropdownActive = DROPDOWN_FILTER_KEYS.has(value);
  const dropdownLabel = dropdownActive ? ALL_QUEUE_FILTERS[value].label : '큐 타입';

  const pick = (key) => { onChange?.(key); setOpen(false); };

  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 2,
      background: C.panel, border: `1px solid ${C.panelBorder}`,
      borderRadius: 8, padding: '2px 6px',
    }}>
      {['ALL', 'SOLO', 'FLEX', 'ARAM'].map(key => (
        <TabButton key={key} label={QUEUE_FILTERS[key].label}
          active={value === key} onClick={() => pick(key)} />
      ))}

      {/* 큐 타입 드롭다운 */}
      <div style={{ position: 'relative', marginLeft: 'auto' }}>
        <TabButton label={dropdownLabel} active={dropdownActive}
          onClick={() => setOpen(o => !o)}>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none"
            style={{ transform: open ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s' }}>
            <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="2.5"
              strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </TabButton>

        {open && (
          <>
            {/* 바깥 클릭 닫기용 오버레이 */}
            <div onClick={() => setOpen(false)}
              style={{ position: 'fixed', inset: 0, zIndex: 30 }} />
            <div style={{
              position: 'absolute', top: 'calc(100% + 6px)', right: 0, zIndex: 31,
              background: C.panel, border: `1px solid ${C.panelBorder}`, borderRadius: 8,
              padding: 4, minWidth: 150, boxShadow: '0 8px 24px rgba(0,0,0,0.45)',
            }}>
              {QUEUE_TYPE_OPTIONS.map(opt => (
                <button key={opt.key} onClick={() => pick(opt.key)}
                  style={{
                    display: 'block', width: '100%', textAlign: 'left',
                    padding: '9px 12px', background: value === opt.key ? C.divider : 'transparent',
                    border: 'none', borderRadius: 6,
                    color: value === opt.key ? C.gold : C.text,
                    fontSize: 13, fontWeight: value === opt.key ? 700 : 500, cursor: 'pointer',
                  }}
                  onMouseEnter={e => { e.currentTarget.style.background = C.divider; }}
                  onMouseLeave={e => { e.currentTarget.style.background = value === opt.key ? C.divider : 'transparent'; }}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
}
