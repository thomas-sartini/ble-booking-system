import { X } from 'lucide-react';
import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { toast } from 'sonner';
import { BookingStatusBadge } from '@/components/BookingStatusBadge';
import { PageHeader } from '@/components/PageHeader';
import { SectionCards, type Stat } from '@/components/section-cards';
import { EmptyView, ErrorView, LoadingView } from '@/components/StateViews';
import { Alert, AlertAction, AlertDescription } from '@/components/ui/alert';
import {
    AlertDialog, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter,
    AlertDialogHeader, AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { useIsMobile } from '@/hooks/use-mobile';
import { useLoad } from '@/hooks/useLoad';
import { ApiError } from '@/services/apiClient';
import { bookingService } from '@/services/bookingService';
import { messageForError } from '@/services/errorMessages';
import type { Booking } from '@/services/types';
import { isoTime, longDateLabel } from '@/utils/week';

type Tab = 'upcoming' | 'past';
const TABS: { value: Tab; label: string; empty: string }[] = [
    { value: 'upcoming', label: 'Upcoming', empty: 'No upcoming bookings' },
    { value: 'past', label: 'Past', empty: 'No past bookings' },
];

const toCents = (amount: string) => {
    const [whole, frac = ''] = amount.split('.');
    return Number(whole) * 100 + Number(frac.padEnd(2, '0').slice(0, 2));
};
const formatCents = (cents: number) => `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`;

/** Summary cards computed from the loaded list only (no extra request). Cancelled bookings are not billed. */
function statsFor(tab: Tab, bookings: Booking[]): Stat[] {
    const billable = bookings.filter((b) => b.status !== 'CANCELLED');
    const featured = bookings[0]; // upcoming: soonest first, past: newest first
    return [
        {
            label: tab === 'upcoming' ? 'Upcoming bookings' : 'Past bookings',
            value: String(bookings.length),
            hint: `${billable.length} billable, ${bookings.length - billable.length} cancelled`,
        },
        {
            label: 'Total',
            value: `CHF ${formatCents(billable.reduce((sum, b) => sum + toCents(b.price), 0))}`,
            hint: 'Cancelled bookings excluded',
        },
        {
            label: tab === 'upcoming' ? 'Next session' : 'Latest session',
            value: featured.resource.name,
            hint: `${longDateLabel(featured.start.slice(0, 10))}, ${isoTime(featured.start)}–${isoTime(featured.end)}`,
        },
    ];
}

// Date and time come from the ISO text, so they stay in the resource's (Zurich) time.
const dateOf = (b: Booking) => longDateLabel(b.start.slice(0, 10));
const timeOf = (b: Booking) => `${isoTime(b.start)}–${isoTime(b.end)}`;

export function BookingsPage() {
    const [tab, setTab] = useState<Tab>('upcoming');
    const { state, reload } = useLoad((signal) => bookingService.listBookings(tab, signal), tab);
    // Success is announced by a toast; a stale-list error (INVALID_STATUS / NOT_FOUND) stays visible as an alert.
    const [errorNotice, setErrorNotice] = useState<string | null>(null);
    const [toCancel, setToCancel] = useState<Booking | null>(null);
    const [busy, setBusy] = useState(false);
    const isMobile = useIsMobile();
    const [cancelError, setCancelError] = useState<string | null>(null);
    const submitting = useRef(false);
    // The dialog is opened by state, so remember the opener to give focus back on close.
    const opener = useRef<Element | null>(null);

    const confirmCancel = async () => {
        if (!toCancel || submitting.current) return;
        submitting.current = true;
        setBusy(true);
        setCancelError(null);
        try {
            await bookingService.cancelBooking(toCancel.id);
            setToCancel(null);
            toast.success('Booking cancelled.');
            reload();
        } catch (err) {
            const message = messageForError(err);
            if (err instanceof ApiError && (err.code === 'INVALID_STATUS' || err.code === 'NOT_FOUND')) {
                // The list is out of date: close the dialog, explain, and refresh.
                setToCancel(null);
                setErrorNotice(message);
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
    const cancelLabel = (b: Booking) => `Cancel booking of ${b.resource.name}, ${dateOf(b)}, ${timeOf(b)}`;
    const startCancel = (b: Booking) => { opener.current = document.activeElement; setCancelError(null); setToCancel(b); };

    return (
        <>
            <PageHeader
                title="My bookings"
                subtitle="Track your sessions and manage your reservations."
                action={<Button asChild><Link to="/resources">Browse resources</Link></Button>}
            />
            <Tabs value={tab} onValueChange={(v) => { setTab(v as Tab); setErrorNotice(null); }}>
                <TabsList variant="line" className="mb-4 w-full justify-start border-b" aria-label="Booking scope">
                    {TABS.map((t) => (
                        <TabsTrigger key={t.value} value={t.value} className="flex-none px-4">{t.label}</TabsTrigger>
                    ))}
                </TabsList>

                <TabsContent value={tab}>
                    {errorNotice && (
                        <Alert variant="destructive" className="mb-4 border-danger-foreground/25 bg-danger pr-12">
                            <AlertDescription className="text-danger-foreground">{errorNotice}</AlertDescription>
                            <AlertAction>
                                <Button variant="ghost" size="icon-sm" aria-label="Dismiss" onClick={() => setErrorNotice(null)}>
                                    <X aria-hidden="true" />
                                </Button>
                            </AlertAction>
                        </Alert>
                    )}
                    {state.status === 'loading' && <LoadingView label="Loading bookings…" rows={3} />}
                    {state.status === 'error' && <ErrorView message={state.message} code={state.code} onRetry={reload} />}
                    {state.status === 'ready' && state.data.length === 0 && (
                        <EmptyView
                            message={current.empty}
                            text="Bookings you make will show up here."
                            icon="calendar"
                            action={{ label: 'Browse resources', to: '/resources' }}
                        />
                    )}
                    {state.status === 'ready' && state.data.length > 0 && (
                        <>
                            <SectionCards stats={statsFor(tab, state.data)} />
                            {isMobile ? (
                                <ul className="flex flex-col gap-3">
                                    {state.data.map((b) => (
                                        <li key={b.id}>
                                            <Card>
                                                <CardContent className="flex flex-col gap-3">
                                                    <div className="flex items-start justify-between gap-3">
                                                        <div>
                                                            <Link to={`/resources/${b.resource.id}`} className="font-semibold underline-offset-4 hover:underline">
                                                                {b.resource.name}
                                                            </Link>
                                                            <p className="text-xs text-muted-foreground">#{b.id}</p>
                                                        </div>
                                                        <BookingStatusBadge status={b.status} />
                                                    </div>
                                                    <dl className="grid grid-cols-[max-content_1fr] gap-x-4 gap-y-1 text-sm">
                                                        <dt className="text-muted-foreground">Date</dt>
                                                        <dd>{dateOf(b)}</dd>
                                                        <dt className="text-muted-foreground">Time slot</dt>
                                                        <dd>{timeOf(b)}</dd>
                                                        <dt className="text-muted-foreground">Total</dt>
                                                        <dd>{b.currency} {b.price}</dd>
                                                    </dl>
                                                    {b.cancellable && (
                                                        <Button variant="outline" className="h-10" aria-label={cancelLabel(b)} onClick={() => startCancel(b)}>
                                                            Cancel
                                                        </Button>
                                                    )}
                                                </CardContent>
                                            </Card>
                                        </li>
                                    ))}
                                </ul>
                            ) : (
                                <div className="overflow-hidden rounded-xl border bg-card shadow-xs">
                                    <Table>
                                        <TableHeader className="bg-muted/60">
                                            <TableRow>
                                                <TableHead className="px-4">Booking ID</TableHead>
                                                <TableHead>Resource</TableHead>
                                                <TableHead>Date</TableHead>
                                                <TableHead>Time slot</TableHead>
                                                <TableHead>Status</TableHead>
                                                <TableHead className="text-right">Total</TableHead>
                                                <TableHead className="px-4">Actions</TableHead>
                                            </TableRow>
                                        </TableHeader>
                                        <TableBody>
                                            {state.data.map((b) => (
                                                <TableRow key={b.id} className="h-16">
                                                    <TableCell className="px-4 text-muted-foreground tabular-nums">#{b.id}</TableCell>
                                                    <TableCell>
                                                        <Link to={`/resources/${b.resource.id}`} className="font-medium underline-offset-4 hover:underline">
                                                            {b.resource.name}
                                                        </Link>
                                                    </TableCell>
                                                    <TableCell>{dateOf(b)}</TableCell>
                                                    <TableCell>{timeOf(b)}</TableCell>
                                                    <TableCell><BookingStatusBadge status={b.status} /></TableCell>
                                                    <TableCell className="text-right tabular-nums">{b.currency} {b.price}</TableCell>
                                                    <TableCell className="px-4">
                                                        {b.cancellable && (
                                                            <Button variant="outline" size="sm" aria-label={cancelLabel(b)} onClick={() => startCancel(b)}>
                                                                Cancel
                                                            </Button>
                                                        )}
                                                    </TableCell>
                                                </TableRow>
                                            ))}
                                        </TableBody>
                                    </Table>
                                </div>
                            )}
                            <p className="mt-3 text-sm text-muted-foreground">
                                Showing {state.data.length} {state.data.length === 1 ? 'booking' : 'bookings'}
                            </p>
                        </>
                    )}
                </TabsContent>
            </Tabs>

            <AlertDialog open={!!toCancel} onOpenChange={(open) => { if (!open && !busy) setToCancel(null); }}>
                {toCancel && (
                    <AlertDialogContent
                        onCloseAutoFocus={(e) => {
                            e.preventDefault();
                            if (opener.current instanceof HTMLElement && opener.current.isConnected) opener.current.focus();
                        }}
                    >
                        <AlertDialogHeader>
                            <AlertDialogTitle>Cancel this booking?</AlertDialogTitle>
                            <AlertDialogDescription>
                                The slot becomes available for other people again.
                            </AlertDialogDescription>
                        </AlertDialogHeader>
                        <dl className="grid grid-cols-[max-content_1fr] gap-x-6 gap-y-2 text-sm">
                            <dt className="text-muted-foreground">Resource</dt>
                            <dd>{toCancel.resource.name}</dd>
                            <dt className="text-muted-foreground">Date</dt>
                            <dd>{dateOf(toCancel)}</dd>
                            <dt className="text-muted-foreground">Time</dt>
                            <dd>{timeOf(toCancel)}</dd>
                            <dt className="text-muted-foreground">Price</dt>
                            <dd>{toCancel.currency} {toCancel.price}</dd>
                        </dl>
                        {cancelError && (
                            <Alert variant="destructive" className="border-danger-foreground/25 bg-danger">
                                <AlertDescription className="text-danger-foreground">{cancelError}</AlertDescription>
                            </Alert>
                        )}
                        <AlertDialogFooter>
                            <AlertDialogCancel disabled={busy}>Keep booking</AlertDialogCancel>
                            {/* A plain Button, not AlertDialogAction: the dialog must stay open while the request runs or fails. */}
                            <Button variant="destructive" onClick={() => void confirmCancel()} disabled={busy}>
                                {busy ? 'Please wait…' : 'Yes, cancel booking'}
                            </Button>
                        </AlertDialogFooter>
                    </AlertDialogContent>
                )}
            </AlertDialog>
        </>
    );
}
