import { ArrowLeft, Banknote, CalendarClock, Globe, MapPin, X } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { toast } from 'sonner';
import { BookableAvailability } from '@/components/BookableAvailability';
import { PageHeader } from '@/components/PageHeader';
import { ErrorView, LoadingView } from '@/components/StateViews';
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { codeOf, messageForError } from '@/services/errorMessages';
import { resourceService } from '@/services/resourceService';
import type { Availability, Booking, ResourceDetails } from '@/services/types';
import { BOOKING_HORIZON_DAYS } from '@/utils/booking';
import { foldWindows } from '@/utils/hours';
import { addDays, isoTime, longDateLabel, mondayOf, shortDateLabel, todayIn } from '@/utils/week';

type Loadable<T> =
    | { status: 'loading' }
    | { status: 'error'; message: string; code?: string }
    | { status: 'ready'; data: T };

function BackLink() {
    return (
        <Link to="/resources" className="mb-1 inline-flex items-center gap-1 rounded-sm text-sm font-medium text-muted-foreground hover:text-foreground">
            <ArrowLeft className="size-4" aria-hidden="true" />
            All resources
        </Link>
    );
}

function announceBooking(booking: Booking) {
    toast.success(
        <span>
            Booked {booking.resource.name}: {longDateLabel(booking.start.slice(0, 10))},{' '}
            {isoTime(booking.start)}–{isoTime(booking.end)}, CHF {booking.price}.{' '}
            <Link to="/bookings" className="font-semibold underline underline-offset-2">View my bookings</Link>
        </span>,
        { duration: 15_000 },
    );
}

export function ResourceDetailsPage() {
    const { id = '' } = useParams();
    const [resource, setResource] = useState<Loadable<ResourceDetails>>({ status: 'loading' });
    const [availability, setAvailability] = useState<Loadable<Availability>>({ status: 'loading' });
    const [weekOffset, setWeekOffset] = useState(0);
    const [resourceReload, setResourceReload] = useState(0);
    const [availabilityReload, setAvailabilityReload] = useState(0);
    // A conflict (BOOKING_OVERLAP) stays visible as an alert; a successful booking is announced by a toast.
    const [conflict, setConflict] = useState<string | null>(null);

    useEffect(() => {
        const controller = new AbortController();
        setResource({ status: 'loading' });
        setWeekOffset(0);
        resourceService
            .getResource(id, controller.signal)
            .then((data) => setResource({ status: 'ready', data }))
            .catch((err: unknown) => {
                if (controller.signal.aborted) return;
                setResource({ status: 'error', message: messageForError(err), code: codeOf(err) });
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
                setAvailability({ status: 'error', message: messageForError(err), code: codeOf(err) });
            });
        return () => controller.abort();
    }, [id, monday, sunday, availabilityReload]);

    if (resource.status === 'loading') return <LoadingView label="Loading resource…" rows={2} />;
    if (resource.status === 'error') {
        return (
            <>
                <BackLink />
                <ErrorView message={resource.message} code={resource.code} onRetry={() => setResourceReload((k) => k + 1)} />
            </>
        );
    }

    const r = resource.data;
    const hours = foldWindows(r.windows);

    return (
        <>
            <PageHeader
                title={r.name}
                back={<BackLink />}
                subtitle={r.description}
                meta={
                    // Booking is the point of this page: the resource facts are one slim line under the title.
                    <ul aria-label="Resource information" className="mt-2 flex flex-wrap items-center gap-x-5 gap-y-1.5 text-sm text-muted-foreground">
                        <li><Badge variant="secondary">{r.type === 'MOBILE' ? 'Mobile' : 'Fixed'}</Badge></li>
                        <li className="flex items-center gap-1.5">
                            <MapPin className="size-4" aria-hidden="true" />
                            <span className="sr-only">Location: </span>
                            <span>{r.position ?? '—'}</span>
                        </li>
                        <li className="flex items-center gap-1.5">
                            <Banknote className="size-4" aria-hidden="true" />
                            <span className="sr-only">Price: </span>
                            <span>CHF {r.pricePerHour} / h</span>
                        </li>
                        <li className="flex items-center gap-1.5">
                            <CalendarClock className="size-4" aria-hidden="true" />
                            <span className="sr-only">Bookable hours: </span>
                            <span>{hours || 'No bookable hours.'}</span>
                        </li>
                        <li className="flex items-center gap-1.5">
                            <Globe className="size-4" aria-hidden="true" />
                            Times in <span>{r.timeZone}</span>
                        </li>
                    </ul>
                }
            />

            <div className="mb-4 flex flex-wrap items-center justify-between gap-3 max-md:grid max-md:grid-cols-2">
                <h2 className="text-xl font-semibold max-md:col-span-2">Book a time slot</h2>
                <div className="flex items-center gap-3 max-md:contents">
                    <Button variant="outline" onClick={() => setWeekOffset((o) => o - 1)} disabled={weekOffset <= 0}>
                        ← Previous week
                    </Button>
                    <span aria-live="polite" className="font-semibold max-md:order-first max-md:col-span-2">
                        {monday && sunday ? `${shortDateLabel(monday)} – ${shortDateLabel(sunday)}` : ''}
                    </span>
                    <Button variant="outline" onClick={() => setWeekOffset((o) => o + 1)} disabled={!hasNextWeek}>
                        Next week →
                    </Button>
                </div>
            </div>

            {availability.status === 'loading' && <LoadingView label="Loading availability…" rows={4} />}
            {availability.status === 'error' && (
                <ErrorView
                    message={availability.message}
                    code={availability.code}
                    onRetry={() => setAvailabilityReload((k) => k + 1)}
                />
            )}
            {conflict && (
                <Alert variant="destructive" className="mb-4 border-danger-foreground/25 bg-danger pr-12">
                    <AlertDescription className="text-danger-foreground">{conflict}</AlertDescription>
                    <AlertAction>
                        <Button variant="ghost" size="icon-sm" aria-label="Dismiss" onClick={() => setConflict(null)}>
                            <X aria-hidden="true" />
                        </Button>
                    </AlertAction>
                </Alert>
            )}
            {availability.status === 'ready' && (
                // A new key per week and reload drops the selection, whose indices belong to the old data.
                <BookableAvailability
                    key={`${monday}-${availabilityReload}`}
                    resource={r}
                    availability={availability.data}
                    today={today}
                    onBooked={(booking) => {
                        announceBooking(booking);
                        setConflict(null);
                        setAvailabilityReload((k) => k + 1);
                    }}
                    onConflict={(message) => {
                        setConflict(message);
                        setAvailabilityReload((k) => k + 1);
                    }}
                />
            )}
        </>
    );
}
