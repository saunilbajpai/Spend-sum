import React, { useEffect, useState } from 'react';
import Card from '../components/Card';
import Badge from '../components/Badge';
import { apiService } from '../services/api';
import { AlertTriangle, Clock } from 'lucide-react';

const AnomalyMonitor = () => {
  const [anomalies, setAnomalies] = useState([]);

  useEffect(() => {
    const fetchInsights = async () => {
      try {
        const data = await apiService.getInsights(1);
        const filtered = data.filter(i => i.anomalyType === 'VELOCITY' || i.anomalyType === 'OVER_BUDGET' || i.anomalyType === 'DEFICIT');
        setAnomalies(filtered.reverse());
      } catch (e) {
        console.error(e);
      }
    };
    fetchInsights();
  }, []);

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title" style={{ color: 'var(--accent-red)', display: 'flex', alignItems: 'center', gap: '12px' }}>
          <AlertTriangle size={28} /> Anomaly Monitor
        </h1>
        <p className="page-subtitle">Real-time detection of spending velocity and budget breaches.</p>
      </div>

      <div className="grid-2">
        <Card title="System Status" glow="red">
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', marginTop: '16px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Active Anomalies</span>
              <span style={{ fontSize: '24px', fontWeight: 'bold', color: 'var(--accent-red)' }}>{anomalies.length}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Rule Engine Latency</span>
              <span style={{ fontWeight: 'bold' }}>~2ms</span>
            </div>
          </div>
        </Card>
      </div>

      <h2 style={{ fontSize: '20px', marginBottom: '16px', marginTop: '32px' }}>Detected Events</h2>
      <div className="list-container">
        {anomalies.length > 0 ? anomalies.map(anomaly => (
          <div key={anomaly.id} className="list-item" style={{ borderLeft: `4px solid var(--accent-red)` }}>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '8px' }}>
                <Badge type={anomaly.anomalyType}>{anomaly.anomalyType}</Badge>
                <span style={{ fontSize: '13px', color: 'var(--text-secondary)', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <Clock size={14} /> {anomaly.createdAt ? anomaly.createdAt.split('T')[0] : 'Today'}
                </span>
              </div>
              <p style={{ fontSize: '15px' }}>{anomaly.insightText}</p>
            </div>
            {anomaly.estimatedDaysToExhaustion !== null && anomaly.estimatedDaysToExhaustion > 0 && (
              <div style={{ textAlign: 'right', marginLeft: '24px' }}>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>Days to empty</div>
                <div style={{ fontSize: '24px', fontWeight: 'bold', color: 'var(--accent-red)' }}>{anomaly.estimatedDaysToExhaustion}</div>
              </div>
            )}
          </div>
        )) : (
          <div style={{ padding: '32px', textAlign: 'center', color: 'var(--text-secondary)' }}>
            System is normal. No anomalies detected.
          </div>
        )}
      </div>
    </div>
  );
};

export default AnomalyMonitor;
