import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ThemeToggle } from '@/components/ThemeToggle';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useAuth } from '@/context/AuthContext';
import { ApiError } from '@/services/apiClient';
import { messageForError } from '@/services/errorMessages';

export function LoginPage() {
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
            navigate('/resources');
        } catch (err: unknown) {
            setError(err instanceof ApiError && err.status === 401
                ? 'Email or password is incorrect.'
                : messageForError(err));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex min-h-svh flex-col items-center px-4 py-8">
            <div className="self-end"><ThemeToggle /></div>
            <main className="flex w-full flex-1 flex-col items-center justify-center">
                <div className="mb-8 flex items-center gap-3 text-lg font-medium">
                    <span className="flex h-9 items-center rounded-md bg-primary px-2.5 text-sm font-bold text-primary-foreground">BLE</span>
                    Booking System
                </div>

                <Card className="w-full max-w-115 gap-6 p-2 sm:p-4">
                    <CardHeader>
                        <h1 className="text-2xl font-medium tracking-tight">Sign in</h1>
                        <CardDescription>Access your bookings and university resources.</CardDescription>
                    </CardHeader>
                    <CardContent className="flex flex-col gap-6">
                        {error && (
                            <Alert variant="destructive" className="border-danger-foreground/25 bg-danger">
                                <AlertTitle className="font-semibold">{error}</AlertTitle>
                                <AlertDescription className="text-foreground/80">
                                    Check your credentials and try again.
                                </AlertDescription>
                            </Alert>
                        )}

                        <form onSubmit={handleSubmit} className="flex flex-col gap-5">
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="login-email">Institutional email</Label>
                                <Input
                                    id="login-email"
                                    type="email"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    placeholder="firstname.lastname@ble.edu"
                                    className="h-10"
                                />
                            </div>
                            <div className="flex flex-col gap-2">
                                <Label htmlFor="login-password">Password</Label>
                                <Input
                                    id="login-password"
                                    type="password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    placeholder="••••••••••••"
                                    className="h-10"
                                />
                                <a
                                    href="#forgot"
                                    onClick={(e) => e.preventDefault()}
                                    className="self-end rounded-sm text-sm font-medium underline-offset-4 hover:underline"
                                >
                                    Forgot password?
                                </a>
                            </div>
                            <Button type="submit" size="lg" className="w-full" disabled={loading}>
                                {loading ? 'Signing in...' : 'Sign In'}
                            </Button>
                        </form>

                        <p className="text-sm text-muted-foreground">
                            New to BLE?{' '}
                            <a
                                href="#register"
                                onClick={(e) => e.preventDefault()}
                                className="rounded-sm underline underline-offset-4 hover:text-foreground"
                            >
                                Register with your invitation link.
                            </a>
                        </p>
                    </CardContent>
                </Card>
            </main>
            <footer className="mt-8 text-xs text-muted-foreground">
                University Resource Services · Secure institutional access
            </footer>
        </div>
    );
}
