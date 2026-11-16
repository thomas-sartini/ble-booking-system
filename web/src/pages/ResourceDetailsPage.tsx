import React, { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { BookableAvailability } from '../components/BookableAvailability';
import { Notice } from '../components/Notice';
import { ErrorView, LoadingView } from '../components/StateViews';
import { messageForError } from '../services/errorMessages';
import { resourceService } from '../services/resourceService';
import type { Availability, Booking, DayName, ResourceDetails, WeeklyWindow } from '../services/types';
import { BOOKING_HORIZON_DAYS } from '../utils/booking';
import { isoTime, addDays, longDateLabel, mondayOf, shortDateLabel, todayIn } from '../utils/week';
import './ResourceDetailsPage.css';

const DAY_ORDER: DayName[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const DAY_LABEL: Record<DayName, string> = {
    MONDAY: 'Monday', TUESDAY: 'Tuesday', WEDNESDAY: 'Wednesday', THURSDAY: 'Thursday',
    FRIDAY: 'Friday', SATURDAY: 'Saturday', SUNDAY: 'Sunday',
};

type Loadable<T> =
    | { status: 'loading' }
    | { status: 'error'; message: string }
    | { status: 'ready'; data: T };

function groupWindows(windows: WeeklyWindow[]): { day: DayName; times: string[] }[] {
    return DAY_ORDER.map((day) => ({
        day,
        times: windows.filter((w) => w.dayOfWeek === day).map((w) => `${w.start}–${w.end}`),
    })).filter((g) => g.times.length > 0);
}

export const ResourceDetailsPage: React.FC = () => {
    const { id = '' } = useParams();
    const [resource, setResource] = useState<Loadable<ResourceDetails>>({ status: 'loading' });
    const [availability, setAvailability] = useState<Loadable<Availability>>({ status: 'loading' });
    const [weekOffset, setWeekOffset] = useState(0);
    const [resourceReload, setResourceReload] = useState(0);
    const [availabilityReload, setAvailabilityReload] = useState(0);
    const [notice, setNotice] = useState<
        { kind: 'success'; booking: Booking } | { kind: 'error'; message: string } | null
    >(null);

    useEffect(() => {
        const controller = new AbortController();
        setResource({ status: 'loading' });
        setWeekOffset(0);
        resourceService
            .getResource(id, controller.signal)
            .then((data) => setResource({ status: 'ready', data }))
            .catch((err: unknown) => {
                if (controller.signal.aborted) return;
                setResource({ status: 'error', message: messageForError(err) });
            });
        return () => controller.abort();
    }, [id, resourceReload]);

    const timeZone = resource.status === 'ready' ? resource.data.timeZone : null;
    // Weeks run Monday to Sunday in the resource's own zone, not the browser's.
    const today = useMemo(() => (timeZone ? todayIn(timeZone) : null), [timeZone]);
    const currentMonday = today ? mondayOf(today) : null;
    const monday = currentMonday ? addDays(currentMonday, 7 * weekOffset) : null;
    const sunday = monday ? addDays(monday, 6) : null;
    // The next week is only useful if it starts within the booking horizon (D8); later weeks are empty.
    const hasNextWeek = !!monday && !!today && addDays(monday, 7) <= addDays(today, BOOKING_HORIZON_DAYS);

    useEffect(() => {
        if (!monday || !sunday) return;
        const controller = new AbortController();
        setAvailability({ status: 'loading' });
        resourceService
            .getAvailability(id, monday, sunday, controller.signal)
            .then((data) => setAvailability({ status: 'ready', data }))
            .catch((err: unknown) => {
                if (controller.signal.aborted) return;
                setAvailability({ status: 'error', message: messageForError(err) });
            });
        return () => controller.abort();
    }, [id, monday, sunday, availabilityReload]);

    if (resource.status === 'loading') return <LoadingView label="Loading resource…" />;
    if (resource.status === 'error') {
        return (
            <>
                <Link to="/resources">← All resources</Link>
                <ErrorView message={resource.message} onRetry={() => setResourceReload((k) => k + 1)} />
            </>
        );
    }

    const r = resource.data;
    const groups = groupWindows(r.windows);

    return (
        <>
            <Link to="/resources">← All resources</Link>
            <h1>{r.name}</h1>
            <dl className="details-list">
                <dt>Description</dt>
                <dd>{r.description ?? '—'}</dd>
                <dt>Location</dt>
                <dd>{r.position ?? '—'}</dd>
                <dt>Type</dt>
                <dd>{r.type === 'MOBILE' ? 'Mobile' : 'Fixed'}</dd>
                <dt>Price</dt>
                <dd>CHF {r.pricePerHour} / h</dd>
                <dt>Time zone</dt>
                <dd>{r.timeZone}</dd>
            </dl>

            <h2>Bookable hours</h2>
            {groups.length === 0 ? (
                <p>No bookable hours.</p>
            ) : (
                <ul className="windows-list">
                    {groups.map((g) => (
                        <li key={g.day}>
                            <strong>{DAY_LABEL[g.day]}</strong> {g.times.join(', ')}
                        </li>
                    ))}
                </ul>
            )}

            <h2>Availability</h2>
            <div className="week-nav">
                <button
                    type="button"
                    onClick={() => setWeekOffset((o) => o - 1)}
                    disabled={weekOffset <= 0}
                >
                    ← Previous week
                </button>
                <span className="week-range" aria-live="polite">
                    {monday && sunday ? `${shortDateLabel(monday)} – ${shortDateLabel(sunday)}` : ''}
                </span>
                <button
                    type="button"
                    onClick={() => setWeekOffset((o) => o + 1)}
                    disabled={!hasNextWeek}
                >
                    Next week →
                </button>
            </div>

            {availability.status === 'loading' && <LoadingView label="Loading availability…" />}
            {availability.status === 'error' && (
                <ErrorView message={availability.message} onRetry={() => setAvailabilityReload((k) => k + 1)} />
            )}
            {notice?.kind === 'success' && (
                <Notice kind="success" onDismiss={() => setNotice(null)}>
                    Booked {notice.booking.resource.name}: {longDateLabel(notice.booking.start.slice(0, 10))},{' '}
                    {isoTime(notice.booking.start)}–{isoTime(notice.booking.end)}, CHF {notice.booking.price}.{' '}
                    <Link to="/bookings">View my bookings</Link>
                </Notice>
            )}
            {notice?.kind === 'error' && (
                <Notice kind="error" onDismiss={() => setNotice(null)}>{notice.message}</Notice>
            )}
            {availability.status === 'ready' && (
                // A new key per week and reload drops the selection, whose indices belong to the old data.
                <BookableAvailability
                    key={`${monday}-${availabilityReload}`}
                    resource={r}
                    availability={availability.data}
                    onBooked={(booking) => {
                        setNotice({ kind: 'success', booking });
                        setAvailabilityReload((k) => k + 1);
                    }}
                    onConflict={(message) => {
                        setNotice({ kind: 'error', message });
                        setAvailabilityReload((k) => k + 1);
                    }}
                />
            )}
        </>
    );
};
