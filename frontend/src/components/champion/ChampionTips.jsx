import React, { useEffect, useState } from 'react';
import {
  getChampionTips, createChampionTip, voteChampionTip, reportChampionTip,
} from '../../api/championTip';

/*
 * 챔피언 운영 팁 게시판(코멘트) — 챔피언 상세 페이지 하단.
 * op.gg 챔피언 팁처럼 로그인 없이 닉네임으로 한 줄 팁을 남기고 추천/비추천·신고가 달린다. 대댓글은 없다.
 * 중복 투표는 localStorage 로 브라우저 단위로만 막는다(계정이 없어 완벽한 차단은 아님).
 */

const C = {
  card: '#2a2a30', head: '#212126', box: '#303037',
  line: '#3a3a43', text: '#e6e6ea', sub: '#9a9aa3', muted: '#6c6c75', accent: '#5b9bd5',
};
const PAGE_SIZE = 20;
const NICK_MAX = 20;
const CONTENT_MAX = 500;
const VOTE_KEY = 'championTipVotes';

const loadVoted = () => {
  try { return JSON.parse(localStorage.getItem(VOTE_KEY)) || {}; } catch { return {}; }
};
const saveVoted = (v) => {
  try { localStorage.setItem(VOTE_KEY, JSON.stringify(v)); } catch { /* 저장 실패는 무시 */ }
};

