import { Card, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';

export interface Stat {
    label: string;
    value: string;
    hint: string;
}

/** Row of summary cards (the dashboard-01 "section cards"), filled with values computed from loaded data. */
export function SectionCards({ stats }: { stats: Stat[] }) {
    return (
        <div className="mb-6 grid gap-4 sm:grid-cols-3">
            {stats.map((s) => (
                <Card key={s.label} size="sm">
                    <CardHeader>
                        <CardDescription>{s.label}</CardDescription>
                        <CardTitle className="text-2xl font-semibold tabular-nums group-data-[size=sm]/card:text-2xl">{s.value}</CardTitle>
                    </CardHeader>
                    <CardFooter className="text-sm text-muted-foreground">{s.hint}</CardFooter>
                </Card>
            ))}
        </div>
    );
}
