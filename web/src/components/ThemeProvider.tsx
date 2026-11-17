import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { STORAGE_KEY, ThemeContext, type ThemeChoice } from '@/components/theme-context';

const prefersDark = () => window.matchMedia('(prefers-color-scheme: dark)').matches;

function readChoice(): ThemeChoice {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === 'light' || saved === 'dark' || saved === 'system' ? saved : 'system';
}

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [choice, setChoiceState] = useState<ThemeChoice>(readChoice);
    const [systemDark, setSystemDark] = useState(prefersDark);

    useEffect(() => {
        const query = window.matchMedia('(prefers-color-scheme: dark)');
        const onChange = () => setSystemDark(query.matches);
        query.addEventListener('change', onChange);
        return () => query.removeEventListener('change', onChange);
    }, []);

    const resolved = choice === 'system' ? (systemDark ? 'dark' : 'light') : choice;

    useEffect(() => {
        document.documentElement.classList.toggle('dark', resolved === 'dark');
        document.documentElement.style.colorScheme = resolved;
    }, [resolved]);

    const setChoice = useCallback((next: ThemeChoice) => {
        localStorage.setItem(STORAGE_KEY, next);
        setChoiceState(next);
    }, []);

    const value = useMemo(() => ({ choice, resolved, setChoice }), [choice, resolved, setChoice]);
    return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
