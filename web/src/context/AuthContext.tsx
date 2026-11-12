import React, { createContext, useContext, useState } from 'react';
import { authService, type User } from '../services/authService';

interface AuthContextType {
    user: User | null;
    token: string | null;
    login: (email: string, pass: string) => Promise<void>;
    logout: () => Promise<void>;
    isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [user, setUser] = useState<User | null>(() => {
        const saved = localStorage.getItem('authUser');
        return saved ? JSON.parse(saved) : null;
    });
    const [token, setToken] = useState<string | null>(localStorage.getItem('authToken'));

    const login = async (email: string, pass: string) => {
        const response = await authService.login(email, pass);
        setUser(response.user);
        setToken(response.token);
        localStorage.setItem('authToken', response.token);
        localStorage.setItem('authUser', JSON.stringify(response.user));
    };

    const logout = async () => {
        await authService.logout();
        setUser(null);
        setToken(null);
    };

    return (
        <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) throw new Error('useAuth must be used within an AuthProvider');
    return context;
};