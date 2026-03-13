import React, { useState, useEffect } from 'react';
import apiClient from '../api/client';

const ITEM_IMAGE_BASE =
  'https://ddragon.leagueoflegends.com/cdn/16.5.1/img/item/';

function ItemPage() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

    useEffect(() => {
      apiClient
        .get('/api/items')
        .then((res) => {
          setItems(res.data);
          setLoading(false);
        })
        .catch((err) => {
          console.error(err);
          setError('아이템 목록을 불러오지 못했습니다.');
          setLoading(false);
        });
  }, []);

  if (loading) return <div>로딩 중...</div>
  if (error) return <div>에러 : {error} </div>

  return (
    <div>
        <h1>아이템 목록</h1>
        <ul>
            {items.map((item) => (
                <li
                key={item.itemKey}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  marginBottom: '8px',
                }}
                >
                    {item.image && (
                        <img
                          src={ITEM_IMAGE_BASE + item.image}
                          alt={item.itemName}
                          style={{ width: 32, height: 32, marginRight: '8px' }}
                   />
                )}
                <div>
                    <div>
                        <strong>{item.itemName}</strong>
                    </div>
                    <div style={{ fontSize: 12, color: '#555'}}>
                        기본 가격: {item.goldBase} / 판매 가격: {item.goldSell}
                    </div>
                </div>
            </li>
            ))}
        </ul>
    </div>
  );
}

export default ItemPage;