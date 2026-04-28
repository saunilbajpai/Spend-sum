import React, { useEffect, useState } from 'react';
import Card from '../components/Card';
import { apiService } from '../services/api';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

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
  const COLORS = ['#3b82f6', '#8b5cf6', '#ef4444', '#10b981', '#f59e0b'];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
        <p className="page-subtitle">Welcome back. Here is your financial overview.</p>
      </div>

      <div className="grid-4">
        <Card title="Total Savings" value={`$${data.savings.toFixed(2)}`} glow={data.savings > 0 ? 'green' : 'red'} />
        <Card title="Top Category" value={data.topCategory || 'N/A'} />
        <Card title="Active Alerts" value="2" glow="purple" />
        <Card title="Agent Status" value="Active" />
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
                  <Tooltip contentStyle={{ background: '#18181b', border: '1px solid #27272a', borderRadius: '8px' }}/>
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
