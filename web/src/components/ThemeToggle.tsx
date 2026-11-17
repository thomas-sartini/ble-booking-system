import { Monitor, Moon, Sun } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
    DropdownMenu, DropdownMenuContent, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { useTheme, type ThemeChoice } from '@/components/theme-context';

/** Light / dark / system switch. The icon shows the theme that is currently displayed. */
export function ThemeToggle() {
    const { choice, resolved, setChoice } = useTheme();
    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="size-10" aria-label="Change theme">
                    {resolved === 'dark' ? <Moon aria-hidden="true" /> : <Sun aria-hidden="true" />}
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                <DropdownMenuRadioGroup value={choice} onValueChange={(v) => setChoice(v as ThemeChoice)}>
                    <DropdownMenuRadioItem value="light"><Sun aria-hidden="true" />Light</DropdownMenuRadioItem>
                    <DropdownMenuRadioItem value="dark"><Moon aria-hidden="true" />Dark</DropdownMenuRadioItem>
                    <DropdownMenuRadioItem value="system"><Monitor aria-hidden="true" />System</DropdownMenuRadioItem>
                </DropdownMenuRadioGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
