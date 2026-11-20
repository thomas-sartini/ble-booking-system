import { Outlet } from 'react-router-dom';
import { AppSidebar } from '@/components/app-sidebar';
import { SiteHeader } from '@/components/site-header';
import { SidebarInset, SidebarProvider } from '@/components/ui/sidebar';
import { Toaster } from '@/components/ui/sonner';
import { TooltipProvider } from '@/components/ui/tooltip';

/** Authenticated shell, laid out like shadcn's dashboard-01: inset sidebar, site header, content. */
export function AppLayout() {
    return (
        <TooltipProvider delayDuration={150}>
            <SidebarProvider className="[--header-height:--spacing(12)]">
                <a
                    href="#main"
                    className="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-100 focus:rounded-md focus:bg-primary focus:px-4 focus:py-2 focus:text-primary-foreground"
                >
                    Skip to main content
                </a>
                <AppSidebar variant="inset" />
                <SidebarInset>
                    <SiteHeader />
                    <main id="main" tabIndex={-1} className="mx-auto w-full max-w-300 flex-1 px-4 pt-6 pb-24 outline-none md:px-6 md:pt-8">
                        <Outlet />
                    </main>
                </SidebarInset>
                <Toaster position="top-center" />
            </SidebarProvider>
        </TooltipProvider>
    );
}
