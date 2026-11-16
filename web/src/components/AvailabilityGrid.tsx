import React from 'react';
import type { DayAvailability, SlotStatus } from '../services/types';
import { isoTime, shortDateLabel, weekDayLabel } from '../utils/week';
import './AvailabilityGrid.css';

const STATUS_LABEL: Record<SlotStatus, string> = {
    AVAILABLE: 'Available',
    BOOKED: 'Booked',
    BOOKED_BY_ME: 'Your booking',
    PAST: 'Past',
};

export const AvailabilityGrid: React.FC<{ days: DayAvailability[] }> = ({ days }) => (
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
                            {day.slots.map((slot) => {
                                const available = slot.status === 'AVAILABLE';
                                return (
                                    <li
                                        key={slot.start}
                                        className={`slot slot-${slot.status.toLowerCase()}`}
                                        aria-disabled={available ? undefined : true}
                                    >
                                        <span className="slot-time">
                                            {isoTime(slot.start)}–{isoTime(slot.end)}
                                        </span>
                                        <span className="slot-status">{STATUS_LABEL[slot.status]}</span>
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
