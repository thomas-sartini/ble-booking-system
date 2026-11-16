import React from 'react';
import './Notice.css';

interface Props {
    kind: 'success' | 'error';
    children: React.ReactNode;
    onDismiss: () => void;
}

export const Notice: React.FC<Props> = ({ kind, children, onDismiss }) => (
    <div className={`notice notice-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
        <div>{children}</div>
        <button type="button" onClick={onDismiss} aria-label="Dismiss">×</button>
    </div>
);
