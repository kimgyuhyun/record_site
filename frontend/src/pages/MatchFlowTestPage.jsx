import React, { useState, useEffect } from 'react';
import apiClient from '../api/client';

/** 다른 페이지와 맞춤; 필요 시 최신 패치로 교체 */
const DDRAGON_VERSION = '16.5.1';
// JSON(매핑용)은 Riot CDN을 유지하고, PNG(아이콘)은 로컬 정적 미러를 사용합니다.
const DATA_CDN = `https://ddragon.leagueoflegends.com/cdn/${DDRAGON_VERSION}`;
const IMG_CDN = `http://localhost:5173/cdn/${DDRAGON_VERSION}`;

const ICON = {
  size: { sm: 28, md: 40, lg: 48 },
  border: { borderRadius: 4, border: '1px solid #ccc', background: '#1a1a2e' },
};

function ItemStrip({ itemIds = [], spell1, spell2, spellMap, size = 24 }) {
  const slotStyle = {
    width: size,
    height: size,
    borderRadius: 4,
    border: '1px solid #333',
    objectFit: 'cover',
    background: '#111',
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 4, flexWrap: 'wrap' }}>
      {spell1 != null && spell1 !== 0 && spellMap[spell1] && (
        <img
          src={`${IMG_CDN}/img/spell/${spellMap[spell1]}`}
          alt=""
          title={`spell ${spell1}`}
          style={slotStyle}
        />
      )}
      {spell2 != null && spell2 !== 0 && spellMap[spell2] && (
        <img
          src={`${IMG_CDN}/img/spell/${spellMap[spell2]}`}
          alt=""
          title={`spell ${spell2}`}
          style={slotStyle}
        />
      )}
      <span style={{ width: 6 }} />
      {itemIds.map((id, i) =>
        id > 0 ? (
          <img
            key={`${id}-${i}`}
            src={`${IMG_CDN}/img/item/${id}.png`}
            alt=""
            title={`item ${id}`}
            style={slotStyle}
          />
        ) : (
          <div
            key={`empty-${i}`}
            style={{ ...slotStyle, background: '#2a2a3a', borderStyle: 'dashed' }}
          />
        )
      )}
    </div>
  );
}

function ChampionFace({ championId, championKeyById, championName, size = ICON.size.md }) {
  const key = championKeyById[championId] || (championName ? championName.replace(/[^a-zA-Z0-9]/g, '') : null);
  const src = key ? `${IMG_CDN}/img/champion/${key}.png` : null;

  if (!src) {
    return (
      <div
        style={{
          width: size,
          height: size,
          borderRadius: ICON.border.borderRadius,
          background: '#333',
          ...ICON.border,
        }}
      />
    );
  }

  return (
    <img
      src={src}
      alt={championName || key}
      title={championName || key}
      style={{
        width: size,
        height: size,
        borderRadius: ICON.border.borderRadius,
        objectFit: 'cover',
        ...ICON.border,
      }}
      onError={(e) => {
        e.target.style.visibility = 'hidden';
      }}
    />
  );
}

