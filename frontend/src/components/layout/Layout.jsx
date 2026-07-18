import React from 'react';
import Header from './Header';
import Footer from './Footer';

export default function Layout({ children, region, setRegion }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Header region={region} setRegion={setRegion} />
      <main style={{
        paddingTop: 52,
        flex: 1,
        background: '#0d1520',
      }}>
        {children}
      </main>
      <Footer />
    </div>
  );
}
