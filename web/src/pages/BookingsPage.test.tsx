import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { lockBooking, seedBooking } from '../mocks/bookingStore'
import { server } from '../mocks/server'
import { API, renderApp, setNow, signIn } from '../test/utils'

// Now: Monday 2026-11-16 10:00 Zurich.
const at = (date: string, from: string, to: string) => ({
  start: `${date}T${from}:00+01:00`, end: `${date}T${to}:00+01:00`,
})

beforeEach(() => {
  setNow()
  signIn()
})

function seedTypical() {
  const soon = seedBooking({ resourceId: 1, ...at('2026-11-17', '10:00', '11:30') })
  const later = seedBooking({ resourceId: 3, ...at('2026-11-20', '09:00', '10:00') })
  const done = seedBooking({ resourceId: 2, ...at('2026-11-10', '14:00', '15:00'), status: 'COMPLETED' })
  const cancelled = seedBooking({ resourceId: 1, ...at('2026-11-18', '08:00', '09:00'), status: 'CANCELLED' })
  return { soon, later, done, cancelled }
}

const openBookings = async () => {
  renderApp('/bookings')
  return screen.findByRole('table')
}
const rows = () => within(screen.getByRole('tabpanel')).getAllByRole('row').slice(1) // without the header row

describe('BookingsPage', () => {
  it('shows a loading state, then upcoming bookings sorted by start with all fields', async () => {
    seedTypical()
    server.use(http.get(`${API}/bookings`, async () => {
      await delay(30)
      return undefined // fall through to the mock store
    }))
    renderApp('/bookings')
    expect(screen.getByRole('status')).toHaveTextContent('Loading bookings')

    await screen.findByRole('table')
    expect(rows()).toHaveLength(2)
    expect(rows()[0]).toHaveTextContent('Cargo Bike 1')
    expect(within(rows()[0]).getAllByRole('cell')[0]).toHaveTextContent(/^#\d+$/)
    expect(rows()[0]).toHaveTextContent('Tue, 17 Nov 2026')
    expect(rows()[0]).toHaveTextContent('10:00–11:30')
    expect(rows()[0]).toHaveTextContent('Booked')
    expect(rows()[0]).toHaveTextContent('CHF 7.50')
    expect(within(rows()[0]).getByRole('link', { name: 'Cargo Bike 1' })).toHaveAttribute('href', '/resources/1')
    expect(rows()[1]).toHaveTextContent('3D Printer Lab')
    expect(rows()[1]).toHaveTextContent('CHF 12.50')
    expect(screen.getByRole('tab', { name: 'Upcoming' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByText('Showing 2 bookings')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse resources' })).toHaveAttribute('href', '/resources')
  })

  it('shows summary cards computed from the loaded bookings', async () => {
    seedTypical()
    renderApp('/bookings')
    await screen.findByRole('table')
    const card = (label: string) =>
      within(screen.getByRole('tabpanel')).getByText(label, { selector: '[data-slot="card-description"]' }).closest('[data-slot="card"]')!
    expect(card('Upcoming bookings')).toHaveTextContent('2')
    expect(card('Total')).toHaveTextContent('CHF 20.00') // 7.50 + 12.50
    expect(card('Next session')).toHaveTextContent('Cargo Bike 1')

    // Past: the cancelled booking is listed but not billed.
    await userEvent.click(screen.getByRole('tab', { name: 'Past' }))
    await waitFor(() => expect(within(screen.getByRole('tabpanel')).getByText('Past bookings')).toBeInTheDocument())
    expect(screen.getByText('1 billable, 1 cancelled')).toBeInTheDocument()
    expect(screen.getByText('Latest session')).toBeInTheDocument()
  })

  it('requests scope=upcoming first and scope=past after switching the tab', async () => {
    const scopes: (string | null)[] = []
    server.use(http.get(`${API}/bookings`, ({ request }) => {
      scopes.push(new URL(request.url).searchParams.get('scope'))
      return HttpResponse.json([])
    }))
    renderApp('/bookings')
    await screen.findByText('No upcoming bookings')
    await userEvent.click(screen.getByRole('tab', { name: 'Past' }))
    await screen.findByText('No past bookings')
    expect(scopes).toEqual(['upcoming', 'past'])
    expect(screen.getByRole('tab', { name: 'Past' })).toHaveAttribute('aria-selected', 'true')
  })

  it('lists past and cancelled bookings under Past, newest first, without cancel buttons', async () => {
    seedTypical()
    renderApp('/bookings')
    await screen.findByRole('table')
    await userEvent.click(screen.getByRole('tab', { name: 'Past' }))
    await waitFor(() => expect(rows()).toHaveLength(2))
    expect(rows()[0]).toHaveTextContent('Cancelled')
    expect(rows()[1]).toHaveTextContent('Completed')
    expect(rows()[1]).toHaveTextContent('Project Room D2.14')
    expect(rows()[1]).toHaveTextContent('CHF 0.00')
    expect(screen.queryByRole('button', { name: /^Cancel booking/ })).not.toBeInTheDocument()
  })

  it('shows the empty state for each tab', async () => {
    renderApp('/bookings')
    expect(await screen.findByText('No upcoming bookings')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: 'Browse resources' })).toHaveLength(2) // header action and empty state
    await userEvent.click(screen.getByRole('tab', { name: 'Past' }))
    expect(await screen.findByText('No past bookings')).toBeInTheDocument()
  })

  it('shows the error state and recovers on Retry', async () => {
    let fail = true
    server.use(http.get(`${API}/bookings`, () =>
      fail ? HttpResponse.text('boom', { status: 500 }) : HttpResponse.json([]),
    ))
    renderApp('/bookings')
    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
    fail = false
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByText('No upcoming bookings')).toBeInTheDocument()
  })

  it('shows Cancel only for cancellable bookings', async () => {
    seedBooking({ resourceId: 1, ...at('2026-11-17', '10:00', '11:00') })
    seedBooking({ resourceId: 1, ...at('2026-11-18', '10:00', '11:00'), status: 'CHECKED_IN' })
    await openBookings()
    expect(rows()).toHaveLength(2)
    expect(within(rows()[0]).getByRole('button', { name: /^Cancel booking of Cargo Bike 1/ })).toBeInTheDocument()
    expect(rows()[1]).toHaveTextContent('Checked in')
    expect(within(rows()[1]).queryByRole('button')).not.toBeInTheDocument()
  })
})

describe('cancel flow', () => {
  it('confirms, sends DELETE, treats 204 as success and refreshes the list', async () => {
    const { soon } = seedTypical()
    const deleted: string[] = []
    server.use(http.delete(`${API}/bookings/:id`, ({ params }) => {
      deleted.push(String(params.id))
      return undefined // the mock store handles it and answers 204
    }))
    await openBookings()
    await userEvent.click(within(rows()[0]).getByRole('button', { name: /^Cancel booking of/ }))

    const dialog = await screen.findByRole('alertdialog')
    expect(dialog).toHaveTextContent('Cancel this booking?')
    expect(dialog).toHaveTextContent('Cargo Bike 1')
    expect(dialog).toHaveTextContent('Tue, 17 Nov 2026')
    expect(dialog).toHaveTextContent('10:00–11:30')
    expect(deleted).toEqual([]) // nothing sent before confirming

    await userEvent.click(within(dialog).getByRole('button', { name: 'Yes, cancel booking' }))
    expect(await screen.findByText('Booking cancelled.')).toBeInTheDocument()
    expect(deleted).toEqual([String(soon)])
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    await waitFor(() => expect(rows()).toHaveLength(1))
    expect(rows()[0]).toHaveTextContent('3D Printer Lab')
  })

  it('keeps the booking when the user chooses Keep booking', async () => {
    seedTypical()
    let deletes = 0
    server.use(http.delete(`${API}/bookings/:id`, () => { deletes++; return new HttpResponse(null, { status: 204 }) }))
    await openBookings()
    const cancelButton = within(rows()[0]).getByRole('button', { name: /^Cancel booking of/ })
    await userEvent.click(cancelButton)
    await userEvent.click(await screen.findByRole('button', { name: 'Keep booking' }))
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    await waitFor(() => expect(cancelButton).toHaveFocus()) // focus returns to the opener
    expect(deletes).toBe(0)
    expect(rows()).toHaveLength(2)
  })

  it('shows the INVALID_STATUS message, closes the dialog and refreshes the list', async () => {
    const { soon } = seedTypical()
    await openBookings()
    lockBooking(soon) // the server now refuses, but the loaded list still says cancellable
    await userEvent.click(within(rows()[0]).getByRole('button', { name: /^Cancel booking of/ }))
    await userEvent.click(await screen.findByRole('button', { name: 'Yes, cancel booking' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('This booking can no longer be cancelled.')
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    await waitFor(() => expect(rows()).toHaveLength(2))
    expect(rows()[0]).toHaveTextContent('Cargo Bike 1') // still booked
  })

  it('shows the NOT_FOUND message when the booking is gone', async () => {
    seedTypical()
    server.use(http.delete(`${API}/bookings/:id`, () => HttpResponse.json({ code: 'NOT_FOUND' }, { status: 404 })))
    await openBookings()
    await userEvent.click(within(rows()[0]).getByRole('button', { name: /^Cancel booking of/ }))
    await userEvent.click(await screen.findByRole('button', { name: 'Yes, cancel booking' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('This resource or booking is not available.')
  })

  it('keeps the dialog open with the message on other errors', async () => {
    seedTypical()
    server.use(http.delete(`${API}/bookings/:id`, () => HttpResponse.text('boom', { status: 500 })))
    await openBookings()
    await userEvent.click(within(rows()[0]).getByRole('button', { name: /^Cancel booking of/ }))
    const dialog = await screen.findByRole('alertdialog')
    await userEvent.click(within(dialog).getByRole('button', { name: 'Yes, cancel booking' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
    expect(within(dialog).getByRole('button', { name: 'Yes, cancel booking' })).toBeEnabled()
  })

  it('does not send DELETE twice on a double click', async () => {
    seedTypical()
    let deletes = 0
    server.use(http.delete(`${API}/bookings/:id`, async () => {
      deletes++
      await delay(100)
      return new HttpResponse(null, { status: 204 })
    }))
    await openBookings()
    await userEvent.click(within(rows()[0]).getByRole('button', { name: /^Cancel booking of/ }))
    const confirm = await screen.findByRole('button', { name: 'Yes, cancel booking' })
    await userEvent.dblClick(confirm)
    expect(confirm).toBeDisabled()
    await screen.findByText('Booking cancelled.')
    expect(deletes).toBe(1)
  })
})
