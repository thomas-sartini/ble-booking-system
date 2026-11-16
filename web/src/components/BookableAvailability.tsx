import React, { useEffect, useRef, useState } from 'react';
import { ApiError } from '../services/apiClient';
import { bookingService } from '../services/bookingService';
import { messageForError } from '../services/errorMessages';
import type { Availability, Booking, ResourceDetails } from '../services/types';
import {
    formatDuration, pickSlot, selectedSlots, type Selection,
} from '../utils/booking';
import { priceFor } from '../utils/price';
import { isoTime, longDateLabel } from '../utils/week';
import { AvailabilityGrid } from './AvailabilityGrid';
import { ConfirmDialog } from './ConfirmDialog';
import './BookableAvailability.css';

interface Props {
    resource: ResourceDetails;
    availability: Availability;
    onBooked: (booking: Booking) => void;
    /** The slot was taken meanwhile (BOOKING_OVERLAP): page reloads availability and shows the message. */
    onConflict: (message: string) => void;
}

/** Availability grid with slot-range selection and the booking confirm dialog (US-02.02). */
export const BookableAvailability: React.FC<Props> = ({ resource, availability, onBooked, onConflict }) => {
    const [selection, setSelection] = useState<Selection | null>(null);
    const [selectionError, setSelectionError] = useState<string | null>(null);
    const [dialogOpen, setDialogOpen] = useState(false);
    const [busy, setBusy] = useState(false);
    const [submitError, setSubmitError] = useState<string | null>(null);
    const submitting = useRef(false);

    const day = availability.days.find((d) => d.date === selection?.date);
    const slots = selectedSlots(day, selection);
    const hasSelection = slots.length > 0;

    useEffect(() => {
        if (!hasSelection || dialogOpen) return;
        const onKey = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                setSelection(null);
                setSelectionError(null);
            }
        };
        document.addEventListener('keydown', onKey);
        return () => document.removeEventListener('keydown', onKey);
    }, [hasSelection, dialogOpen]);

    const onSelectSlot = (d: typeof availability.days[number], index: number) => {
        const result = pickSlot(selection, d, index, availability.slotMinutes);
        setSelection(result.selection);
        setSelectionError('error' in result ? result.error : null);
    };

    const clear = () => {
        setSelection(null);
        setSelectionError(null);
    };

    const start = slots[0]?.start;
    const end = slots.at(-1)?.end;
    const minutes = slots.length * availability.slotMinutes;
    const timeRange = start && end ? `${isoTime(start)}–${isoTime(end)}` : '';

    const confirm = async () => {
        if (submitting.current || !start || !end) return;
        submitting.current = true;
        setBusy(true);
        setSubmitError(null);
        try {
            const booking = await bookingService.createBooking({ resourceId: resource.id, start, end });
            setDialogOpen(false);
            onBooked(booking);
        } catch (err) {
            const message = messageForError(err);
            if (err instanceof ApiError && err.code === 'BOOKING_OVERLAP') {
                setDialogOpen(false);
                onConflict(message);
            } else {
                setSubmitError(message);
            }
        } finally {
            submitting.current = false;
            setBusy(false);
        }
    };

    return (
        <>
            <AvailabilityGrid days={availability.days} selection={selection} onSelectSlot={onSelectSlot} />

            <div className="selection-bar">
                {selectionError && <p className="selection-error" role="alert">{selectionError}</p>}
                {hasSelection ? (
                    <>
                        <p className="selection-summary">
                            Selected: {longDateLabel(selection!.date)}, {timeRange} ({formatDuration(minutes)})
                            {!selection!.complete && ' – click another slot to extend the range.'}
                        </p>
                        <div className="selection-actions">
                            <button type="button" onClick={clear}>Clear selection</button>
                            <button
                                type="button"
                                className="selection-book"
                                onClick={() => { setSubmitError(null); setDialogOpen(true); }}
                            >
                                Book…
                            </button>
                        </div>
                    </>
                ) : (
                    <p className="selection-hint">Click a free slot to start a booking, then click the last slot of the range.</p>
                )}
            </div>

            {dialogOpen && hasSelection && (
                <ConfirmDialog
                    title="Confirm booking"
                    confirmLabel="Confirm booking"
                    busy={busy}
                    error={submitError}
                    onConfirm={() => void confirm()}
                    onDismiss={() => setDialogOpen(false)}
                >
                    <dl>
                        <dt>Resource</dt>
                        <dd>{resource.name}</dd>
                        <dt>Date</dt>
                        <dd>{longDateLabel(selection!.date)}</dd>
                        <dt>Time</dt>
                        <dd>{timeRange} ({resource.timeZone})</dd>
                        <dt>Duration</dt>
                        <dd>{formatDuration(minutes)}</dd>
                        <dt>Estimated price</dt>
                        <dd>CHF {priceFor(resource.pricePerHour, minutes)} (estimate, the final price is shown after booking)</dd>
                    </dl>
                </ConfirmDialog>
            )}
        </>
    );
};
