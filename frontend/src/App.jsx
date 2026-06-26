import React, { useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import PlayerPage from './pages/PlayerPage';
import SearchResultPage from './pages/SearchResultPage';
import ChampionAnalysisPage from './pages/ChampionAnalysisPage';
import ChampionPage from './pages/ChampionPage';
import RankingPage from './pages/RankingPage';

function App() {
  const [region, setRegion] = useState('KR');

  return (
    <Layout region={region} setRegion={setRegion}>
      <Routes>
        <Route path="/"                      element={<HomePage />} />
        <Route path="/champions"             element={<ChampionAnalysisPage />} />
        <Route path="/champions/:championId" element={<ChampionPage />} />
        <Route path="/ranking"               element={<RankingPage />} />
        <Route path="/search"                element={<SearchResultPage />} />
        <Route path="/find/:region/:slug"    element={<PlayerPage />} />
      </Routes>
    </Layout>
  );
}

export default App;
