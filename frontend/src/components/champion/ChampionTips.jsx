import React, { useEffect, useState } from 'react';
import {
  getChampionTips, createChampionTip, voteChampionTip, reportChampionTip,
  deleteChampionTip, updateChampionTip,
} from '../../api/championTip';
import { DDRAGON_VERSION } from '../../constants/ddragon';
import { timeAgo } from '../../utils/datetime';

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

// "16.12.1" → "16.12" — 작성 시 각인되는 현재 패치와 같은 표기(현재 버전 필터에 사용).
const CURRENT_PATCH = DDRAGON_VERSION.split('.').slice(0, 2).join('.');

// 브라우저 언어 → 표시 라벨. "내 언어만 보기"가 이 값으로 필터하고, 작성 시에도 이 값을 언어로 저장한다.
const langLabel = (code) => {
  const c = (code || '').toLowerCase();
  if (c.startsWith('ko')) return '한국어';
  if (c.startsWith('en')) return 'English';
  if (c.startsWith('ja')) return '日本語';
  if (c.startsWith('zh')) return '中文';
  return code || '한국어';
};
const MY_LANG = langLabel(typeof navigator !== 'undefined' ? navigator.language : 'ko');

const loadVoted = () => {
  try { return JSON.parse(localStorage.getItem(VOTE_KEY)) || {}; } catch { return {}; }
};
const saveVoted = (v) => {
  try { localStorage.setItem(VOTE_KEY, JSON.stringify(v)); } catch { /* 저장 실패는 무시 */ }
};

