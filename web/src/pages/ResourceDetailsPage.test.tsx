import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { contractAvailability, resourceDetails } from '../mocks/fixtures'
import { server } from '../mocks/server'
import { API, renderApp, setNow, signIn } from '../test/utils'

beforeEach(() => {
  signIn()
  setNow() // Monday 2026-11-16 in Zurich
})

function recordAvailability() {
  const ranges: string[] = []
  server.use(http.get(`${API}/resources/:id/availability`, ({ request, params }) => {
    const u = new URL(request.url)
    ranges.push(`${u.searchParams.get('from')}..${u.searchParams.get('to')}`)
    return HttpResponse.json({
      resourceId: Number(params.id), timeZone: 'Europe/Zurich', slotMinutes: 30,
      from: u.searchParams.get('from'), to: u.searchParams.get('to'), days: [],
    })
  }))
  return ranges
}

describe('ResourceDetailsPage', () => {
  it('shows details and bookable hours grouped by day', async () => {
    renderApp('/resources/2')

    expect(await screen.findByRole('heading', { name: 'Project Room D2.14' })).toBeInTheDocument()
    expect(screen.getByText('Building D, Room 2.14')).toBeInTheDocument()
    expect(screen.getByText('CHF 0.00 / h')).toBeInTheDocument()
    expect(screen.getByText('Europe/Zurich')).toBeInTheDocument()
    const hours = screen.getByText('Monday').closest('li')!
    expect(hours).toHaveTextContent('07:00–21:00')
    expect(screen.getByText('Saturday').closest('li')).toHaveTextContent('09:00–14:00')
    expect(screen.queryByText('Sunday', { selector: 'strong' })).not.toBeInTheDocument()
  })

  it('renders the contract example for resource 3', async () => {
    // The example date is a Tuesday: go there by pointing "today" into that week.
    setNow('2026-11-17T07:00:00+01:00')
    server.use(http.get(`${API}/resources/3/availability`, () =>
      HttpResponse.json({ ...contractAvailability, from: '2026-11-16', to: '2026-11-22' }),
    ))
    renderApp('/resources/3')

    const day = await screen.findByRole('region', { name: '2026-11-17' })
    const items = within(day).getAllByRole('listitem')
    expect(items.map((i) => i.textContent)).toEqual([
      '08:00–08:30Past', '08:30–09:00Booked', '09:00–09:30Available',
    ])
  })

  it('requests Monday to Sunday and navigates between weeks', async () => {
    const ranges = recordAvailability()
    renderApp('/resources/1')
    await screen.findByRole('heading', { name: 'Cargo Bike 1' })

    const prev = screen.getByRole('button', { name: /Previous week/ })
    const next = screen.getByRole('button', { name: /Next week/ })
    await waitFor(() => expect(ranges).toEqual(['2026-11-16..2026-11-22']))
    expect(screen.getByText('16 Nov – 22 Nov')).toBeInTheDocument()
    expect(prev).toBeDisabled()

    await userEvent.click(next)
    await waitFor(() => expect(ranges).toEqual(['2026-11-16..2026-11-22', '2026-11-23..2026-11-29']))
    expect(prev).toBeEnabled()

    await userEvent.click(prev)
    await waitFor(() => expect(ranges.at(-1)).toBe('2026-11-16..2026-11-22'))
    expect(prev).toBeDisabled()
  })

  it('stops at the last week that starts within the 30-day horizon', async () => {
    // Now is Monday 2026-11-16: horizon ends 2026-12-16, so the last useful week starts Monday 2026-12-14.
    const ranges = recordAvailability()
    renderApp('/resources/1')
    const next = await screen.findByRole('button', { name: /Next week/ })

    for (let i = 0; i < 4; i++) await userEvent.click(next)
    await waitFor(() => expect(ranges.at(-1)).toBe('2026-12-14..2026-12-20'))
    expect(next).toBeDisabled()
  })

  it('allows one more week when the horizon reaches into it', async () => {
    setNow('2026-11-21T10:00:00+01:00') // Saturday: horizon ends 2026-12-21, which is the Monday of one more week
    const ranges = recordAvailability()
    renderApp('/resources/1')
    const next = await screen.findByRole('button', { name: /Next week/ })
    for (let i = 0; i < 5; i++) await userEvent.click(next)
    await waitFor(() => expect(ranges.at(-1)).toBe('2026-12-21..2026-12-27'))
    expect(next).toBeDisabled()
  })

  it('computes the current week in the resource time zone, not the browser zone', async () => {
    // Sunday 23:30 UTC is already Monday 00:30 in Zurich.
    setNow('2026-11-22T23:30:00Z')
    const ranges = recordAvailability()
    renderApp('/resources/1')
    await waitFor(() => expect(ranges).toEqual(['2026-11-23..2026-11-29']))
  })

  it('shows the not-available message for an unknown resource', async () => {
    renderApp('/resources/999')
    expect(await screen.findByRole('alert')).toHaveTextContent('This resource or booking is not available.')
  })

  it('shows an availability error with retry while keeping the details', async () => {
    let fail = true
    server.use(http.get(`${API}/resources/1/availability`, () =>
      fail
        ? HttpResponse.json({ code: 'NOT_FOUND', detail: 'gone' }, { status: 404 })
        : HttpResponse.json({ ...contractAvailability, resourceId: 1 }),
    ))
    renderApp('/resources/1')

    expect(await screen.findByRole('alert')).toHaveTextContent('This resource or booking is not available.')
    expect(screen.getByRole('heading', { name: resourceDetails[0].name })).toBeInTheDocument()
    fail = false
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument())
  })

  it('shows a loading state while the resource loads', () => {
    renderApp('/resources/1')
    expect(screen.getByRole('status')).toHaveTextContent('Loading resource')
  })
})
