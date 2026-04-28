import React, { useState, useEffect } from 'react';
import Card from '../components/Card';
import Badge from '../components/Badge';
import { apiService } from '../services/api';

const Transactions = () => {
  const [transactions, setTransactions] = useState([]);
  const [form, setForm] = useState({ amount: '', description: '', date: '', type: 'EXPENSE', categoryId: 1 });
  const [loading, setLoading] = useState(false);

  const fetchTxs = async () => {
    try {
      const data = await apiService.getTransactions(1);
      setTransactions(data || []);
    } catch (e) {
      console.error(e);
    }
  };

  useEffect(() => { fetchTxs(); }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await apiService.createTransaction({
        amount: parseFloat(form.amount),
        description: form.description,
        date: form.date,
        type: form.type,
        user: { id: 1 },
        category: { id: parseInt(form.categoryId) }
      });
      fetchTxs();
      setForm({ ...form, amount: '', description: '' });
    } catch (e) {
      alert('Failed to add transaction. Make sure the Category ID exists in your database.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Transactions</h1>
        <p className="page-subtitle">Manage your expenses. Adding a transaction triggers the AI Agent.</p>
      </div>

      <div className="grid-2">
        <Card title="Add Transaction">
          <form onSubmit={handleSubmit} style={{ marginTop: '16px' }}>
            <div className="form-group">
              <label className="form-label">Amount ($)</label>
              <input type="number" step="0.01" className="form-input" required value={form.amount} onChange={e => setForm({...form, amount: e.target.value})} />
            </div>
            <div className="form-group">
              <label className="form-label">Description</label>
              <input type="text" className="form-input" required value={form.description} onChange={e => setForm({...form, description: e.target.value})} />
            </div>
            <div className="grid-2" style={{ marginBottom: 0 }}>
              <div className="form-group">
                <label className="form-label">Date</label>
                <input type="date" className="form-input" required value={form.date} onChange={e => setForm({...form, date: e.target.value})} />
              </div>
              <div className="form-group">
                <label className="form-label">Category ID</label>
                <input type="number" className="form-input" required value={form.categoryId} onChange={e => setForm({...form, categoryId: e.target.value})} />
              </div>
            </div>
            <button type="submit" className="btn primary" disabled={loading} style={{ width: '100%', justifyContent: 'center' }}>
              {loading ? 'Processing via Agent...' : 'Add Transaction'}
            </button>
          </form>
        </Card>

        <Card title="Recent Transactions">
          <div className="list-container" style={{ marginTop: '16px', maxHeight: '350px', overflowY: 'auto' }}>
            {transactions.length > 0 ? transactions.slice().reverse().map(tx => (
              <div key={tx.id} className="list-item">
                <div>
                  <div style={{ fontWeight: 500 }}>{tx.description}</div>
                  <div style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>{tx.date}</div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                  <Badge type={tx.type}>{tx.type}</Badge>
                  <span style={{ fontWeight: 600 }}>${tx.amount.toFixed(2)}</span>
                </div>
              </div>
            )) : (
              <div style={{ padding: '24px', textAlign: 'center', color: 'var(--text-secondary)' }}>No transactions yet.</div>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default Transactions;
