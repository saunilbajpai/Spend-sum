import React from 'react';

const Card = ({ title, value, glow, className = '', children, onClick }) => {
  const glowClass = glow ? `glow-${glow}` : '';
  const clickableClass = onClick ? 'clickable' : '';

  return (
    <div className={`glass-card ${glowClass} ${clickableClass} ${className}`} onClick={onClick}>
      {title && <div className="card-title">{title}</div>}
      {value && <div className="card-value">{value}</div>}
      {children}
    </div>
  );
};

export default Card;
