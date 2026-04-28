import React, { useEffect, useState } from 'react';
import Card from '../components/Card';
import { apiService } from '../services/api';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts';

const Trends = () => {
  const [txs, setTxs] = useState([]);

  useEffect(() => {
    const fetchTxs = async () => {
      try {
        const data = await apiService.getTransactions(1);
        setTxs(data || []);
      } catch (e) {
        console.error(e);
      }
    };
    fetchTxs();
  }, []);

  // Process data for charts
  // Group by date
  const timelineData = [];
  const grouped = txs.reduce((acc, tx) => {
    const date = tx.date;
    if (!acc[date]) acc[date] = { date, income: 0, expense: 0 };
    if (tx.type === 'EXPENSE') acc[date].expense += tx.amount;
    else acc[date].income += tx.amount;
    return acc;
  }, {});

  Object.values(grouped).sort((a, b) => new Date(a.date) - new Date(b.date)).forEach(item => {
    timelineData.push(item);
  });

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Financial Trends</h1>
        <p className="page-subtitle">Visualize your spending patterns over time.</p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '24px' }}>
        <Card title="Income vs Expense Timeline">
          <div style={{ height: 350, width: '100%', marginTop: '24px' }}>
            {timelineData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={timelineData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                  <XAxis dataKey="date" stroke="#a1a1aa" fontSize={12} tickLine={false} axisLine={false} />
                  <YAxis stroke="#a1a1aa" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(value) => `$${value}`} />
                  <Tooltip 
                    contentStyle={{ background: '#18181b', border: '1px solid #27272a', borderRadius: '8px' }}
                    itemStyle={{ fontSize: '14px', fontWeight: 500 }}
                  />
                  <Line type="monotone" dataKey="expense" stroke="#ef4444" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                  <Line type="monotone" dataKey="income" stroke="#10b981" strokeWidth={3} dot={{ r: 4 }} activeDot={{ r: 6 }} />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ color: '#a1a1aa', textAlign: 'center', paddingTop: '150px' }}>No timeline data available</div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Trends;
