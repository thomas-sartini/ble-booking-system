import type { ReactNode } from 'react';

interface Props {
    title: string;
    subtitle?: ReactNode;
    action?: ReactNode;
    /** Shown above the title (e.g. a back link). */
    back?: ReactNode;
    /** Compact info row under the subtitle. */
    meta?: ReactNode;
}

export function PageHeader({ title, subtitle, action, back, meta }: Props) {
    return (
        <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
            <div className="flex flex-col gap-1">
                {back}
                <h1 className="text-2xl font-medium tracking-tight md:text-3xl">{title}</h1>
                {subtitle && <p className="text-muted-foreground">{subtitle}</p>}
                {meta}
            </div>
            {action}
        </div>
    );
}
