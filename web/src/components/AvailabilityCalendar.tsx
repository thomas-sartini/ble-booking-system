import { Check, Circle, X } from 'lucide-react';
import { useState } from 'react';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import type { DayAvailability, Slot, SlotStatus } from '@/services/types';
import type { Selection } from '@/utils/booking';
import { cn } from '@/lib/utils';
import { isoTime, shortDateLabel, weekDayLabel } from '@/utils/week';

const STATUS_LABEL: Record<SlotStatus, string> = {
    AVAILABLE: 'Available',
    BOOKED: 'Booked',
    BOOKED_BY_ME: 'Your booking',
    PAST: 'Past',
};

type CellState = SlotStatus | 'SELECTED';

/** Colour plus pattern plus icon: a state is never told by colour alone. */
const CELL_STYLE: Record<CellState, string> = {
    AVAILABLE: 'border-input bg-card text-foreground',
    BOOKED: 'border-danger-foreground/30 bg-[repeating-linear-gradient(135deg,var(--danger)_0_4px,var(--card)_4px_8px)] text-danger-foreground',
    BOOKED_BY_ME: 'border-info-foreground/40 bg-info text-info-foreground',
    PAST: 'border-transparent bg-muted text-muted-foreground',
    SELECTED: 'border-primary bg-primary text-primary-foreground',
};

function CellIcon({ state }: { state: CellState }) {
    if (state === 'BOOKED') return <X className="size-3" aria-hidden="true" />;
    if (state === 'BOOKED_BY_ME') return <Check className="size-3" aria-hidden="true" />;
    if (state === 'SELECTED') return <Circle className="size-2 fill-current" aria-hidden="true" />;
    return null;
}

const toMinutes = (hhmm: string) => Number(hhmm.slice(0, 2)) * 60 + Number(hhmm.slice(3, 5));
const startMinutes = (slot: Slot) => toMinutes(isoTime(slot.start));
const pad = (n: number) => String(n).padStart(2, '0');
const hourLabel = (minutes: number) => `${pad(Math.floor(minutes / 60))}:${pad(minutes % 60)}`;

const ROW = 'h-6 max-md:h-10';

interface Props {
    days: DayAvailability[];
    /** When given, AVAILABLE slots are buttons. Without it the calendar is read-only. */
    onSelectSlot?: (day: DayAvailability, index: number) => void;
    selection?: Selection | null;
    /** Today in the resource's zone (yyyy-MM-dd): highlights that column. */
    today?: string | null;
}

/**
 * Week calendar: a time column and one column per day, one cell per slot over the union of the
 * week's windows. Below 768px a day tab strip shows one day at a time (all days stay in the DOM).
 */
