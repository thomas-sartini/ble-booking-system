import { Search } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/components/PageHeader';
import { EmptyView, ErrorView, LoadingView } from '@/components/StateViews';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { ToggleGroup, ToggleGroupItem } from '@/components/ui/toggle-group';
import { codeOf, messageForError } from '@/services/errorMessages';
import { resourceService } from '@/services/resourceService';
import type { ResourceSummary, ResourceType } from '@/services/types';

const SEARCH_DEBOUNCE_MS = 300;

type TypeFilter = 'ALL' | ResourceType;
const FILTERS: { value: TypeFilter; label: string }[] = [
    { value: 'ALL', label: 'All types' },
    { value: 'MOBILE', label: 'Mobile' },
    { value: 'FIXED', label: 'Fixed' },
];

type State =
    | { status: 'loading' }
    | { status: 'error'; message: string; code?: string }
    | { status: 'ready'; resources: ResourceSummary[] };

export function ResourceCatalogPage() {
    const [type, setType] = useState<TypeFilter>('ALL');
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [reloadKey, setReloadKey] = useState(0);
    const [state, setState] = useState<State>({ status: 'loading' });

    useEffect(() => {
        const timer = setTimeout(() => setDebouncedSearch(search), SEARCH_DEBOUNCE_MS);
        return () => clearTimeout(timer);
    }, [search]);

    useEffect(() => {
        // Aborting on cleanup means a slower, older response can never overwrite a newer one.
        const controller = new AbortController();
        setState({ status: 'loading' });
        resourceService
            .listResources({ type: type === 'ALL' ? undefined : type, q: debouncedSearch }, controller.signal)
            .then((resources) => setState({ status: 'ready', resources }))
            .catch((err: unknown) => {
                if (controller.signal.aborted) return;
                setState({ status: 'error', message: messageForError(err), code: codeOf(err) });
            });
        return () => controller.abort();
    }, [type, debouncedSearch, reloadKey]);

    const filtered = type !== 'ALL' || search !== '';
    const resetFilters = () => {
        setType('ALL');
        setSearch('');
        setDebouncedSearch('');
    };

    return (
        <>
            <PageHeader
                title="Resource catalog"
                subtitle="Find rooms, labs and equipment for your next session."
            />
            <div className="mb-5 grid gap-4 md:grid-cols-[1fr_auto]">
                <div className="flex flex-col gap-2">
                    <Label htmlFor="catalog-search">Search resources</Label>
                    <div className="relative">
                        <Search
                            className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
                            aria-hidden="true"
                        />
                        <Input
                            id="catalog-search"
                            type="search"
                            className="h-10 bg-card pl-9"
                            placeholder="Name, description or position"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    </div>
                </div>
                <div className="flex flex-col gap-2">
                    <span id="catalog-type-label" className="text-sm leading-none font-medium">Type</span>
                    <ToggleGroup
                        type="single"
                        variant="outline"
                        spacing={0}
                        value={type}
                        // Radix reports '' when the active item is clicked again: keep the current filter.
                        onValueChange={(value) => { if (value) setType(value as TypeFilter); }}
                        aria-labelledby="catalog-type-label"
                        className="h-10 bg-card"
                    >
                        {FILTERS.map((f) => (
                            <ToggleGroupItem key={f.value} value={f.value} className="h-10 px-4 data-[state=on]:bg-primary data-[state=on]:text-primary-foreground">
                                {f.label}
                            </ToggleGroupItem>
                        ))}
                    </ToggleGroup>
                </div>
            </div>

            {state.status === 'loading' && <LoadingView label="Loading resources…" rows={3} />}
            {state.status === 'error' && (
                <ErrorView message={state.message} code={state.code} onRetry={() => setReloadKey((k) => k + 1)} />
            )}
            {state.status === 'ready' && state.resources.length === 0 && (
                <EmptyView
                    message="No resources found"
                    text="Try a different search or resource type."
                    icon="search"
                    action={filtered ? { label: 'Reset filters', onClick: resetFilters } : undefined}
                />
            )}
            {state.status === 'ready' && state.resources.length > 0 && (
                <>
                    <div className="mb-3 flex items-center justify-between gap-3 text-sm text-muted-foreground">
                        <p aria-live="polite">
                            {state.resources.length} {state.resources.length === 1 ? 'resource' : 'resources'}
                        </p>
                        {filtered && (
                            <Button variant="ghost" className="h-10 text-foreground md:h-8" onClick={resetFilters}>
                                Reset filters
                            </Button>
                        )}
                    </div>
                    <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {state.resources.map((r) => (
                            <li key={r.id}>
                                <Card size="sm" className="h-full" role="article" aria-labelledby={`resource-${r.id}`}>
                                    <CardHeader>
                                        <Badge variant="secondary" className="mb-1">
                                            {r.type === 'MOBILE' ? 'Mobile' : 'Fixed'}
                                        </Badge>
                                        <CardTitle id={`resource-${r.id}`} className="text-base font-semibold">{r.name}</CardTitle>
                                        <p className="text-sm text-muted-foreground">{r.position ?? 'No location'}</p>
                                    </CardHeader>
                                    <CardFooter className="mt-auto items-center justify-between gap-2">
                                        <span className="text-sm font-semibold">CHF {r.pricePerHour} / hour</span>
                                        <Button asChild size="sm">
                                            <Link to={`/resources/${r.id}`} aria-label={`View slots for ${r.name}`}>View slots</Link>
                                        </Button>
                                    </CardFooter>
                                </Card>
                            </li>
                        ))}
                    </ul>
                </>
            )}
        </>
    );
}
