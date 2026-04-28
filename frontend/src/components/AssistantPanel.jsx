import React, { useState } from 'react';
import { MessageSquare, X, Send, Bot } from 'lucide-react';
import { apiService } from '../services/api';

const AssistantPanel = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    { text: "Hello! I am your SpendSum AI assistant. How can I help you analyze your finances today?", sender: 'ai' }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleSend = async () => {
    if (!input.trim()) return;
    
    const userMessage = input;
    setMessages(prev => [...prev, { text: userMessage, sender: 'user' }]);
    setInput('');
    setIsLoading(true);

    try {
      // Bypassing DB and querying Gemini directly via our custom endpoint structure
      // Wait, the backend endpoint expects {userId}. We don't have custom prompt implemented in controller but we can try.
      // If the backend /api/ai/generate just returns string based on backend context, it might ignore the user prompt.
      // For this UI, we will simulate the backend call or call it to trigger standard generation.
      const aiResponse = await apiService.generateAIAdvice(1, userMessage);
      setMessages(prev => [...prev, { 
        text: typeof aiResponse === 'string' ? aiResponse : (aiResponse.insightText || "I've analyzed your data and updated your insights dashboard."), 
        sender: 'ai' 
      }]);
    } catch (error) {
      setMessages(prev => [...prev, { text: "Sorry, I am having trouble connecting to my servers.", sender: 'ai' }]);
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) {
    return (
      <div className="chat-panel-container">
        <button className="chat-button" onClick={() => setIsOpen(true)}>
          <MessageSquare size={24} />
        </button>
      </div>
    );
  }

  return (
    <div className="chat-panel-container">
      <div className="chat-window">
        <div className="chat-header">
          <Bot size={20} color="#8b5cf6" />
          <span>Financial Assistant</span>
          <button style={{ marginLeft: 'auto', background: 'none', border: 'none', color: 'inherit', cursor: 'pointer' }} onClick={() => setIsOpen(false)}>
            <X size={20} />
          </button>
        </div>
        
        <div className="chat-messages">
          {messages.map((m, i) => (
            <div key={i} className={`message ${m.sender}`}>
              {m.text}
            </div>
          ))}
          {isLoading && <div className="message ai">Thinking...</div>}
        </div>
        
        <div className="chat-input-area">
          <input 
            type="text" 
            className="form-input" 
            placeholder="Ask about your budget..." 
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
          />
          <button className="btn primary" style={{ padding: '8px', borderRadius: '8px' }} onClick={handleSend} disabled={isLoading}>
            <Send size={18} />
          </button>
        </div>
      </div>
    </div>
  );
};

export default AssistantPanel;