function MatchFlowTestPage({ onSummonerLoaded }) {
  const [name, setName] = useState('');
  const [tagLine, setTagLine] = useState('');
  const [region, setRegion] = useState('KR');

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [summoner, setSummoner] = useState(null);
  const [matchList, setMatchList] = useState([]);
  const [expandedMap, setExpandedMap] = useState({});
  const [summaryMap, setSummaryMap] = useState({});
  const [summaryLoadingMap, setSummaryLoadingMap] = useState({});

  const [spellMap, setSpellMap] = useState({});
  const [championKeyById, setChampionKeyById] = useState({});
  const [ddragonReady, setDdragonReady] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadDdragon() {
      try {
        const [sumRes, champRes] = await Promise.all([
          fetch(`${DATA_CDN}/data/en_US/summoner.json`),
          fetch(`${DATA_CDN}/data/en_US/champion.json`),
        ]);
        const sumJson = await sumRes.json();
        const champJson = await champRes.json();

        const spells = {};
        Object.values(sumJson.data).forEach((s) => {
          const sid = Number(s.key);
          if (!Number.isNaN(sid)) spells[sid] = s.image.full;
        });

        const champs = {};
        Object.values(champJson.data).forEach((c) => {
          champs[Number(c.key)] = c.id;
        });

        if (!cancelled) {
          setSpellMap(spells);
          setChampionKeyById(champs);
          setDdragonReady(true);
        }
      } catch (e) {
        console.warn('Data Dragon load failed', e);
        if (!cancelled) setDdragonReady(true);
      }
    }

    loadDdragon();
    return () => {
      cancelled = true;
    };
  }, []);

  const fetchMatchFlow = async () => {
    if (!name.trim()) {
      setError('소환사 이름을 입력하세요.');
      return;
    }

    setLoading(true);
    setError('');
    setSummoner(null);
    setMatchList([]);
    setExpandedMap({});
    setSummaryMap({});
    setSummaryLoadingMap({});

    try {
      const summonerRes = await apiClient.get('/api/summoners', {
        params: {
          name: name.trim(),
          tagLine: tagLine.trim() || undefined,
          region,
        },
      });

      const summonerData = summonerRes.data;
      setSummoner(summonerData);

      // 프로필 페이지에 소환사 데이터 전달
      if (onSummonerLoaded) onSummonerLoaded(summonerData);

      const matchRes = await apiClient.get('/api/matches', {
        params: { puuid: summonerData.puuid },
      });
      setMatchList(matchRes.data?.content || []);
    } catch (err) {
      console.error(err);
      setError('조회 중 오류가 발생했습니다. 백엔드 실행 상태와 입력값을 확인하세요.');
    } finally {
      setLoading(false);
    }
  };

  const [refreshing, setRefreshing] = useState(false);

  // 비동기 갱신: 작업 큐에 투입(즉시 jobId) → DONE 될 때까지 폴링 후 매치 재조회.
  const refreshMatches = async () => {
    if (!summoner?.puuid) return;
    setRefreshing(true);
    setError('');
    try {
      const { data: job } = await apiClient.post('/api/matches/refresh', null, {
        params: { puuid: summoner.puuid },
      });
      const finished = job.status === 'DONE'
        ? job
        : await pollUntilDone(job.jobId);

      const matchRes = await apiClient.get('/api/matches', {
        params: { puuid: summoner.puuid },
      });
      setMatchList(matchRes.data?.content || []);
      if (finished.status === 'FAILED') {
        setError('전적 갱신에 실패했습니다.');
      } else {
        alert(`${finished.done}개의 매치를 처리했습니다.`);
      }
    } catch (err) {
      console.error(err);
      setError('전적 갱신 중 오류가 발생했습니다.');
    } finally {
      setRefreshing(false);
    }
  };

  // jobId 작업이 DONE/FAILED 가 될 때까지 2.5초 간격으로 상태를 조회한다.
  const pollUntilDone = (jobId) => new Promise((resolve, reject) => {
    const timer = setInterval(async () => {
      try {
        const { data } = await apiClient.get(`/api/matches/refresh-jobs/${jobId}`);
        if (data.status === 'DONE' || data.status === 'FAILED') {
          clearInterval(timer);
          resolve(data);
        }
      } catch (err) {
        clearInterval(timer);
        reject(err);
      }
    }, 2500);
  });

  const toggleSummary = async (matchId) => {
    const currentlyExpanded = !!expandedMap[matchId];
    if (currentlyExpanded) {
      setExpandedMap((prev) => ({ ...prev, [matchId]: false }));
      return;
    }

    setExpandedMap((prev) => ({ ...prev, [matchId]: true }));

    if (summaryMap[matchId]) return;

    setSummaryLoadingMap((prev) => ({ ...prev, [matchId]: true }));
    try {
      const res = await apiClient.get(`/api/matches/${matchId}/summary`);
      setSummaryMap((prev) => ({ ...prev, [matchId]: res.data || [] }));
    } catch (err) {
      console.error(err);
      setError(`매치 상세 조회 실패: ${matchId}`);
      setExpandedMap((prev) => ({ ...prev, [matchId]: false }));
    } finally {
      setSummaryLoadingMap((prev) => ({ ...prev, [matchId]: false }));
    }
  };

  const profileIconSrc =
    summoner?.profileIconId != null
      // profileicon은 로컬에 없을 수 있어 Riot에서 가져오도록 유지합니다.
      ? `${DATA_CDN}/img/profileicon/${summoner.profileIconId}.png`
      : null;

  return (
    <div style={{ padding: 16, maxWidth: 1100, margin: '0 auto', fontFamily: 'system-ui, sans-serif' }}>
      <h1 style={{ marginTop: 0 }}>Summoner → Match Flow Test</h1>
      <p style={{ fontSize: 13, color: '#666', marginTop: -8 }}>
        Data Dragon {' '}
        <code>{DDRAGON_VERSION}</code>
        {!ddragonReady && ' (이미지 매핑 로딩 중…)'}
      </p>

      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 }}>
        <input
          placeholder="name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          style={{ padding: 8, minWidth: 200 }}
        />
        <input
          placeholder="tagLine (optional)"
          value={tagLine}
          onChange={(e) => setTagLine(e.target.value)}
          style={{ padding: 8, minWidth: 160 }}
        />
        <select value={region} onChange={(e) => setRegion(e.target.value)} style={{ padding: 8 }}>
          <option value="KR">KR</option>
          <option value="NA">NA</option>
          <option value="EUW">EUW</option>
          <option value="EUNE">EUNE</option>
          <option value="JP">JP</option>
          <option value="BR">BR</option>
          <option value="LAN">LAN</option>
          <option value="LAS">LAS</option>
          <option value="OCE">OCE</option>
          <option value="TR">TR</option>
          <option value="RU">RU</option>
        </select>
        <button onClick={fetchMatchFlow} disabled={loading} style={{ padding: '8px 12px' }}>
          {loading ? '조회 중...' : '조회'}
        </button>
      </div>

      {error && <div style={{ marginBottom: 12, color: 'crimson' }}>{error}</div>}

      {summoner && (
        <div
          style={{
            marginBottom: 16,
            border: '1px solid #ddd',
            padding: 12,
            display: 'flex',
            gap: 16,
            alignItems: 'center',
            background: '#fafafa',
          }}
        >
          {profileIconSrc && (
            <img
              src={profileIconSrc}
              alt="profile"
              style={{
                width: 64,
                height: 64,
                borderRadius: 8,
                border: '2px solid #c89b3c',
                objectFit: 'cover',
              }}
            />
          )}
          <div>
            <strong style={{ fontSize: 18 }}>
              {summoner.name}
              <span style={{ color: '#666', fontWeight: 600 }}>#{summoner.tagLine}</span>
            </strong>
            <div style={{ fontSize: 13, marginTop: 4 }}>level {summoner.level}</div>
            <div style={{ fontSize: 11, color: '#888', wordBreak: 'break-all', marginTop: 4 }}>
              {summoner.puuid}
            </div>
            <button
              onClick={refreshMatches}
              disabled={refreshing}
              style={{ marginTop: 8, padding: '4px 10px', fontSize: 12 }}
            >
              {refreshing ? '갱신 중...' : '전적 갱신'}
            </button>
          </div>
        </div>
      )}

      <h2>Match List ({matchList.length})</h2>
      {matchList.length === 0 && !loading && <div>매치 데이터가 없습니다.</div>}

      {matchList.map((match) => {
        const isExpanded = !!expandedMap[match.matchId];
        const summaryLoading = !!summaryLoadingMap[match.matchId];
        const summaryRows = summaryMap[match.matchId] || [];
        const items = [
          match.myItem0,
          match.myItem1,
          match.myItem2,
          match.myItem3,
          match.myItem4,
          match.myItem5,
          match.myItem6,
        ];

        return (
          <div key={match.matchId} style={{ border: '1px solid #ddd', marginBottom: 12, overflow: 'hidden' }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'stretch',
                padding: 12,
                background: match.myWin ? 'linear-gradient(90deg,#28359322,#fafafa)' : 'linear-gradient(90deg,#b71c1c18,#fafafa)',
                gap: 12,
              }}
            >
              <div style={{ display: 'flex', gap: 12, alignItems: 'center', flex: 1, minWidth: 0 }}>
                <div style={{ position: 'relative' }}>
                  <ChampionFace
                    championId={match.myChampionId}
                    championKeyById={championKeyById}
                    championName={match.myChampionName}
                    size={52}
                  />
                  <span
                    style={{
                      position: 'absolute',
                      right: -4,
                      bottom: -4,
                      background: '#111',
                      color: '#fff',
                      fontSize: 11,
                      padding: '0 4px',
                      borderRadius: 4,
                      border: '1px solid #444',
                    }}
                  >
                    {match.myChampionLevel}
                  </span>
                </div>
                <div style={{ minWidth: 0 }}>
                  <div style={{ fontWeight: 700 }}>
                    {match.myWin ? (
                      <span style={{ color: '#3949ab' }}>승리</span>
                    ) : (
                      <span style={{ color: '#c62828' }}>패배</span>
                    )}{' '}
                    <span style={{ color: '#333', fontSize: 12, fontWeight: 500 }}>{match.matchId}</span>
                  </div>
                  <div style={{ fontSize: 14, marginTop: 2 }}>
                    <strong>{match.myChampionName}</strong>{' '}
                    <span style={{ color: '#555' }}>
                      {match.myKills} / {match.myDeaths} / {match.myAssists}
                    </span>
                  </div>
                  <div style={{ marginTop: 6 }}>
                    <ItemStrip
                      itemIds={items}
                      spell1={match.mySpell1}
                      spell2={match.mySpell2}
                      spellMap={spellMap}
                      size={26}
                    />
                  </div>
                </div>
              </div>

              {match.participants && match.participants.length > 0 && (
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 2,
                    flexShrink: 0,
                    paddingLeft: 8,
                    borderLeft: '1px solid #e0e0e0',
                  }}
                  title="해당 판 챔피언"
                >
                  {match.participants.map((p) => (
                    <ChampionFace
                      key={p.puuid + p.participantId}
                      championId={p.championId}
                      championKeyById={championKeyById}
                      championName={p.championName}
                      size={28}
                    />
                  ))}
                </div>
              )}

              <button
                onClick={() => toggleSummary(match.matchId)}
                style={{ alignSelf: 'center', flexShrink: 0 }}
              >
                {isExpanded ? '접기' : '상세'}
              </button>
            </div>

            {isExpanded && (
              <div style={{ padding: 12, background: '#fff' }}>
                {summaryLoading ? (
                  <div>상세 조회 중...</div>
                ) : (
                  <div style={{ overflowX: 'auto' }}>
                    <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: 520 }}>
                      <thead>
                        <tr style={{ fontSize: 12, color: '#666' }}>
                          <th style={{ textAlign: 'left', borderBottom: '1px solid #ddd', padding: 8 }}>플레이어</th>
                          <th style={{ textAlign: 'left', borderBottom: '1px solid #ddd', padding: 8 }}>빌드</th>
                          <th style={{ textAlign: 'left', borderBottom: '1px solid #ddd', padding: 8 }}>K/D/A</th>
                          <th style={{ textAlign: 'left', borderBottom: '1px solid #ddd', padding: 8 }}>승패</th>
                          <th style={{ textAlign: 'right', borderBottom: '1px solid #ddd', padding: 8 }}>챔딜</th>
                        </tr>
                      </thead>
                      <tbody>
                        {summaryRows.map((row) => {
                          const rowItems = [
                            row.item0,
                            row.item1,
                            row.item2,
                            row.item3,
                            row.item4,
                            row.item5,
                            row.item6,
                          ];
                          const rowBg = row.win ? '#e8eaf622' : '#ffebee22';
                          return (
                            <tr
                              key={`${row.puuid}-${row.championId}-${row.kills}-${row.deaths}-${row.teamId}`}
                              style={{ background: rowBg }}
                            >
                              <td style={{ padding: 8, verticalAlign: 'middle' }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                                  <ChampionFace
                                    championId={row.championId}
                                    championKeyById={championKeyById}
                                    championName={row.championName}
                                    size={40}
                                  />
                                  <div>
                                    <div style={{ fontWeight: 600, fontSize: 13 }}>
                                      {row.gameName}
                                      <span style={{ color: '#888' }}>#{row.tagLine}</span>
                                    </div>
                                    <div style={{ fontSize: 12, color: '#555' }}>
                                      {row.championName} · Lv.{row.championLevel}
                                    </div>
                                  </div>
                                </div>
                              </td>
                              <td style={{ padding: 8, verticalAlign: 'middle' }}>
                                <ItemStrip
                                  itemIds={rowItems}
                                  spell1={row.spell1}
                                  spell2={row.spell2}
                                  spellMap={spellMap}
                                  size={22}
                                />
                              </td>
                              <td style={{ padding: 8, verticalAlign: 'middle', fontSize: 13 }}>
                                {row.kills}/{row.deaths}/{row.assists}
                              </td>
                              <td style={{ padding: 8, verticalAlign: 'middle' }}>
                                {row.win ? (
                                  <span style={{ color: '#3949ab', fontWeight: 600 }}>WIN</span>
                                ) : (
                                  <span style={{ color: '#c62828', fontWeight: 600 }}>LOSE</span>
                                )}
                              </td>
                              <td style={{ padding: 8, verticalAlign: 'middle', textAlign: 'right', fontSize: 13 }}>
                                {row.totalDamageDealtToChampions?.toLocaleString?.() ?? row.totalDamageDealtToChampions}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

export default MatchFlowTestPage;
