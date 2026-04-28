import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { LayoutDashboard, Receipt, BrainCircuit, Activity, LineChart, User } from 'lucide-react';
import AssistantPanel from './AssistantPanel';

const Layout = () => {
  return (
    <div className="app-container">
      <aside className="sidebar">
        <div className="logo-container">
          <BrainCircuit color="#8b5cf6" size={28} />
          <span>SpendSum AI</span>
        </div>
        
        <nav className="sidebar-nav">
          <NavLink to="/" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <LayoutDashboard size={20} color="#3b82f6" /> Dashboard
          </NavLink>
          <NavLink to="/transactions" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Receipt size={20} color="#10b981" /> Transactions
          </NavLink>
          <NavLink to="/insights" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <BrainCircuit size={20} color="#8b5cf6" /> AI Insights
          </NavLink>
          <NavLink to="/monitor" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <Activity size={20} color="#ef4444" /> Anomaly Monitor
          </NavLink>
          <NavLink to="/trends" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <LineChart size={20} color="#f59e0b" /> Trends
          </NavLink>
          <NavLink to="/profile" className={({isActive}) => isActive ? "nav-link active" : "nav-link"}>
            <User size={20} color="#0ea5e9" /> Profile
          </NavLink>
        </nav>
      </aside>
      
      <main className="main-content">
        <Outlet />
      </main>

      {/* Global AI Assistant */}
      <AssistantPanel />
    </div>
  );
};

export default Layout;
