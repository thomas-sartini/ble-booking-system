import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { EmptyView, ErrorView, LoadingView } from '../components/StateViews';
import { messageForError } from '../services/errorMessages';
import { resourceService } from '../services/resourceService';
import type { ResourceSummary, ResourceType } from '../services/types';
import './ResourceCatalogPage.css';

const SEARCH_DEBOUNCE_MS = 300;

type TypeFilter = 'ALL' | ResourceType;
const FILTERS: { value: TypeFilter; label: string }[] = [
    { value: 'ALL', label: 'All' },
    { value: 'MOBILE', label: 'Mobile' },
    { value: 'FIXED', label: 'Fixed' },
];

type State =
    | { status: 'loading' }
    | { status: 'error'; message: string }
    | { status: 'ready'; resources: ResourceSummary[] };

export const ResourceCatalogPage: React.FC = () => {
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
                setState({ status: 'error', message: messageForError(err) });
            });
        return () => controller.abort();
    }, [type, debouncedSearch, reloadKey]);

    return (
        <>
            <h1>Resources</h1>
            <div className="catalog-controls">
                <div className="catalog-filters" role="group" aria-label="Resource type">
                    {FILTERS.map((f) => (
                        <button
                            key={f.value}
                            type="button"
                            aria-pressed={type === f.value}
                            className={type === f.value ? 'active' : ''}
                            onClick={() => setType(f.value)}
                        >
                            {f.label}
                        </button>
                    ))}
                </div>
                <input
                    type="search"
                    aria-label="Search resources"
                    placeholder="Search by name, description or position"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
            </div>

            {state.status === 'loading' && <LoadingView label="Loading resources…" />}
            {state.status === 'error' && (
                <ErrorView message={state.message} onRetry={() => setReloadKey((k) => k + 1)} />
            )}
            {state.status === 'ready' && state.resources.length === 0 && <EmptyView message="No resources found" />}
            {state.status === 'ready' && state.resources.length > 0 && (
                <ul className="catalog-grid">
                    {state.resources.map((r) => (
                        <li key={r.id}>
                            <Link to={`/resources/${r.id}`} className="resource-card">
                                <span className="resource-name">{r.name}</span>
                                <span className={`resource-type type-${r.type.toLowerCase()}`}>
                                    {r.type === 'MOBILE' ? 'Mobile' : 'Fixed'}
                                </span>
                                <span className="resource-position">{r.position ?? 'No location'}</span>
                                <span className="resource-price">CHF {r.pricePerHour} / h</span>
                            </Link>
                        </li>
                    ))}
                </ul>
            )}
        </>
    );
};
