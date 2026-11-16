import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './AppLayout.css';

export const AppLayout: React.FC = () => {
    const { user, logout } = useAuth();
    const displayName = [user?.firstName, user?.lastName].filter(Boolean).join(' ') || user?.email;

    return (
        <div className="app-shell">
            <header className="app-header">
                <div className="app-brand">
                    <span className="ble-badge">BLE</span>
                    <span>Booking System</span>
                </div>
                <nav className="app-nav" aria-label="Main">
                    <NavLink to="/resources">Resources</NavLink>
                    <NavLink to="/bookings">My bookings</NavLink>
                </nav>
                <div className="app-user">
                    <span className="app-user-name">{displayName}</span>
                    <button type="button" onClick={() => void logout()}>Sign out</button>
                </div>
            </header>
            <main className="app-main">
                <Outlet />
            </main>
        </div>
    );
};
