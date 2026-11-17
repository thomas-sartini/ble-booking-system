import { CalendarDays, LayoutGrid, Receipt } from 'lucide-react';
import type { ReactNode } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import {
    SidebarGroup, SidebarGroupContent, SidebarGroupLabel, SidebarMenu, SidebarMenuButton, SidebarMenuItem, useSidebar,
} from '@/components/ui/sidebar';

/** Nav entry: the active route is highlighted, and the mobile drawer closes on navigation. */
function NavItem({ to, icon, children }: { to: string; icon: ReactNode; children: ReactNode }) {
    const { setOpenMobile } = useSidebar();
    const { pathname } = useLocation();
    return (
        <SidebarMenuItem>
            <SidebarMenuButton asChild tooltip={String(children)} isActive={pathname === to || pathname.startsWith(`${to}/`)}>
                <NavLink to={to} onClick={() => setOpenMobile(false)}>
                    {icon}
                    <span>{children}</span>
                </NavLink>
            </SidebarMenuButton>
        </SidebarMenuItem>
    );
}

export function NavMain() {
    return (
        <SidebarGroup>
            <SidebarGroupLabel>Workspace</SidebarGroupLabel>
            <SidebarGroupContent>
                <nav aria-label="Main">
                    <SidebarMenu>
                        <NavItem to="/resources" icon={<LayoutGrid />}>Resource catalog</NavItem>
                        <NavItem to="/bookings" icon={<CalendarDays />}>My bookings</NavItem>
                        <SidebarMenuItem>
                            <SidebarMenuButton disabled aria-disabled="true" className="text-muted-foreground disabled:opacity-100">
                                <Receipt />
                                <span>Invoices</span>
                                <Badge variant="secondary" className="ml-auto">Coming soon</Badge>
                            </SidebarMenuButton>
                        </SidebarMenuItem>
                    </SidebarMenu>
                </nav>
            </SidebarGroupContent>
        </SidebarGroup>
    );
}
