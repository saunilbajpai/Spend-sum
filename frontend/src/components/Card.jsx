import React from 'react';

const Card = ({ title, value, glow, theme, icon, className = '', children, onClick }) => {
  const glowClass = glow ? `glow-${glow}` : '';
  const themeClass = theme ? `theme-${theme}` : '';
  const clickableClass = onClick ? 'clickable' : '';

  return (
    <div className={`glass-card ${glowClass} ${themeClass} ${clickableClass} ${className}`} onClick={onClick}>
      {title && (
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
          <div className="card-title" style={{ marginBottom: 0 }}>{title}</div>
          {icon && <div className="card-icon">{icon}</div>}
        </div>
      )}
      {value && <div className={`card-value ${theme ? 'text-' + theme : ''}`}>{value}</div>}
      {children}
    </div>
  );
};

export default Card;
