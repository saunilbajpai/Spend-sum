import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Centralized error handler
const handleRequest = async (request) => {
  try {
    const response = await request();
    return response.data;
  } catch (error) {
    console.error('API Error:', error);
    // Return empty fallback states so the UI doesn't crash
    if (error.response && error.response.data) {
        throw error.response.data;
    }
    throw new Error('Network error');
  }
};

export const apiService = {
  // Transactions
  getTransactions: (userId) => handleRequest(() => api.get(`/transactions/user/${userId}`)),
  createTransaction: (data) => handleRequest(() => api.post('/transactions', data)),
  
  // Metrics & Visualizations
  getSavings: (userId) => handleRequest(() => api.get(`/transactions/savings/${userId}`)),
  getTopCategory: (userId) => handleRequest(() => api.get(`/transactions/top-category/${userId}`, { responseType: 'text' })),
  getCategorySpending: (userId) => handleRequest(() => api.get(`/transactions/category-wise/${userId}`)),
  getIncomeExpense: (userId) => handleRequest(() => api.get(`/transactions/income-expense/${userId}`)),
  
  // AI Insights & Research
  getInsights: (userId) => handleRequest(() => api.get(`/insights/user/${userId}`)),
  sendFeedback: (insightId, isHelpful) => handleRequest(() => api.post(`/insights/${insightId}/feedback`, { isHelpful })),
  generateAIAdvice: (userId, promptContext) => handleRequest(() => api.post('/ai/generate', { userId, customContext: promptContext }))
};
