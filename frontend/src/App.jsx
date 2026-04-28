import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import Transactions from './pages/Transactions';
import InsightsList from './pages/InsightsList';
import InsightDetail from './pages/InsightDetail';
import AnomalyMonitor from './pages/AnomalyMonitor';
import Trends from './pages/Trends';
import Profile from './pages/Profile';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="transactions" element={<Transactions />} />
          <Route path="insights" element={<InsightsList />} />
          <Route path="insights/:id" element={<InsightDetail />} />
          <Route path="monitor" element={<AnomalyMonitor />} />
          <Route path="trends" element={<Trends />} />
          <Route path="profile" element={<Profile />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
