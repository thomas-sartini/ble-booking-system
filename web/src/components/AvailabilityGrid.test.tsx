import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { contractAvailability } from '../mocks/fixtures'
import type { DayAvailability } from '../services/types'
import { AvailabilityGrid } from './AvailabilityGrid'

const day = (date: string, slots: DayAvailability['slots']): DayAvailability => ({ date, slots })

describe('AvailabilityGrid', () => {
  it('renders the contract example with local times and status labels', () => {
    render(<AvailabilityGrid days={contractAvailability.days} />)
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

  it('distinguishes every status by label, class and aria-disabled', () => {
    const slot = (h: string, status: DayAvailability['slots'][number]['status']) => ({
      start: `2026-11-18T${h}:00:00+01:00`, end: `2026-11-18T${h}:30:00+01:00`, status,
    })
    render(
      <AvailabilityGrid
        days={[day('2026-11-18', [
          slot('08', 'AVAILABLE'), slot('09', 'BOOKED'), slot('10', 'BOOKED_BY_ME'), slot('11', 'PAST'),
        ])]}
      />,
    )
    const section = screen.getByRole('region', { name: '2026-11-18' })
    const [available, booked, mine, past] = within(section).getAllByRole('listitem')

    expect(available).toHaveClass('slot-available')
    expect(available).not.toHaveAttribute('aria-disabled')
    expect(available).toHaveTextContent('Available')

    expect(booked).toHaveClass('slot-booked')
    expect(booked).toHaveAttribute('aria-disabled', 'true')
    expect(booked).toHaveTextContent('Booked')

    expect(mine).toHaveClass('slot-booked_by_me')
    expect(mine).toHaveAttribute('aria-disabled', 'true')
    expect(mine).toHaveTextContent('Your booking')

    expect(past).toHaveClass('slot-past')
    expect(past).toHaveAttribute('aria-disabled', 'true')
    expect(past).toHaveTextContent('Past')
  })

  it('shows times from the ISO text, also with a summer offset', () => {
    render(
      <AvailabilityGrid
        days={[day('2026-07-01', [
          { start: '2026-07-01T17:30:00+02:00', end: '2026-07-01T18:00:00+02:00', status: 'AVAILABLE' },
        ])]}
      />,
    )
    expect(screen.getByText('17:30–18:00')).toBeInTheDocument()
  })

  it('shows a message for a day without slots and a legend', () => {
    render(<AvailabilityGrid days={[day('2026-11-21', []), day('2026-11-22', [])]} />)
    expect(screen.getAllByText('No bookable slots')).toHaveLength(2)
    const legend = screen.getByRole('list', { name: 'Legend' })
    for (const label of ['Available', 'Booked', 'Your booking', 'Past']) {
      expect(within(legend).getByText(label)).toBeInTheDocument()
    }
  })
})
