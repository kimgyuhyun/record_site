import React from 'react';
import Header from './Header';

export default function Layout({ children, region, setRegion }) {
  return (
    <>
      <Header region={region} setRegion={setRegion} />
      <main style={{
        paddingTop: 52,
        minHeight: '100vh',
        background: '#0d1520',
      }}>
        {children}
      </main>
    </>
  );
}
