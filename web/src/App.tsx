import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';

// Eine einfache Platzhalter-Dashboard-Seite für eingeloggte Nutzer
const DashboardPage: React.FC = () => {
  const { user, logout } = useAuth();

  return (
    <div style={{ padding: '2rem', fontFamily: 'sans-serif' }}>
      <h1>Dashboard (BLE Booking)</h1>
      <p>Willkommen zurück, <strong>{user?.email}</strong>!</p>
      <p>Rolle: {user?.role}</p>
      <button onClick={logout} style={{ padding: '0.5rem 1rem', cursor: 'pointer' }}>
        Sign Out
      </button>
    </div>
  );
};

export function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Öffentliche Route für das Sign-in */}
          <Route path="/login" element={<LoginPage />} />

          {/* Geschützte Routen (nur nach Log-in erreichbar) */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<DashboardPage />} />
          </Route>

          {/* Fallback / Standard-Weiterleitung */}
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;