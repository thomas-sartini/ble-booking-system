import type { LucideIcon } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';

interface Props {
    icon: LucideIcon;
    title: string;
    text?: string;
    action?: { label: string; to?: string; onClick?: () => void };
}

/** Shared empty state: icon, title, short text and an optional call to action. */
export function EmptyState({ icon: Icon, title, text, action }: Props) {
    return (
        <div className="flex flex-col items-center gap-2 rounded-xl border border-dashed bg-card px-4 py-12 text-center">
            <span className="flex size-12 items-center justify-center rounded-full bg-muted text-muted-foreground">
                <Icon className="size-6" aria-hidden="true" />
            </span>
            <p className="text-lg font-semibold">{title}</p>
            {text && <p className="max-w-md text-muted-foreground">{text}</p>}
            {action?.to && (
                <Button asChild variant="outline" className="mt-2">
                    <Link to={action.to}>{action.label}</Link>
                </Button>
            )}
            {action?.onClick && (
                <Button variant="outline" className="mt-2" onClick={action.onClick}>{action.label}</Button>
            )}
        </div>
    );
}
