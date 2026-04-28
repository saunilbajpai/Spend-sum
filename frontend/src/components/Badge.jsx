import React from 'react';

const Badge = ({ type, children }) => {
  let colorClass = 'gray';
  
  if (type === 'VELOCITY' || type === 'OVER_BUDGET' || type === 'HIGH' || type === 'ALERT') colorClass = 'red';
  else if (type === 'AI_GENERATED') colorClass = 'purple';
  else if (type === 'DEFICIT_RESOLVED' || type === 'INCOME' || type === 'HELPFUL') colorClass = 'green';
  
  return (
    <span className={`badge ${colorClass}`}>
      {children}
    </span>
  );
};

export default Badge;
