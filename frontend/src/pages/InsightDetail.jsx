import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Card from '../components/Card';
import Badge from '../components/Badge';
import { apiService } from '../services/api';
import { ArrowLeft, ThumbsUp, ThumbsDown } from 'lucide-react';

const InsightDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [insight, setInsight] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchInsight = async () => {
      try {
        const data = await apiService.getInsights(1);
        const found = data.find(i => i.id.toString() === id);
        setInsight(found);
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    };
    fetchInsight();
  }, [id]);

  const handleFeedback = async (isHelpful) => {
    try {
      const updated = await apiService.sendFeedback(id, isHelpful);
      setInsight(updated);
    } catch (e) {
      console.error('Feedback failed');
    }
  };

  if (loading) return <div>Loading...</div>;
  if (!insight) return <div>Insight not found.</div>;

  return (
    <div>
      <div className="page-header">
        <button onClick={() => navigate(-1)} className="btn" style={{ marginBottom: '16px' }}>
          <ArrowLeft size={16} /> Back to Insights
        </button>
        <h1 className="page-title">Insight Analysis</h1>
        <p className="page-subtitle">Detailed view of the AI-generated context and metrics.</p>
      </div>

      <div className="grid-2">
        <Card title="The Advice" glow={insight.source === 'AI_GENERATED' ? 'purple' : ''}>
          <p style={{ fontSize: '18px', lineHeight: 1.6, marginTop: '16px', color: 'var(--text-primary)' }}>
            "{insight.insightText}"
          </p>
          
          <div style={{ marginTop: '32px', borderTop: '1px solid var(--border-color)', paddingTop: '24px' }}>
            <div className="card-title" style={{ marginBottom: '16px' }}>Research Metrics</div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '4px' }}>Latency</div>
                <div style={{ fontWeight: 600 }}>{insight.processingTimeMs} ms</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '4px' }}>Confidence Score</div>
                <div style={{ fontWeight: 600 }}>{(insight.confidenceScore * 100).toFixed(1)}%</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '4px' }}>Source</div>
                <Badge type={insight.source}>{insight.source}</Badge>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: 'var(--text-secondary)', marginBottom: '4px' }}>Anomaly Detected</div>
                <Badge type={insight.anomalyType}>{insight.anomalyType}</Badge>
              </div>
            </div>
          </div>
        </Card>

        <Card title="Provide Feedback">
          <p style={{ color: 'var(--text-secondary)', marginBottom: '24px', fontSize: '14px' }}>
            Was this insight accurate and helpful? Your feedback improves the model and provides data for our false-positive research metrics.
          </p>
          
          <div style={{ display: 'flex', gap: '16px' }}>
            <button 
              className={`btn ${insight.isHelpful === true ? 'primary' : ''}`}
              onClick={() => handleFeedback(true)}
              style={{ flex: 1, justifyContent: 'center' }}
            >
              <ThumbsUp size={18} /> Helpful
            </button>
            <button 
              className={`btn ${insight.isHelpful === false ? 'primary' : ''}`}
              onClick={() => handleFeedback(false)}
              style={{ flex: 1, justifyContent: 'center', background: insight.isHelpful === false ? 'var(--accent-red)' : '' }}
            >
              <ThumbsDown size={18} /> Not Helpful
            </button>
          </div>
          
          {insight.isHelpful !== null && (
            <div style={{ marginTop: '16px', fontSize: '13px', color: 'var(--accent-green)', textAlign: 'center' }}>
              Feedback recorded. Thank you!
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default InsightDetail;
