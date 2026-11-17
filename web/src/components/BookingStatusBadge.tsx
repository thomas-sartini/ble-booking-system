import { AlertTriangle, CalendarDays, Check, X, type LucideIcon } from 'lucide-react';
import { Badge } from '@/components/ui/badge';
import type { BookingStatus } from '@/services/types';

type Variant = 'info' | 'success' | 'secondary' | 'warning';

const STATUS: Record<BookingStatus, { label: string; variant: Variant; icon: LucideIcon }> = {
    BOOKED: { label: 'Booked', variant: 'info', icon: CalendarDays },
    CHECKED_IN: { label: 'Checked in', variant: 'success', icon: Check },
    COMPLETED: { label: 'Completed', variant: 'success', icon: Check },
    CANCELLED: { label: 'Cancelled', variant: 'secondary', icon: X },
    NO_SHOW: { label: 'No show', variant: 'warning', icon: AlertTriangle },
};

/** Status is always text plus icon, never colour only. */
export function BookingStatusBadge({ status }: { status: BookingStatus }) {
    const { label, variant, icon: Icon } = STATUS[status];
    return (
        <Badge variant={variant}>
            <Icon aria-hidden="true" />
            {label}
        </Badge>
    );
}
