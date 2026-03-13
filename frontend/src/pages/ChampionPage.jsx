import React, { useState, useEffect } from 'react';
import apiClient from '../api/client';

function ChampionPage() {
    const [champions, setChampions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const CHAMP_IMAGE_BASE =
    'https://ddragon.leagueoflegends.com/cdn/16.5.1/img/champion/';

    useEffect(() => {
        apiClient
        .get('/api/champions')
        .then((res) => {
            setChampions(res.data)
            setLoading(false);
        })
        .catch(err => {
            console.error(err);
            setError("챔피언 목록을 불러오지 못했습니다.");
            setLoading(false);
    });
}, []);

if (loading) return <div>로딩 중...</div>
if (error) return <div>에러: {error}</div>

return (
    <div>
        <h1>챔피언 목록</h1>
        <ul>
            {champions.map((champ) => (
                <li key={champ.championId}>
                    <img
                      src={CHAMP_IMAGE_BASE + champ.imageUrl}
                      alt={champ.nameKor}
                      style={{ width: 48, height: 48, marginRight: 8}}
                    />
                    {champ.championId} / {champ.nameKor} / {champ.nameEn}
                </li>
                ))}
         </ul>
    </div>
    );
}

export default ChampionPage;