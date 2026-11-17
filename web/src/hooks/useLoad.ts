import { useCallback, useEffect, useRef, useState } from 'react';
import { codeOf, messageForError } from '../services/errorMessages';

export type LoadState<T> =
    | { status: 'loading' }
    | { status: 'error'; message: string; code?: string }
    | { status: 'ready'; data: T };

/**
 * Loads data when `key` changes or `reload()` is called. A superseded request is aborted,
 * so a slow old response can never overwrite a newer one.
 */
export function useLoad<T>(loader: (signal: AbortSignal) => Promise<T>, key: string) {
    const [state, setState] = useState<LoadState<T>>({ status: 'loading' });
    const [reloadKey, setReloadKey] = useState(0);
    const loaderRef = useRef(loader);

    useEffect(() => {
        loaderRef.current = loader;
    });

    useEffect(() => {
        const controller = new AbortController();
        setState({ status: 'loading' });
        loaderRef.current(controller.signal)
            .then((data) => setState({ status: 'ready', data }))
            .catch((err: unknown) => {
                if (controller.signal.aborted) return;
                setState({ status: 'error', message: messageForError(err), code: codeOf(err) });
            });
        return () => controller.abort();
    }, [key, reloadKey]);

    const reload = useCallback(() => setReloadKey((k) => k + 1), []);
    return { state, reload };
}
