import React, { useEffect, useState } from 'react';
import Card from '../components/Card';
import { apiService } from '../services/api';
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from 'recharts';

const Profile = () => {
  const [metrics, setMetrics] = useState({ total: 0, helpful: 0, notHelpful: 0 });

  useEffect(() => {
    const fetchInsights = async () => {
      try {
        const data = await apiService.getInsights(1);
        let helpful = 0;
        let notHelpful = 0;
        
        data.forEach(i => {
          if (i.isHelpful === true) helpful++;
          if (i.isHelpful === false) notHelpful++;
        });

        setMetrics({ total: data.length, helpful, notHelpful });
      } catch (e) {
        console.error(e);
      }
    };
    fetchInsights();
  }, []);

  const pieData = [
    { name: 'Helpful', value: metrics.helpful },
    { name: 'Not Helpful', value: metrics.notHelpful }
  ];
  const COLORS = ['#10b981', '#ef4444'];

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">User Profile & Research Metrics</h1>
        <p className="page-subtitle">Your Agentic AI interaction data.</p>
      </div>

      <div className="grid-3">
        <Card title="Total Insights Generated" value={metrics.total} />
        <Card title="Helpful Feedback" value={metrics.helpful} glow="green" />
        <Card title="False Positives" value={metrics.notHelpful} glow="red" />
      </div>

      <div className="grid-2">
        <Card title="Feedback Ratio (AI Usefulness)">
          <div style={{ width: '100%', height: 300, marginTop: '20px' }}>
            {metrics.helpful > 0 || metrics.notHelpful > 0 ? (
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
              <div style={{ color: '#a1a1aa', textAlign: 'center', paddingTop: '100px' }}>No feedback data available. Go rate some insights!</div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Profile;
