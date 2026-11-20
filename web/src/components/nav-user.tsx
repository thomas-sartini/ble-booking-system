import { ChevronsUpDown, LogOut } from 'lucide-react';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import {
    DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { SidebarMenu, SidebarMenuButton, SidebarMenuItem, useSidebar } from '@/components/ui/sidebar';
import { useAuth } from '@/context/AuthContext';
import type { Role } from '@/services/types';

const ROLE_LABEL: Record<Role, string> = { STUDENT: 'Student', STAFF: 'Staff', ADMIN: 'Admin' };

function initialsOf(name: string): string {
    const parts = name.trim().split(/[\s@.]+/).filter(Boolean);
    return ((parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[1][0] : '')).toUpperCase() || '?';
}

/** Avatar with initials, name and role; the menu holds sign-out. */
export function NavUser() {
    const { user, logout } = useAuth();
    const { isMobile } = useSidebar();
    if (!user) return null;
    const name = [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email;

    const identity = (
        <>
            <Avatar className="size-8 rounded-lg" aria-hidden="true">
                <AvatarFallback className="rounded-lg bg-primary text-xs font-semibold text-primary-foreground">
                    {initialsOf(name)}
                </AvatarFallback>
            </Avatar>
            <span className="grid flex-1 text-left text-sm leading-tight">
                <span className="truncate font-medium">{name}</span>
                <span className="truncate text-xs text-muted-foreground">{ROLE_LABEL[user.role]}</span>
            </span>
        </>
    );

    return (
        <SidebarMenu>
            <SidebarMenuItem>
                <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                        <SidebarMenuButton size="lg" className="data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground">
                            {identity}
                            <ChevronsUpDown className="ml-auto size-4" aria-hidden="true" />
                        </SidebarMenuButton>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent
                        className="w-(--radix-dropdown-menu-trigger-width) min-w-56 rounded-lg"
                        side={isMobile ? 'bottom' : 'right'}
                        align="end"
                        sideOffset={4}
                    >
                        <DropdownMenuLabel className="flex flex-col gap-1 px-2 py-1.5 font-normal">
                            <span className="truncate text-sm font-medium">{name}</span>
                            <span className="truncate text-xs text-muted-foreground">{user.email}</span>
                            <Badge variant="secondary" className="mt-1 w-fit">{ROLE_LABEL[user.role]}</Badge>
                        </DropdownMenuLabel>
                        <DropdownMenuSeparator />
                        <DropdownMenuItem onSelect={() => void logout()}>
                            <LogOut aria-hidden="true" />
                            Sign out
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                </DropdownMenu>
            </SidebarMenuItem>
        </SidebarMenu>
    );
}
