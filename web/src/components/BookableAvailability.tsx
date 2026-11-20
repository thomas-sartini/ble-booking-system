import { useEffect, useRef, useState } from 'react';
import { AvailabilityCalendar } from '@/components/AvailabilityCalendar';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/services/apiClient';
import { bookingService } from '@/services/bookingService';
import { messageForError } from '@/services/errorMessages';
import type { Availability, Booking, ResourceDetails } from '@/services/types';
import {
    formatDuration, pickSlot, selectedSlots, type Selection,
} from '@/utils/booking';
import { priceFor } from '@/utils/price';
import { isoTime, longDateLabel } from '@/utils/week';

interface Props {
    resource: ResourceDetails;
    availability: Availability;
    /** Today in the resource's zone, for the calendar's "today" column. */
    today?: string | null;
    onBooked: (booking: Booking) => void;
    /** The slot was taken meanwhile (BOOKING_OVERLAP): page reloads availability and shows the message. */
    onConflict: (message: string) => void;
}

/** Availability grid with slot-range selection and the booking confirm dialog (US-02.02). */
export function BookableAvailability({ resource, availability, today, onBooked, onConflict }: Props) {
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
            <AvailabilityCalendar days={availability.days} selection={selection} today={today} onSelectSlot={onSelectSlot} />

            <div className="sticky bottom-4 z-10 mt-4 flex flex-col gap-2 rounded-xl border bg-card p-3 shadow-md md:px-4">
                {selectionError && (
                    <Alert variant="destructive" className="border-danger-foreground/25 bg-danger py-2">
                        <AlertDescription className="text-danger-foreground">{selectionError}</AlertDescription>
                    </Alert>
                )}
                {hasSelection ? (
                    <div className="flex flex-wrap items-center justify-between gap-3">
                        <p data-slot="selection-summary" className="text-sm text-muted-foreground">
                            <span className="font-semibold text-foreground">Selected:</span> {longDateLabel(selection!.date)}, {timeRange} ({formatDuration(minutes)})
                            {' · '}CHF {priceFor(resource.pricePerHour, minutes)} (estimate)
                            {!selection!.complete && ' – click another slot to extend the range.'}
                        </p>
                        <div className="flex w-full gap-2 md:w-auto">
                            <Button variant="outline" className="max-md:flex-1" onClick={clear}>Clear selection</Button>
                            <Button
                                className="max-md:flex-1"
                                onClick={() => { setSubmitError(null); setDialogOpen(true); }}
                            >
                                Book…
                            </Button>
                        </div>
                    </div>
                ) : (
                    <p className="text-sm text-muted-foreground">
                        Click a free slot to start a booking, then click the last slot of the range.
                    </p>
                )}
            </div>

            {dialogOpen && hasSelection && (
                <ConfirmDialog
                    title="Confirm booking"
                    description={`Book ${resource.name} for the selected time.`}
                    confirmLabel="Confirm booking"
                    busy={busy}
                    error={submitError}
                    onConfirm={() => void confirm()}
                    onDismiss={() => setDialogOpen(false)}
                >
                    <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2 text-sm">
                        <dt className="text-muted-foreground">Resource</dt>
                        <dd>{resource.name}</dd>
                        <dt className="text-muted-foreground">Date</dt>
                        <dd>{longDateLabel(selection!.date)}</dd>
                        <dt className="text-muted-foreground">Time</dt>
                        <dd>{timeRange} ({resource.timeZone})</dd>
                        <dt className="text-muted-foreground">Duration</dt>
                        <dd>{formatDuration(minutes)}</dd>
                        <dt className="text-muted-foreground">Estimated price</dt>
                        <dd>CHF {priceFor(resource.pricePerHour, minutes)} (estimate, the final price is shown after booking)</dd>
                    </dl>
                </ConfirmDialog>
            )}
        </>
    );
}
