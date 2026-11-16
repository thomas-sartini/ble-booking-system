import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/AppLayout';
import { LoginPage } from './pages/LoginPage';
import { ResourceCatalogPage } from './pages/ResourceCatalogPage';
import { ResourceDetailsPage } from './pages/ResourceDetailsPage';
import { BookingsPlaceholderPage } from './pages/BookingsPlaceholderPage';

export const AppRoutes: React.FC = () => (
    <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route element={<ProtectedRoute />}>
            <Route element={<AppLayout />}>
                <Route path="/resources" element={<ResourceCatalogPage />} />
                <Route path="/resources/:id" element={<ResourceDetailsPage />} />
                <Route path="/bookings" element={<BookingsPlaceholderPage />} />
                <Route path="/dashboard" element={<Navigate to="/resources" replace />} />
            </Route>
        </Route>

        <Route path="*" element={<Navigate to="/resources" replace />} />
    </Routes>
);

export function App() {
    return (
        <AuthProvider>
            <Router>
                <AppRoutes />
            </Router>
        </AuthProvider>
    );
}

export default App;
