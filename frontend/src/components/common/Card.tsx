import React from 'react';

export interface CardProps {
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  action?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
}

export const Card: React.FC<CardProps> = ({
  title,
  subtitle,
  action,
  children,
  className = '',
}) => {
  return (
    <section className={`clinical-card ${className}`}>
      {(title || action) && (
        <div className="clinical-card-header">
          <div>
            {title && <h3 className="clinical-card-title">{title}</h3>}
            {subtitle && <p className="clinical-card-subtitle">{subtitle}</p>}
          </div>
          {action && <div className="clinical-card-actions">{action}</div>}
        </div>
      )}
      <div className="clinical-card-body">{children}</div>
    </section>
  );
};
