import React, { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { Notice } from '../components/Notice';
import { EmptyView, ErrorView, LoadingView } from '../components/StateViews';
import { useLoad } from '../hooks/useLoad';
import { ApiError } from '../services/apiClient';
import { bookingService } from '../services/bookingService';
import { messageForError } from '../services/errorMessages';
import type { Booking, BookingStatus } from '../services/types';
import { isoTime, longDateLabel } from '../utils/week';
import './BookingsPage.css';

type Tab = 'upcoming' | 'past';
const TABS: { value: Tab; label: string; empty: string }[] = [
    { value: 'upcoming', label: 'Upcoming', empty: 'No upcoming bookings' },
    { value: 'past', label: 'Past', empty: 'No past bookings' },
];

const STATUS_LABEL: Record<BookingStatus, string> = {
    BOOKED: 'Booked',
    CHECKED_IN: 'Checked in',
    COMPLETED: 'Completed',
    CANCELLED: 'Cancelled',
    NO_SHOW: 'No show',
};

// Date and time come from the ISO text, so they stay in the resource's (Zurich) time.
const dateOf = (b: Booking) => longDateLabel(b.start.slice(0, 10));
const timeOf = (b: Booking) => `${isoTime(b.start)}–${isoTime(b.end)}`;

export const BookingsPage: React.FC = () => {
    const [tab, setTab] = useState<Tab>('upcoming');
    const { state, reload } = useLoad((signal) => bookingService.listBookings(tab, signal), tab);
    const [notice, setNotice] = useState<{ kind: 'success' | 'error'; message: string } | null>(null);
    const [toCancel, setToCancel] = useState<Booking | null>(null);
    const [busy, setBusy] = useState(false);
    const [cancelError, setCancelError] = useState<string | null>(null);
    const submitting = useRef(false);

    const confirmCancel = async () => {
        if (!toCancel || submitting.current) return;
        submitting.current = true;
        setBusy(true);
        setCancelError(null);
        try {
            await bookingService.cancelBooking(toCancel.id);
            setToCancel(null);
            setNotice({ kind: 'success', message: 'Booking cancelled.' });
            reload();
        } catch (err) {
            const message = messageForError(err);
            if (err instanceof ApiError && (err.code === 'INVALID_STATUS' || err.code === 'NOT_FOUND')) {
                // The list is out of date: close the dialog, explain, and refresh.
                setToCancel(null);
                setNotice({ kind: 'error', message });
                reload();
            } else {
                setCancelError(message);
            }
        } finally {
            submitting.current = false;
            setBusy(false);
        }
    };

    const current = TABS.find((t) => t.value === tab)!;

    return (
        <>
            <h1>My bookings</h1>
            <div className="tabs" role="tablist" aria-label="Booking scope">
                {TABS.map((t) => (
                    <button
                        key={t.value}
                        type="button"
                        role="tab"
                        id={`tab-${t.value}`}
                        aria-selected={tab === t.value}
                        aria-controls="bookings-panel"
                        className={tab === t.value ? 'active' : ''}
                        onClick={() => { setTab(t.value); setNotice(null); }}
                    >
                        {t.label}
                    </button>
                ))}
            </div>

            <div id="bookings-panel" role="tabpanel" aria-labelledby={`tab-${tab}`}>
                {notice && <Notice kind={notice.kind} onDismiss={() => setNotice(null)}>{notice.message}</Notice>}
                {state.status === 'loading' && <LoadingView label="Loading bookings…" />}
                {state.status === 'error' && <ErrorView message={state.message} onRetry={reload} />}
                {state.status === 'ready' && state.data.length === 0 && <EmptyView message={current.empty} />}
                {state.status === 'ready' && state.data.length > 0 && (
                    <ul className="booking-list">
                        {state.data.map((b) => (
                            <li key={b.id} className="booking-row">
                                <div className="booking-main">
                                    <Link to={`/resources/${b.resource.id}`} className="booking-resource">
                                        {b.resource.name}
                                    </Link>
                                    <span className="booking-when">{dateOf(b)}, {timeOf(b)}</span>
                                </div>
                                <span className={`status-badge status-${b.status.toLowerCase()}`}>
                                    {STATUS_LABEL[b.status]}
                                </span>
                                <span className="booking-price">{b.currency} {b.price}</span>
                                {b.cancellable && (
                                    <button
                                        type="button"
                                        className="booking-cancel"
                                        aria-label={`Cancel booking of ${b.resource.name}, ${dateOf(b)}, ${timeOf(b)}`}
                                        onClick={() => { setCancelError(null); setToCancel(b); }}
                                    >
                                        Cancel
                                    </button>
                                )}
                            </li>
                        ))}
                    </ul>
                )}
            </div>

            {toCancel && (
                <ConfirmDialog
                    title="Cancel this booking?"
                    confirmLabel="Yes, cancel booking"
                    dismissLabel="Keep booking"
                    busy={busy}
                    error={cancelError}
                    onConfirm={() => void confirmCancel()}
                    onDismiss={() => setToCancel(null)}
                >
                    <dl>
                        <dt>Resource</dt>
                        <dd>{toCancel.resource.name}</dd>
                        <dt>Date</dt>
                        <dd>{dateOf(toCancel)}</dd>
                        <dt>Time</dt>
                        <dd>{timeOf(toCancel)}</dd>
                        <dt>Price</dt>
                        <dd>{toCancel.currency} {toCancel.price}</dd>
                    </dl>
                </ConfirmDialog>
            )}
        </>
    );
};
