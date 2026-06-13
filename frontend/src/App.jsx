import React, { useState } from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import PlayerPage from './pages/PlayerPage';

function App() {
  const [region, setRegion] = useState('KR');

  return (
    <Layout region={region} setRegion={setRegion}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/find/:region/:slug" element={<PlayerPage />} />
      </Routes>
    </Layout>
  );
}

export default App;
