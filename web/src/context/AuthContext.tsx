import React, { createContext, useContext, useEffect, useState } from 'react';
import { authService, isValidUser, type User } from '../services/authService';
import { tokenStore } from '../services/tokenStore';

interface AuthContextType {
    user: User | null;
    token: string | null;
    login: (identifier: string, pass: string) => Promise<void>;
    logout: () => Promise<void>;
    isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function loadPersistedUser(): User | null {
    const saved = tokenStore.getUserJson();
    if (!saved) return null;
    try {
        const parsed: unknown = JSON.parse(saved);
        // Sessions stored by the old login (no numeric id / unknown role) are discarded.
        return isValidUser(parsed) ? parsed : null;
    } catch {
        return null;
    }
}

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(() => {
        const persisted = loadPersistedUser();
        if (!persisted) tokenStore.clear();
        return persisted;
    });
    const [token, setToken] = useState<string | null>(() => (user ? tokenStore.getAccessToken() : null));

    useEffect(() => {
        tokenStore.onSessionExpired(() => {
            setUser(null);
            setToken(null);
        });
        return () => tokenStore.onSessionExpired(null);
    }, []);

    const login = async (identifier: string, pass: string) => {
        const loggedIn = await authService.login(identifier, pass);
        setUser(loggedIn);
        setToken(tokenStore.getAccessToken());
    };

    const logout = async () => {
        await authService.logout();
        setUser(null);
        setToken(null);
    };

    return (
        <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token && !!user }}>
            {children}
        </AuthContext.Provider>
    );
};

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within an AuthProvider');
    return context;
};