const timeAgo = (iso) => {
  const diff = Math.max(0, Date.now() - new Date(iso).getTime());
  const m = Math.floor(diff / 60000);
  if (m < 1) return '방금 전';
  if (m < 60) return `${m}분 전`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}시간 전`;
  const d = Math.floor(h / 24);
  if (d < 7) return `${d}일 전`;
  if (d < 30) return `${Math.floor(d / 7)}주 전`;
  return `${Math.floor(d / 30)}개월 전`;
};

export default function ChampionTips({ championId, championName }) {
  const [sort, setSort] = useState('popular');
  const [tips, setTips] = useState([]);
  const [total, setTotal] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  const [nickname, setNickname] = useState('');
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [voted, setVoted] = useState(loadVoted);

  // 챔피언/정렬 바뀌면 첫 페이지를 새로 불러온다.
  useEffect(() => {
    if (!championId) return undefined;
    let cancelled = false;
    setLoading(true);
    getChampionTips(championId, { sort, page: 0, size: PAGE_SIZE })
      .then(res => {
        if (cancelled) return;
        setTips(res.data.tips);
        setTotal(res.data.totalCount);
        setHasNext(res.data.hasNext);
        setPage(0);
      })
      .catch(() => { if (!cancelled) { setTips([]); setTotal(0); setHasNext(false); } })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [championId, sort]);

  const reloadFirstPage = async () => {
    const res = await getChampionTips(championId, { sort, page: 0, size: PAGE_SIZE });
    setTips(res.data.tips);
    setTotal(res.data.totalCount);
    setHasNext(res.data.hasNext);
    setPage(0);
  };

  const loadMore = async () => {
    const next = page + 1;
    const res = await getChampionTips(championId, { sort, page: next, size: PAGE_SIZE });
    setTips(prev => [...prev, ...res.data.tips]);
    setHasNext(res.data.hasNext);
    setPage(next);
  };

  const submit = async () => {
    if (submitting) return;
    const n = nickname.trim();
    const c = content.trim();
    if (!n) { alert('닉네임을 입력하세요.'); return; }
    if (!c) { alert('팁 내용을 입력하세요.'); return; }
    setSubmitting(true);
    try {
      await createChampionTip({ championId, nickname: n, content: c });
      setContent('');
      await reloadFirstPage();
    } catch (e) {
      alert(e?.response?.data?.message || '등록에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const vote = async (tip, direction) => {
    if (voted[tip.id]) return; // 이미 투표한 팁
    const delta = direction === 'UP' ? 1 : -1;
    setTips(prev => prev.map(t => t.id === tip.id
      ? {
          ...t,
          upvotes: t.upvotes + (direction === 'UP' ? 1 : 0),
          downvotes: t.downvotes + (direction === 'DOWN' ? 1 : 0),
          score: t.score + delta,
        }
      : t));
    const nextVoted = { ...voted, [tip.id]: direction };
    setVoted(nextVoted);
    saveVoted(nextVoted);
    try { await voteChampionTip(tip.id, direction); } catch { /* 로컬 반영 유지 */ }
  };

  const report = async (tip) => {
    if (!window.confirm('이 팁을 신고할까요?')) return;
    try { await reportChampionTip(tip.id); alert('신고했습니다.'); }
    catch { alert('신고에 실패했습니다.'); }
  };

  return (
    <div style={{ background: C.card, border: `1px solid ${C.line}`, borderRadius: 8, overflow: 'hidden' }}>
      {/* 헤더 */}
      <div style={{ padding: '11px 16px', background: C.head, borderBottom: `1px solid ${C.line}` }}>
        <span style={{ color: C.text, fontSize: 14, fontWeight: 800 }}>{championName} 운영 팁</span>
        <span style={{ color: C.muted, fontSize: 13, fontWeight: 700, marginLeft: 6 }}>({total}개)</span>
      </div>

      <div style={{ padding: 16 }}>
        {/* 작성 박스 */}
        <div style={{ background: C.box, border: `1px solid ${C.line}`, borderRadius: 8, padding: 12 }}>
          <div style={{ color: C.muted, fontSize: 11.5, marginBottom: 8, lineHeight: 1.5 }}>
            나만의 {championName} 플레이 팁을 남겨주세요. 비속어·음란성 글은 예고 없이 삭제될 수 있습니다.
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'stretch' }}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 8 }}>
              <input
                value={nickname}
                onChange={e => setNickname(e.target.value)}
                maxLength={NICK_MAX}
                placeholder="닉네임"
                style={inputStyle}
              />
              <textarea
                value={content}
                onChange={e => setContent(e.target.value)}
                maxLength={CONTENT_MAX}
                placeholder="팁을 입력하세요"
                rows={2}
                style={{ ...inputStyle, resize: 'vertical', minHeight: 40, lineHeight: 1.5, paddingTop: 8 }}
              />
            </div>
            <button
              onClick={submit}
              disabled={submitting}
              style={{
                width: 80, flexShrink: 0, background: C.accent, color: '#fff', border: 'none',
                borderRadius: 8, fontSize: 14, fontWeight: 800, cursor: submitting ? 'default' : 'pointer',
                opacity: submitting ? 0.6 : 1, fontFamily: 'inherit',
              }}
            >등록</button>
          </div>
        </div>

        {/* 정렬 탭 */}
        <div style={{ display: 'flex', gap: 4, margin: '14px 0 8px' }}>
          {[{ k: 'popular', l: '인기순' }, { k: 'recent', l: '최신순' }].map(t => {
            const active = sort === t.k;
            return (
              <button key={t.k} onClick={() => setSort(t.k)} style={{
                background: active ? C.accent : 'transparent',
                border: `1px solid ${active ? C.accent : C.line}`,
                color: active ? '#fff' : C.sub,
                fontSize: 12.5, fontWeight: 700, padding: '5px 12px', borderRadius: 6,
                cursor: 'pointer', fontFamily: 'inherit',
              }}>{t.l}</button>
            );
          })}
        </div>

        {/* 목록 */}
        {loading && tips.length === 0 && <Empty>불러오는 중…</Empty>}
        {!loading && tips.length === 0 && <Empty>아직 등록된 팁이 없어요. 첫 팁을 남겨보세요!</Empty>}

        {tips.map(tip => (
          <div key={tip.id} style={{ display: 'flex', gap: 12, padding: '12px 2px',
            borderTop: `1px solid ${C.line}` }}>
            {/* 투표 */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', width: 28, flexShrink: 0 }}>
              <VoteArrow dir="up" active={voted[tip.id] === 'UP'} disabled={!!voted[tip.id]}
                onClick={() => vote(tip, 'UP')} />
              <span style={{ color: tip.score > 0 ? '#5aa9e6' : tip.score < 0 ? '#e06a6a' : C.sub,
                fontSize: 13, fontWeight: 800, margin: '1px 0' }}>{tip.score}</span>
              <VoteArrow dir="down" active={voted[tip.id] === 'DOWN'} disabled={!!voted[tip.id]}
                onClick={() => vote(tip, 'DOWN')} />
            </div>
            {/* 본문 */}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap', marginBottom: 4 }}>
                <span style={{ color: C.text, fontSize: 12.5, fontWeight: 700 }}>{tip.nickname}</span>
                <Dot /><span style={{ color: C.muted, fontSize: 11.5 }}>{tip.language}</span>
                <Dot /><span style={{ color: C.muted, fontSize: 11.5 }}>{timeAgo(tip.createdAt)}</span>
                {tip.patchVersion && (
                  <><Dot /><span style={{ color: C.muted, fontSize: 11.5 }}>버전 {tip.patchVersion}</span></>
                )}
                <button onClick={() => report(tip)} style={{
                  marginLeft: 'auto', background: 'transparent', border: `1px solid ${C.line}`,
                  color: C.muted, fontSize: 11, padding: '2px 8px', borderRadius: 5,
                  cursor: 'pointer', fontFamily: 'inherit',
                }}>신고</button>
              </div>
              <div style={{ color: '#d3d3da', fontSize: 13, lineHeight: 1.55, whiteSpace: 'pre-wrap',
                wordBreak: 'break-word' }}>{tip.content}</div>
            </div>
          </div>
        ))}

        {hasNext && (
          <button onClick={loadMore} style={{
            width: '100%', marginTop: 12, background: C.box, border: `1px solid ${C.line}`,
            color: C.sub, fontSize: 12.5, fontWeight: 700, padding: '9px 0', borderRadius: 8,
            cursor: 'pointer', fontFamily: 'inherit',
          }}>더 보기</button>
        )}
      </div>
    </div>
  );
}

function VoteArrow({ dir, active, disabled, onClick }) {
  return (
    <button onClick={onClick} disabled={disabled} style={{
      background: 'none', border: 'none', padding: 0, lineHeight: 1,
      cursor: disabled ? 'default' : 'pointer',
      color: active ? '#5b9bd5' : '#5a5a63',
      fontSize: 12,
    }}>{dir === 'up' ? '▲' : '▼'}</button>
  );
}

function Dot() {
  return <span style={{ color: '#4a4a52', fontSize: 10 }}>·</span>;
}

function Empty({ children }) {
  return <div style={{ color: '#6c6c75', fontSize: 12.5, padding: '28px 0', textAlign: 'center' }}>{children}</div>;
}

const inputStyle = {
  width: '100%', boxSizing: 'border-box',
  background: '#26262c', border: '1px solid #3a3a43', borderRadius: 6,
  color: '#e6e6ea', fontSize: 13, padding: '0 10px', height: 34, outline: 'none',
  fontFamily: 'inherit',
};
