import React, { useEffect, useState } from 'react';
import Card from '../components/Card';
import { apiService } from '../services/api';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip as RechartsTooltip } from 'recharts';
import { Wallet, TrendingUp, BellRing, Bot } from 'lucide-react';

const Dashboard = () => {
  const [data, setData] = useState({
    savings: 0, topCategory: 'Loading...', categorySpending: {}
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        const savings = await apiService.getSavings(1);
        const topCat = await apiService.getTopCategory(1);
        const spending = await apiService.getCategorySpending(1);
        setData({ savings, topCategory: topCat, categorySpending: spending });
      } catch (e) {
        console.error(e);
      }
    };
    fetchData();
  }, []);

  const pieData = Object.keys(data.categorySpending).map(key => ({
    name: key, value: data.categorySpending[key]
  }));
  // Vibrant colors for the pie chart
  const COLORS = ['#3b82f6', '#8b5cf6', '#ec4899', '#10b981', '#f59e0b', '#06b6d4'];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title text-gradient">Dashboard</h1>
        <p className="page-subtitle">Welcome back. Here is your financial overview.</p>
      </div>

      <div className="grid-4">
        <Card title="Total Savings" value={`$${data.savings.toFixed(2)}`} theme="green" icon={<Wallet size={24} />} />
        <Card title="Top Category" value={data.topCategory || 'N/A'} theme="orange" icon={<TrendingUp size={24} />} />
        <Card title="Active Alerts" value="2" theme="purple" icon={<BellRing size={24} />} />
        <Card title="Agent Status" value="Active" theme="blue" icon={<Bot size={24} />} />
      </div>

      <div className="grid-2">
        <Card title="Spending Breakdown">
          <div style={{ width: '100%', height: 300, marginTop: '20px' }}>
            {pieData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={pieData} innerRadius={60} outerRadius={100} paddingAngle={5} dataKey="value">
                    {pieData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <RechartsTooltip contentStyle={{ background: 'rgba(24, 24, 27, 0.9)', backdropFilter: 'blur(8px)', border: '1px solid #3f3f46', borderRadius: '12px', boxShadow: '0 10px 15px -3px rgba(0, 0, 0, 0.5)' }} itemStyle={{ color: '#f8fafc', fontWeight: 600 }}/>
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ color: '#a1a1aa', textAlign: 'center', paddingTop: '100px' }}>No spending data available</div>
            )}
          </div>
        </Card>
        
        <Card title="Recent Agent Actions">
          <div className="list-container" style={{ marginTop: '20px' }}>
             <div className="list-item">Analyzed new grocery transaction</div>
             <div className="list-item">Calculated velocity for Dining Out</div>
             <div className="list-item">Generated 1 new AI advice</div>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Dashboard;
