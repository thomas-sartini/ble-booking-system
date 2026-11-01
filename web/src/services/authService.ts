// Mock User types & credentials
export interface User {
    id: string;
    email: string;
    role: 'Student' | 'Lecturer' | 'Admin';
}

export const authService = {
    async login(email: string, password: string): Promise<{ user: User; token: string }> {
        // Simulating network latency
        await new Promise((resolve) => setTimeout(resolve, 600));

        // Simple mock logic
        if (email && password.length >= 6) {
            return {
                user: { id: '1', email, role: 'Student' },
                token: 'mock-jwt-token-xyz123'
            };
        }
        throw new Error('Invalid username/email or password');
    },

    async logout(): Promise<void> {
        await new Promise((resolve) => setTimeout(resolve, 200));
        localStorage.removeItem('authToken');
        localStorage.removeItem('authUser');
    }
};