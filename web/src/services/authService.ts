const API_BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

// User types entsprechend der echten Anwendung
export interface User {
    id?: string;
    email: string;
    role?: 'Student' | 'Lecturer' | 'Admin' | 'USER' | string;
}

export const authService = {
    async login(identifier: string, password: string): Promise<{ user: User; token: string }> {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ identifier, password }),
        });

        if (!response.ok) {
            throw new Error('Anmeldung fehlgeschlagen. Bitte Zugangsdaten prüfen.');
        }

        const data = await response.json();

        // Falls das Backend einen Refresh-Token liefert, diesen direkt für den Logout sichern
        if (data.refreshToken) {
            localStorage.setItem('refreshToken', data.refreshToken);
        }

        return {
            user: {
                email: identifier,
                role: data.role || 'Student'
            },
            token: data.token || data.accessToken || 'live-token'
        };
    },

    async logout(): Promise<void> {
        const refreshToken = localStorage.getItem('refreshToken');

        try {
            await fetch(`${API_BASE_URL}/auth/logout`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ refreshToken }),
            });
        } catch (error) {
            console.error('Logout-Request an Backend fehlgeschlagen, bereinige lokalen Speicher trotzdem.', error);
        } finally {
            localStorage.removeItem('authToken');
            localStorage.removeItem('authUser');
            localStorage.removeItem('refreshToken');
        }
    }
};