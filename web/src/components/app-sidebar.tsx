import type { ComponentProps } from 'react';
import { Link } from 'react-router-dom';
import { NavMain } from '@/components/nav-main';
import { NavUser } from '@/components/nav-user';
import {
    Sidebar, SidebarContent, SidebarFooter, SidebarHeader, SidebarMenu, SidebarMenuButton, SidebarMenuItem,
    useSidebar,
} from '@/components/ui/sidebar';

/** Brand header, navigation and the user menu: the dashboard-01 sidebar, with our entries. */
export function AppSidebar(props: ComponentProps<typeof Sidebar>) {
    const { setOpenMobile } = useSidebar();
    return (
        <Sidebar collapsible="offcanvas" {...props}>
            <SidebarHeader>
                <SidebarMenu>
                    <SidebarMenuItem>
                        <SidebarMenuButton asChild size="lg" className="data-[slot=sidebar-menu-button]:p-1.5!">
                            <Link to="/resources" onClick={() => setOpenMobile(false)}>
                                <span className="flex h-8 items-center rounded-md bg-primary px-2 text-sm font-bold text-primary-foreground">BLE</span>
                                <span className="grid flex-1 text-left leading-tight">
                                    <span className="text-base font-semibold">Booking System</span>
                                    <span className="text-[0.65rem] leading-tight font-normal tracking-wide text-muted-foreground uppercase">
                                        University Resource Services
                                    </span>
                                </span>
                            </Link>
                        </SidebarMenuButton>
                    </SidebarMenuItem>
                </SidebarMenu>
            </SidebarHeader>
            <SidebarContent>
                <NavMain />
            </SidebarContent>
            <SidebarFooter>
                <NavUser />
            </SidebarFooter>
        </Sidebar>
    );
}
