import { render as rtlRender, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { describe, expect, it } from 'vitest'
import { contractAvailability } from '../mocks/fixtures'
import type { DayAvailability } from '../services/types'
import { TooltipProvider } from '@/components/ui/tooltip'
import { AvailabilityCalendar } from './AvailabilityCalendar'

// The calendar's tooltips need the provider that AppLayout supplies in the app.
const render = (ui: ReactElement) => rtlRender(<TooltipProvider>{ui}</TooltipProvider>)

const day = (date: string, slots: DayAvailability['slots']): DayAvailability => ({ date, slots })

describe('AvailabilityCalendar', () => {
  it('renders the contract example with local times and status labels', () => {
    render(<AvailabilityCalendar days={contractAvailability.days} />)
    const tuesday = screen.getByRole('region', { name: '2026-11-17' })

    const items = within(tuesday).getAllByRole('listitem')
    expect(items).toHaveLength(3)
    expect(items[0]).toHaveTextContent('08:00–08:30')
    expect(items[0]).toHaveTextContent('Past')
    expect(items[1]).toHaveTextContent('08:30–09:00')
    expect(items[1]).toHaveTextContent('Booked')
    expect(items[2]).toHaveTextContent('09:00–09:30')
    expect(items[2]).toHaveTextContent('Available')
  })

  it('distinguishes every status by label, data-status and aria-disabled', () => {
    const slot = (h: string, status: DayAvailability['slots'][number]['status']) => ({
      start: `2026-11-18T${h}:00:00+01:00`, end: `2026-11-18T${h}:30:00+01:00`, status,
    })
    render(
      <AvailabilityCalendar
        days={[day('2026-11-18', [
          slot('08', 'AVAILABLE'), slot('09', 'BOOKED'), slot('10', 'BOOKED_BY_ME'), slot('11', 'PAST'),
        ])]}
      />,
    )
    const section = screen.getByRole('region', { name: '2026-11-18' })
    const [available, booked, mine, past] = within(section).getAllByRole('listitem')

    expect(available).toHaveAttribute('data-status', 'AVAILABLE')
    expect(available).not.toHaveAttribute('aria-disabled')
    expect(available).toHaveTextContent('Available')

    expect(booked).toHaveAttribute('data-status', 'BOOKED')
    expect(booked).toHaveAttribute('aria-disabled', 'true')
    expect(booked).toHaveTextContent('Booked')

    expect(mine).toHaveAttribute('data-status', 'BOOKED_BY_ME')
    expect(mine).toHaveAttribute('aria-disabled', 'true')
    expect(mine).toHaveTextContent('Your booking')

    expect(past).toHaveAttribute('data-status', 'PAST')
    expect(past).toHaveAttribute('aria-disabled', 'true')
    expect(past).toHaveTextContent('Past')
  })

  it('shows times from the ISO text, also with a summer offset', () => {
    render(
      <AvailabilityCalendar
        days={[day('2026-07-01', [
          { start: '2026-07-01T17:30:00+02:00', end: '2026-07-01T18:00:00+02:00', status: 'AVAILABLE' },
        ])]}
      />,
    )
    expect(screen.getByText('17:30–18:00')).toBeInTheDocument()
  })

  it('shows a message for a day without slots and a legend', () => {
    render(<AvailabilityCalendar days={[day('2026-11-21', []), day('2026-11-22', [])]} />)
    expect(screen.getAllByText('No bookable slots')).toHaveLength(2)
    const legend = screen.getByRole('list', { name: 'Legend' })
    for (const label of ['Available', 'Booked', 'Your booking', 'Past']) {
      expect(within(legend).getByText(label)).toBeInTheDocument()
    }
  })

  it('marks today\'s column with aria-current', () => {
    render(<AvailabilityCalendar days={contractAvailability.days} today="2026-11-17" />)
    expect(screen.getByRole('heading', { name: /Tue/ })).toHaveAttribute('aria-current', 'date')
  })

  it('shows the selected range with a Selected label', () => {
    const slot = (h: string) => ({
      start: `2026-11-18T${h}:00:00+01:00`, end: `2026-11-18T${h}:30:00+01:00`, status: 'AVAILABLE' as const,
    })
    render(
      <AvailabilityCalendar
        days={[day('2026-11-18', [slot('08'), slot('09'), slot('10')])]}
        selection={{ date: '2026-11-18', anchor: 0, from: 0, to: 1, complete: true }}
        onSelectSlot={() => {}}
      />,
    )
    const buttons = within(screen.getByRole('region', { name: '2026-11-18' })).getAllByRole('button')
    expect(buttons.map((b) => b.getAttribute('aria-pressed'))).toEqual(['true', 'true', 'false'])
    expect(buttons[0]).toHaveTextContent('Selected')
    expect(buttons[2]).toHaveTextContent('Available')
  })

  it('puts the cells of all days on one shared time axis (blank rows for days with other hours)', () => {
    const slot = (h: string, m: string, e: string) => ({
      start: `2026-11-18T${h}:${m}:00+01:00`, end: `2026-11-18T${e}:00+01:00`, status: 'AVAILABLE' as const,
    })
    render(
      <AvailabilityCalendar
        days={[
          day('2026-11-18', [slot('09', '00', '09:30'), slot('09', '30', '10:00')]),
          day('2026-11-19', [slot('10', '00', '10:30')]),
        ]}
      />,
    )
    // The axis spans 09:00-10:30 = 3 rows. Thursday's only slot is in the third row, the first two are blank spacers.
    const thursdayRows = screen.getByRole('region', { name: '2026-11-19' }).querySelectorAll('ul > li')
    expect(thursdayRows).toHaveLength(3)
    expect(thursdayRows[0]).toHaveAttribute('aria-hidden', 'true')
    expect(thursdayRows[1]).toHaveAttribute('aria-hidden', 'true')
    expect(thursdayRows[2]).toHaveTextContent('10:00–10:30')
    expect(screen.getByRole('region', { name: '2026-11-18' }).querySelectorAll('ul > li')).toHaveLength(3)
  })

  it('has a day tab strip for small screens: all days stay in the DOM, one tab is selected', async () => {
    render(<AvailabilityCalendar days={[day('2026-11-18', []), day('2026-11-19', [])]} today="2026-11-19" />)
    const tabs = screen.getByRole('tablist', { name: 'Choose day' })
    expect(within(tabs).getByRole('tab', { name: /Thu 19 Nov, today/ })).toHaveAttribute('aria-selected', 'true')
    await userEvent.click(within(tabs).getByRole('tab', { name: /Wed 18 Nov/ }))
    expect(within(tabs).getByRole('tab', { name: /Wed 18 Nov/ })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getAllByRole('region')).toHaveLength(2)
  })
})
