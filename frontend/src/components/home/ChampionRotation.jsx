import React from 'react';
import { imgChampion } from '../../constants/ddragon';
import useChampionRotation from '../../hooks/useChampionRotation';

/*
 * 무료 로테이션 챔피언 그리드 (백엔드 Riot 프록시 실데이터).
 *  - props.championKeyById/championNameById: HomePage에서 1회 로드해 전달.
 *  - 로딩/에러/빈 상태를 명시적으로 처리한다.
 */
export default function ChampionRotation({ championKeyById = {}, championNameById = {} }) {
  const { freeChampionIds, isLoading, isError } = useChampionRotation();
  const metaReady = Object.keys(championKeyById).length > 0;

  return (
    <section style={cardStyle}>
      <div style={{ padding: '12px 14px', borderBottom: '1px solid #1a2433' }}>
        <h3 style={{ margin: 0, color: '#e2e8f0', fontSize: 14, fontWeight: 700 }}>
          이번 주 무료 로테이션
        </h3>
      </div>

      <div style={{ padding: 14 }}>
        {isError && <StatusText>로테이션 정보를 불러오지 못했습니다.</StatusText>}
        {!isError && (isLoading || !metaReady) && <StatusText>불러오는 중…</StatusText>}
        {!isError && !isLoading && metaReady && freeChampionIds.length === 0 && (
          <StatusText>표시할 로테이션 챔피언이 없습니다.</StatusText>
        )}

        {!isError && !isLoading && metaReady && freeChampionIds.length > 0 && (
          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(56px, 1fr))',
            gap: 12,
          }}>
            {freeChampionIds.map(id => {
              const key = championKeyById[id];
              if (!key) return null;
              return (
                <div key={id} style={{ textAlign: 'center' }}
                  title={championNameById[id] ?? key}>
                  <img src={imgChampion(key)} alt={key} width={48} height={48}
                    style={{ borderRadius: 7, background: '#0d1520', display: 'block', margin: '0 auto' }} />
                  <div style={{
                    marginTop: 4, color: '#8899aa', fontSize: 11,
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                  }}>{championNameById[id] ?? key}</div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}

function StatusText({ children }) {
  return (
    <div style={{ color: '#5a6b7e', fontSize: 12.5, padding: '14px 0', textAlign: 'center' }}>
      {children}
    </div>
  );
}

const cardStyle = {
  background: '#151d2e',
  border: '1px solid #1f2a3a',
  borderRadius: 8,
  overflow: 'hidden',
};
