import React from 'react';
import './StateViews.css';

export const LoadingView: React.FC<{ label?: string }> = ({ label = 'Loading…' }) => (
    <div className="state-view" role="status">{label}</div>
);

export const EmptyView: React.FC<{ message: string }> = ({ message }) => (
    <div className="state-view state-empty">{message}</div>
);

export const ErrorView: React.FC<{ message: string; onRetry?: () => void }> = ({ message, onRetry }) => (
    <div className="state-view state-error" role="alert">
        <p>{message}</p>
        {onRetry && <button type="button" className="state-retry" onClick={onRetry}>Retry</button>}
    </div>
);
