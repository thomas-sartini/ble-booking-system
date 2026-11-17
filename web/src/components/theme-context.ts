import { createContext, useContext } from 'react';

export type ThemeChoice = 'light' | 'dark' | 'system';
export const STORAGE_KEY = 'theme';

export interface ThemeContextValue {
    choice: ThemeChoice;
    /** What is actually shown: "system" resolved against the OS setting. */
    resolved: 'light' | 'dark';
    setChoice: (choice: ThemeChoice) => void;
}

// Without a provider (e.g. in isolated component tests) the app simply stays light.
export const ThemeContext = createContext<ThemeContextValue>({ choice: 'light', resolved: 'light', setChoice: () => {} });

export const useTheme = () => useContext(ThemeContext);
