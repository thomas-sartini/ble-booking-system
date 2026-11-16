import React, { useEffect, useRef } from 'react';
import './ConfirmDialog.css';

interface Props {
    title: string;
    children: React.ReactNode;
    confirmLabel: string;
    dismissLabel?: string;
    busy?: boolean;
    error?: string | null;
    onConfirm: () => void;
    onDismiss: () => void;
}

const FOCUSABLE = 'button:not([disabled]), a[href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

/** Modal confirm dialog: focus moves in, Tab is trapped, Esc dismisses (not while busy), focus returns on close. */
export const ConfirmDialog: React.FC<Props> = ({
    title, children, confirmLabel, dismissLabel = 'Cancel', busy = false, error, onConfirm, onDismiss,
}) => {
    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const previous = document.activeElement as HTMLElement | null;
        ref.current?.focus();
        return () => previous?.focus?.();
    }, []);

    const onKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Escape') {
            e.stopPropagation();
            if (!busy) onDismiss();
        } else if (e.key === 'Tab' && ref.current) {
            const items = Array.from(ref.current.querySelectorAll<HTMLElement>(FOCUSABLE));
            if (items.length === 0) return;
            const first = items[0];
            const last = items[items.length - 1];
            if (e.shiftKey && (document.activeElement === first || document.activeElement === ref.current)) {
                e.preventDefault();
                last.focus();
            } else if (!e.shiftKey && document.activeElement === last) {
                e.preventDefault();
                first.focus();
            }
        }
    };

    return (
        <div className="dialog-backdrop">
            <div
                ref={ref}
                className="dialog"
                role="dialog"
                aria-modal="true"
                aria-labelledby="dialog-title"
                tabIndex={-1}
                onKeyDown={onKeyDown}
            >
                <h2 id="dialog-title">{title}</h2>
                <div className="dialog-body">{children}</div>
                {error && <p className="dialog-error" role="alert">{error}</p>}
                <div className="dialog-actions">
                    <button type="button" onClick={onDismiss} disabled={busy}>{dismissLabel}</button>
                    <button type="button" className="dialog-confirm" onClick={onConfirm} disabled={busy}>
                        {busy ? 'Please wait…' : confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
};
