// User types & credentials
export interface User {
    id: string;
    email: string;
    role: 'STUDENT' | 'STAFF' | 'ADMIN' | 'Student' | 'Lecturer' | 'Admin';
}

const API_BASE_URL = 'https://ble-booking-system-production.up.railway.app';

export const authService = {
    async login(email: string, password: string): Promise<{ user: User; token: string }> {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ email, password }),
        });

        if (!response.ok) {
            // Catch error message from backend if available, otherwise use fallback
            const errorData = await response.json().catch(() => ({}));
            throw new Error(errorData.message || 'Invalid username/email or password');
        }

        const data = await response.json();

        // Save token and user data directly in localStorage (if returned by backend)
        if (data.token) {
            localStorage.setItem('authToken', data.token);
        }
        if (data.user) {
            localStorage.setItem('authUser', JSON.stringify(data.user));
        }

        return data;
    },

    async logout(): Promise<void> {
        try {
            await fetch(`${API_BASE_URL}/auth/logout`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${localStorage.getItem('authToken')}`
                }
            });
        } catch (e) {
            // Ignore errors during backend logout, main goal is local cleanup
        }

        localStorage.removeItem('authToken');
        localStorage.removeItem('authUser');
    }
};