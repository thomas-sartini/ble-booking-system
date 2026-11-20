import { useRef, type ReactNode } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Button } from '@/components/ui/button';
import {
    Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle,
} from '@/components/ui/dialog';

interface Props {
    title: string;
    description?: string;
    children: ReactNode;
    confirmLabel: string;
    dismissLabel?: string;
    busy?: boolean;
    error?: string | null;
    onConfirm: () => void;
    onDismiss: () => void;
}

/**
 * Modal confirm dialog (shadcn Dialog): focus moves in and is trapped, Esc dismisses (not while busy),
 * focus returns to the trigger on close.
 */
export function ConfirmDialog({
    title, description, children, confirmLabel, dismissLabel = 'Cancel', busy = false, error, onConfirm, onDismiss,
}: Props) {
    // Opened by state, not by a DialogTrigger: remember the opener so focus can return to it.
    const opener = useRef<Element | null>(document.activeElement);
    return (
        <Dialog open onOpenChange={(open) => { if (!open && !busy) onDismiss(); }}>
            <DialogContent
                showCloseButton={false}
                onEscapeKeyDown={(e) => { if (busy) e.preventDefault(); }}
                onInteractOutside={(e) => { if (busy) e.preventDefault(); }}
                onCloseAutoFocus={(e) => {
                    e.preventDefault();
                    if (opener.current instanceof HTMLElement && opener.current.isConnected) opener.current.focus();
                }}
            >
                <DialogHeader>
                    <DialogTitle>{title}</DialogTitle>
                    <DialogDescription>{description ?? 'Please check the details below.'}</DialogDescription>
                </DialogHeader>
                {children}
                {error && (
                    <Alert variant="destructive" className="border-danger-foreground/25 bg-danger">
                        <AlertDescription className="text-danger-foreground">{error}</AlertDescription>
                    </Alert>
                )}
                <DialogFooter>
                    <Button variant="outline" onClick={onDismiss} disabled={busy}>{dismissLabel}</Button>
                    <Button onClick={onConfirm} disabled={busy}>{busy ? 'Please wait…' : confirmLabel}</Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
