import { Fragment } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { ThemeToggle } from '@/components/ThemeToggle';
import {
    Breadcrumb, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { Separator } from '@/components/ui/separator';
import { SidebarTrigger } from '@/components/ui/sidebar';

interface Crumb { label: string; to?: string }

function breadcrumbFor(pathname: string): Crumb[] {
    const root = { label: 'My workspace' };
    if (pathname.startsWith('/bookings')) return [root, { label: 'My bookings' }];
    if (/^\/resources\/[^/]+/.test(pathname)) {
        return [root, { label: 'Resource catalog', to: '/resources' }, { label: 'Resource details' }];
    }
    return [root, { label: 'Resource catalog' }];
}

export function SiteHeader() {
    const crumbs = breadcrumbFor(useLocation().pathname);
    return (
        <header className="sticky top-0 z-20 flex h-(--header-height) shrink-0 items-center gap-2 border-b bg-background/90 backdrop-blur md:rounded-t-xl">
            <div className="flex w-full items-center gap-1 px-4 lg:gap-2 lg:px-6">
                <SidebarTrigger className="-ml-1 size-10" aria-label="Toggle navigation" />
                <Separator orientation="vertical" className="mx-2 data-[orientation=vertical]:h-4" />
                <Breadcrumb className="min-w-0 flex-1">
                    <BreadcrumbList className="max-md:flex-nowrap">
                        {crumbs.map((c, i) => (
                            <Fragment key={c.label}>
                                {i > 0 && <BreadcrumbSeparator className="max-md:hidden" />}
                                {/* Phones: only the current page, so the 48px header never wraps. */}
                                <BreadcrumbItem className={i < crumbs.length - 1 ? 'max-md:hidden' : undefined}>
                                    {i === crumbs.length - 1 ? (
                                        <BreadcrumbPage>{c.label}</BreadcrumbPage>
                                    ) : c.to ? (
                                        <BreadcrumbLink asChild><Link to={c.to}>{c.label}</Link></BreadcrumbLink>
                                    ) : (
                                        <span>{c.label}</span>
                                    )}
                                </BreadcrumbItem>
                            </Fragment>
                        ))}
                    </BreadcrumbList>
                </Breadcrumb>
                <ThemeToggle />
            </div>
        </header>
    );
}