export function AvailabilityCalendar({ days, onSelectSlot, selection, today }: Props) {
    const [activeDay, setActiveDay] = useState<string>(
        () => days.find((d) => d.date === today)?.date ?? days[0]?.date ?? '',
    );

    const all = days.flatMap((d) => d.slots);
    const slotMinutes = all.length ? toMinutes(isoTime(all[0].end)) - startMinutes(all[0]) || 30 : 30;
    const first = all.length ? Math.min(...all.map(startMinutes)) : 0;
    const last = all.length ? Math.max(...all.map((s) => toMinutes(isoTime(s.end)))) : 0;
    const axis = Array.from(
        { length: all.length ? Math.round((last - first) / slotMinutes) : 0 },
        (_, i) => first + i * slotMinutes,
    );

    return (
        <div>
            <ul aria-label="Legend" className="mb-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted-foreground">
                {([...Object.keys(STATUS_LABEL), 'SELECTED'] as CellState[]).map((s) => (
                    <li key={s} className="flex items-center gap-2">
                        <span
                            aria-hidden="true"
                            className={cn('flex size-5 items-center justify-center rounded-sm border', CELL_STYLE[s])}
                        >
                            <CellIcon state={s} />
                        </span>
                        {s === 'SELECTED' ? 'Selected' : STATUS_LABEL[s]}
                    </li>
                ))}
            </ul>

            <Tabs value={activeDay} onValueChange={setActiveDay}>
                <TabsList className="mb-3 grid h-auto w-full grid-cols-7 p-1 md:hidden" aria-label="Choose day">
                    {days.map((day) => (
                        <TabsTrigger
                            key={day.date}
                            value={day.date}
                            aria-label={`${weekDayLabel(day.date)} ${shortDateLabel(day.date)}${day.date === today ? ', today' : ''}`}
                            className={cn(
                                'h-12 min-w-0 flex-col gap-0 px-0 text-xs',
                                day.date === today && 'ring-2 ring-primary/60',
                            )}
                        >
                            <span>{weekDayLabel(day.date)}</span>
                            <strong className="text-base font-semibold">{day.date.slice(8)}</strong>
                        </TabsTrigger>
                    ))}
                </TabsList>

                <TabsContent value={activeDay} className="mt-0">
                    <div className="overflow-x-auto rounded-xl border bg-card shadow-xs">
                        <div className="grid min-w-184 grid-cols-[3.5rem_repeat(7,minmax(5.5rem,1fr))] max-md:min-w-0 max-md:grid-cols-[3rem_1fr]">
                            <div aria-hidden="true">
                                <div className="h-14 border-b" />
                                <div className="flex flex-col gap-0.5 p-0.5">
                                    {axis.map((minutes) => (
                                        <span key={minutes} className={cn(ROW, 'flex items-center justify-end pr-2 text-xs text-muted-foreground')}>
                                            {minutes % 60 === 0 ? hourLabel(minutes) : ''}
                                        </span>
                                    ))}
                                </div>
                            </div>

                            {days.map((day) => {
                                const isToday = day.date === today;
                                const byStart = new Map(day.slots.map((slot, index) => [startMinutes(slot), { slot, index }]));
                                const heading = (
                                    <h3
                                        aria-current={isToday ? 'date' : undefined}
                                        className={cn(
                                            'flex h-14 flex-col items-center justify-center border-b text-sm leading-tight font-semibold',
                                            isToday && 'bg-primary text-primary-foreground',
                                        )}
                                    >
                                        {weekDayLabel(day.date)}
                                        <small className={cn('text-xs font-normal', isToday ? 'text-primary-foreground/80' : 'text-muted-foreground')}>
                                            {shortDateLabel(day.date)}
                                        </small>
                                    </h3>
                                );
                                return (
                                    <section
                                        key={day.date}
                                        aria-label={day.date}
                                        className={cn('min-w-0 border-l', day.date !== activeDay && 'max-md:hidden')}
                                    >
                                        {day.slots.length === 0 ? (
                                            <>
                                                <Tooltip>
                                                    <TooltipTrigger asChild>{heading}</TooltipTrigger>
                                                    <TooltipContent>Not bookable</TooltipContent>
                                                </Tooltip>
                                                <p className="sr-only">No bookable slots</p>
                                            </>
                                        ) : heading}
                                        <ul className="flex flex-col gap-0.5 p-0.5">
                                            {axis.map((minutes) => {
                                                const entry = byStart.get(minutes);
                                                if (!entry) return <li key={minutes} aria-hidden="true" className={ROW} />;
                                                const { slot, index } = entry;
                                                const available = slot.status === 'AVAILABLE';
                                                const selected =
                                                    !!selection && selection.date === day.date && index >= selection.from && index <= selection.to;
                                                const state: CellState = selected ? 'SELECTED' : slot.status;
                                                const label = selected ? 'Selected' : STATUS_LABEL[slot.status];
                                                const time = `${isoTime(slot.start)}–${isoTime(slot.end)}`;
                                                const content = (
                                                    <>
                                                        <CellIcon state={state} />
                                                        <span className="slot-time sr-only">{time}</span>
                                                        <span className="slot-status sr-only">{label}</span>
                                                    </>
                                                );
                                                return (
                                                    <Tooltip key={minutes}>
                                                        <TooltipTrigger asChild>
                                                            <li
                                                                className={cn(
                                                                    ROW,
                                                                    'flex items-center justify-center rounded-sm border text-xs',
                                                                    CELL_STYLE[state],
                                                                    !available && 'cursor-not-allowed',
                                                                )}
                                                                data-slot="slot"
                                                                data-status={slot.status}
                                                                data-selected={selected}
                                                                aria-disabled={available ? undefined : true}
                                                            >
                                                                {available && onSelectSlot ? (
                                                                    <button
                                                                        type="button"
                                                                        aria-pressed={selected}
                                                                        onClick={() => onSelectSlot(day, index)}
                                                                        className={cn(
                                                                            'flex size-full cursor-pointer items-center justify-center rounded-[3px] outline-none focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-ring',
                                                                            selected ? 'hover:bg-primary/85' : 'hover:bg-accent',
                                                                        )}
                                                                    >
                                                                        {content}
                                                                    </button>
                                                                ) : content}
                                                            </li>
                                                        </TooltipTrigger>
                                                        <TooltipContent>{`${time} · ${label}`}</TooltipContent>
                                                    </Tooltip>
                                                );
                                            })}
                                        </ul>
                                    </section>
                                );
                            })}
                        </div>
                    </div>
                </TabsContent>
            </Tabs>
        </div>
    );
}
