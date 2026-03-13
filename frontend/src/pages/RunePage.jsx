import React, { useState, useEffect, useMemo } from 'react';
import apiClient from '../api/client';

const RUNE_IMAGE_BASE =
  'https://ddragon.leagueoflegends.com/cdn/img/';

function RunePage() {
  const [paths, setPaths] = useState([]);
  const [runes, setRunes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    Promise.all([
      apiClient.get('/api/runePaths'),
      apiClient.get('/api/runePaths/runes'),
    ])
      .then(([pathsRes, runesRes]) => {
        setPaths(pathsRes.data);
        setRunes(runesRes.data);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setError('룬 정보를 불러오지 못했습니다.');
        setLoading(false);
      });
  }, []);

    // 룬 경로와 그에 해당하는 룬들을 모아 둔 사전
    const runesByPath = useMemo(() => {
      const map = {};
      for (const rune of runes) {
        const key = rune.pathKey;
        if (!map[key]) {
          map[key] = [];
        }
        map[key].push(rune);
      }
      return map;
    }, [runes]);

  if (loading) return <div>로딩중...</div>;
  if (error) return <div>에러: {error}</div>;


  return (
    <div>
      <h1>룬 경로 및 룬 목록</h1>
      {paths.map((path) => {
        const runes = runesByPath[path.pathKey] || [];
        return (
          <div
            key={path.pathKey}
            style={{
              border: '1px solid #ddd',
              padding: '12px',
              marginBottom: '16px',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                marginBottom: 8,
              }}
            >
              {path.image && (
                <img
                  src={RUNE_IMAGE_BASE + path.image}
                  alt={path.runePathNameKor}
                  style={{ width: 32, height: 32, marginRight: 8 }}
                />
              )}
              <strong>
                {path.runePathNameKor} ({path.runePathNameEn})
              </strong>
            </div>

            <ul>
              {runes.map((rune) => (
                <li
                  key={rune.runeKey}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    marginBottom: 4,
                  }}
                >
                  {rune.image && (
                    <img
                      src={RUNE_IMAGE_BASE + rune.image}
                      alt={rune.runeNameKor}
                      style={{ width: 24, height: 24, marginRight: 8 }}
                    />
                  )}
                  <span>
                    {rune.runeNameKor} ({rune.runeNameEn})
                  </span>
                </li>
              ))}
            </ul>
          </div>
        );
      })}
    </div>
  );
}

export default RunePage;