export default function ChampionTips({ championId, championName }) {
  const [sort, setSort] = useState('popular');
  const [tips, setTips] = useState([]);
  const [total, setTotal] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(false);

  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [content, setContent] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const [voted, setVoted] = useState(loadVoted);

  const [onlyMyLang, setOnlyMyLang] = useState(false);
  const [onlyCurrentVersion, setOnlyCurrentVersion] = useState(false);

  const [editingId, setEditingId] = useState(null);
  const [editContent, setEditContent] = useState('');
  const [editPassword, setEditPassword] = useState('');

  // 토글 상태를 반영한 조회 파라미터(꺼진 필터는 undefined 라 서버에서 무시된다).
  const buildParams = (pageNum) => ({
    sort, page: pageNum, size: PAGE_SIZE,
    language: onlyMyLang ? MY_LANG : undefined,
    patchVersion: onlyCurrentVersion ? CURRENT_PATCH : undefined,
  });

  // 챔피언/정렬/필터가 바뀌면 첫 페이지를 새로 불러온다.
  useEffect(() => {
    if (!championId) return undefined;
    let cancelled = false;
    setLoading(true);
    getChampionTips(championId, buildParams(0))
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [championId, sort, onlyMyLang, onlyCurrentVersion]);

  const reloadFirstPage = async () => {
    const res = await getChampionTips(championId, buildParams(0));
    setTips(res.data.tips);
    setTotal(res.data.totalCount);
    setHasNext(res.data.hasNext);
    setPage(0);
  };

  const loadMore = async () => {
    const next = page + 1;
    const res = await getChampionTips(championId, buildParams(next));
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
    if (password.length < 4) { alert('비밀번호는 4자 이상이어야 합니다.'); return; }
    setSubmitting(true);
    try {
      await createChampionTip({ championId, nickname: n, content: c, password, language: MY_LANG });
      setContent('');
      setPassword('');
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

  const remove = async (tip) => {
    const pw = window.prompt('삭제하려면 작성 시 정한 비밀번호를 입력하세요.');
    if (pw == null) return; // 취소
    try {
      await deleteChampionTip(tip.id, pw);
      setTips(prev => prev.filter(t => t.id !== tip.id));
      setTotal(t => Math.max(0, t - 1));
    } catch (e) {
      alert(e?.response?.status === 403 ? '비밀번호가 일치하지 않습니다.' : '삭제에 실패했습니다.');
    }
  };

  const startEdit = (tip) => {
    setEditingId(tip.id);
    setEditContent(tip.content);
    setEditPassword('');
  };
  const cancelEdit = () => {
    setEditingId(null);
    setEditContent('');
    setEditPassword('');
  };
  const saveEdit = async (tip) => {
    const c = editContent.trim();
    if (!c) { alert('내용을 입력하세요.'); return; }
    if (!editPassword) { alert('비밀번호를 입력하세요.'); return; }
    try {
      const res = await updateChampionTip(tip.id, { password: editPassword, content: c });
      setTips(prev => prev.map(t => (t.id === tip.id ? { ...t, content: res.data.content } : t)));
      cancelEdit();
    } catch (e) {
      alert(e?.response?.status === 403 ? '비밀번호가 일치하지 않습니다.' : '수정에 실패했습니다.');
    }
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
              <div style={{ display: 'flex', gap: 8 }}>
                <input
                  value={nickname}
                  onChange={e => setNickname(e.target.value)}
                  maxLength={NICK_MAX}
                  placeholder="닉네임"
                  style={{ ...inputStyle, flex: 1 }}
                />
                <input
                  type="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  maxLength={30}
                  placeholder="비밀번호"
                  style={{ ...inputStyle, width: 120 }}
                />
              </div>
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

        {/* 정렬 탭 + 필터 토글 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, margin: '14px 0 8px', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', gap: 4 }}>
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
          <div style={{ marginLeft: 'auto', display: 'flex', gap: 16 }}>
            <Toggle label="내 언어만" on={onlyMyLang} onToggle={() => setOnlyMyLang(v => !v)} />
            <Toggle label={`${CURRENT_PATCH} 버전만`} on={onlyCurrentVersion}
              onToggle={() => setOnlyCurrentVersion(v => !v)} />
          </div>
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
                <span style={{ marginLeft: 'auto', display: 'inline-flex', gap: 6 }}>
                  <button onClick={() => startEdit(tip)} style={tipActionBtn}>수정</button>
                  <button onClick={() => remove(tip)} style={tipActionBtn}>삭제</button>
                  <button onClick={() => report(tip)} style={tipActionBtn}>신고</button>
                </span>
              </div>
              {editingId === tip.id ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                  <textarea value={editContent} onChange={e => setEditContent(e.target.value)}
                    maxLength={CONTENT_MAX} rows={2}
                    style={{ ...inputStyle, width: '100%', resize: 'vertical', minHeight: 40,
                      lineHeight: 1.5, paddingTop: 8 }} />
                  <div style={{ display: 'flex', gap: 6 }}>
                    <input type="password" value={editPassword} onChange={e => setEditPassword(e.target.value)}
                      maxLength={30} placeholder="비밀번호" style={{ ...inputStyle, width: 120 }} />
                    <button onClick={() => saveEdit(tip)}
                      style={{ ...tipActionBtn, background: C.accent, color: '#fff', borderColor: C.accent }}>저장</button>
                    <button onClick={cancelEdit} style={tipActionBtn}>취소</button>
                  </div>
                </div>
              ) : (
                <div style={{ color: '#d3d3da', fontSize: 13, lineHeight: 1.55, whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word' }}>{tip.content}</div>
              )}
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

function Toggle({ label, on, onToggle }) {
  return (
    <button onClick={onToggle} style={{
      display: 'inline-flex', alignItems: 'center', gap: 6, background: 'none', border: 'none',
      cursor: 'pointer', fontFamily: 'inherit', padding: 0,
    }}>
      <span style={{ color: on ? '#e6e6ea' : '#6c6c75', fontSize: 11.5, fontWeight: 600 }}>{label}</span>
      <span style={{ width: 30, height: 16, borderRadius: 999, background: on ? '#5b9bd5' : '#3a3a43',
        position: 'relative', flexShrink: 0, transition: 'background 0.15s' }}>
        <span style={{ position: 'absolute', top: 2, left: on ? 16 : 2, width: 12, height: 12,
          borderRadius: '50%', background: '#fff', transition: 'left 0.15s' }} />
      </span>
    </button>
  );
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

const tipActionBtn = {
  background: 'transparent', border: '1px solid #3a3a43',
  color: '#6c6c75', fontSize: 11, padding: '2px 8px', borderRadius: 5,
  cursor: 'pointer', fontFamily: 'inherit',
};
