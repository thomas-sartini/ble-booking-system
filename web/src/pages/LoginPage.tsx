import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './LoginPage.css';

export const LoginPage: React.FC = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);

    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault();
        setError(null);

        if (!email || !password) {
            setError('Email or password is incorrect.');
            return;
        }

        try {
            setLoading(true);
            await login(email, password);
            navigate('/dashboard');
        } catch (err: any) {
            setError(err.message || 'Authentication failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page-wrapper">
            {/* Header mit Logo */}
            <div className="login-header">
                <span className="ble-badge">BLE</span>
                <span>Booking System</span>
            </div>

            <div className="login-card">
                <h1>Sign in</h1>
                <p className="login-subtitle">Access your bookings and university resources.</p>

                {error && (
                    <div className="error-banner">
                        <div className="error-title">{error}</div>
                        <div className="error-desc">Check your credentials and try again.</div>
                    </div>
                )}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Institutional email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="alex.morgan@ble.edu"
                        />
                    </div>

                    <div className="form-group">
                        <div className="label-row">
                            <label>Password</label>
                            <a href="#forgot" onClick={(e) => e.preventDefault()} className="forgot-link">Forgot password?</a>
                        </div>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="••••••••••••"
                        />
                    </div>

                    <button type="submit" className="submit-btn" disabled={loading}>
                        {loading ? 'Signing in...' : 'Sign In'}
                    </button>
                </form>

                <div className="card-footer">
                    New to BLE? <a href="#register" onClick={(e) => e.preventDefault()}>Register with your invitation link.</a>
                </div>
            </div>
        </div>
    );
};