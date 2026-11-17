import { AlertTriangle, CalendarDays, Loader2, Lock, Search, WifiOff, type LucideIcon } from 'lucide-react';
import { Link } from 'react-router-dom';
import { EmptyState } from '@/components/EmptyState';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';

/** Spinner plus label; `rows` adds skeleton rows for lists and tables. */
export function LoadingView({ label = 'Loading…', rows = 0 }: { label?: string; rows?: number }) {
    return (
        <div role="status" className="flex flex-col gap-4 py-8">
            <span className="flex items-center justify-center gap-2 text-muted-foreground">
                <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                {label}
            </span>
            {rows > 0 && (
                <div aria-hidden="true" className="flex flex-col gap-3">
                    {Array.from({ length: rows }, (_, i) => <Skeleton key={i} className="h-16 w-full" />)}
                </div>
            )}
        </div>
    );
}

interface EmptyProps {
    /** Title; also the text tests and screen readers find. */
    message: string;
    text?: string;
    icon?: 'search' | 'calendar';
    action?: { label: string; to?: string; onClick?: () => void };
}

const EMPTY_ICONS: Record<NonNullable<EmptyProps['icon']>, LucideIcon> = { search: Search, calendar: CalendarDays };

export function EmptyView({ message, text, icon = 'search', action }: EmptyProps) {
    return <EmptyState icon={EMPTY_ICONS[icon]} title={message} text={text} action={action} />;
}

interface ErrorProps {
    message: string;
    /** API error code: only chooses title and icon, the text comes from messageForError. */
    code?: string;
    onRetry?: () => void;
}

export function ErrorView({ message, code, onRetry }: ErrorProps) {
    const forbidden = code === 'FORBIDDEN';
    const offline = code === 'NETWORK_ERROR';
    const Icon = forbidden ? Lock : offline ? WifiOff : AlertTriangle;
    const title = forbidden ? 'Access denied' : offline ? "Can't reach the server" : 'Something went wrong';
    return (
        <Alert variant="destructive" className="mx-auto my-8 max-w-xl border-danger-foreground/25 bg-danger">
            <Icon aria-hidden="true" />
            <AlertTitle>{title}</AlertTitle>
            <AlertDescription className="text-foreground/80">
                <p>{message}</p>
                {forbidden && (
                    <Button asChild variant="outline" size="sm" className="bg-card">
                        <Link to="/resources">Back to resource catalog</Link>
                    </Button>
                )}
                {!forbidden && onRetry && (
                    <Button variant="outline" size="sm" className="bg-card" onClick={onRetry}>Retry</Button>
                )}
            </AlertDescription>
        </Alert>
    );
}
