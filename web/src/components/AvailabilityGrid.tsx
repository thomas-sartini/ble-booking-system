import React from 'react';
import type { DayAvailability, SlotStatus } from '../services/types';
import type { Selection } from '../utils/booking';
import { isoTime, shortDateLabel, weekDayLabel } from '../utils/week';
import './AvailabilityGrid.css';

const STATUS_LABEL: Record<SlotStatus, string> = {
    AVAILABLE: 'Available',
    BOOKED: 'Booked',
    BOOKED_BY_ME: 'Your booking',
    PAST: 'Past',
};

interface Props {
    days: DayAvailability[];
    /** When given, AVAILABLE slots are buttons. Without it the grid is read-only. */
    onSelectSlot?: (day: DayAvailability, index: number) => void;
    selection?: Selection | null;
}

export const AvailabilityGrid: React.FC<Props> = ({ days, onSelectSlot, selection }) => (
    <div>
        <ul className="availability-legend" aria-label="Legend">
            {(Object.keys(STATUS_LABEL) as SlotStatus[]).map((s) => (
                <li key={s}>
                    <span className={`slot-swatch slot-${s.toLowerCase()}`} aria-hidden="true" />
                    {STATUS_LABEL[s]}
                </li>
            ))}
        </ul>
        <div className="availability-grid">
            {days.map((day) => (
                <section key={day.date} className="availability-day" aria-label={day.date}>
                    <h3>
                        {weekDayLabel(day.date)} <small>{shortDateLabel(day.date)}</small>
                    </h3>
                    {day.slots.length === 0 ? (
                        <p className="availability-none">No bookable slots</p>
                    ) : (
                        <ul>
                            {day.slots.map((slot, index) => {
                                const available = slot.status === 'AVAILABLE';
                                const selected =
                                    !!selection && selection.date === day.date && index >= selection.from && index <= selection.to;
                                const content = (
                                    <>
                                        <span className="slot-time">
                                            {isoTime(slot.start)}–{isoTime(slot.end)}
                                        </span>
                                        <span className="slot-status">{selected ? 'Selected' : STATUS_LABEL[slot.status]}</span>
                                    </>
                                );
                                return (
                                    <li
                                        key={slot.start}
                                        className={`slot slot-${slot.status.toLowerCase()}${selected ? ' slot-selected' : ''}`}
                                        aria-disabled={available ? undefined : true}
                                    >
                                        {available && onSelectSlot ? (
                                            <button
                                                type="button"
                                                className="slot-button"
                                                aria-pressed={selected}
                                                onClick={() => onSelectSlot(day, index)}
                                            >
                                                {content}
                                            </button>
                                        ) : content}
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </section>
            ))}
        </div>
    </div>
);
