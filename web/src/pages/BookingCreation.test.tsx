import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { addForeignBooking } from '../mocks/bookingStore'
import { server } from '../mocks/server'
import { MAX_DURATION_MESSAGE, NOT_AVAILABLE_MESSAGE } from '../utils/booking'
import { API, renderApp, setNow, signIn } from '../test/utils'

// Now: Monday 2026-11-16 10:00 Zurich. Cargo Bike 1 (id 1): CHF 5.00/h, Mon-Fri 08:00-18:00.
const TUESDAY = '2026-11-17'

beforeEach(() => {
  setNow()
  signIn()
})

async function openBike() {
  renderApp('/resources/1')
  return await screen.findByRole('region', { name: TUESDAY })
}
const slotButton = (day: HTMLElement, time: string) =>
  within(day).getByRole('button', { name: new RegExp(`^${time}–`) })
const click = (day: HTMLElement, ...times: string[]) =>
  times.reduce((p, t) => p.then(() => userEvent.click(slotButton(day, t))), Promise.resolve())
const summary = () => document.querySelector('.selection-summary')

async function selectAndOpenDialog(times: [string, string]) {
  const day = await openBike()
  await click(day, ...times)
  await userEvent.click(screen.getByRole('button', { name: 'Book…' }))
  return { day, dialog: await screen.findByRole('dialog') }
}

describe('slot selection', () => {
  it('selects a range with two clicks and marks the slots as pressed', async () => {
    const day = await openBike()
    await click(day, '10:00')
    expect(summary()).toHaveTextContent('10:00–10:30')
    expect(summary()).toHaveTextContent('click another slot')

    await click(day, '11:30')
    expect(summary()).toHaveTextContent('Tue, 17 Nov 2026, 10:00–12:00 (2 h)')
    expect(slotButton(day, '11:00')).toHaveAttribute('aria-pressed', 'true')
    expect(slotButton(day, '12:00')).toHaveAttribute('aria-pressed', 'false')
    expect(slotButton(day, '09:30')).toHaveAttribute('aria-pressed', 'false')
  })

  it('accepts the last slot before the first', async () => {
    const day = await openBike()
    await click(day, '12:00', '11:00')
    expect(summary()).toHaveTextContent('11:00–12:30 (1 h 30 min)')
  })

  it('rejects a range across a booked slot and keeps the previous selection', async () => {
    addForeignBooking(1, `${TUESDAY}T11:00:00+01:00`, `${TUESDAY}T11:30:00+01:00`)
    const day = await openBike()
    await click(day, '10:00', '12:00')
    expect(screen.getByRole('alert')).toHaveTextContent(NOT_AVAILABLE_MESSAGE)
    expect(summary()).toHaveTextContent('10:00–10:30')
    expect(within(day).queryByRole('button', { name: /^11:00–/ })).not.toBeInTheDocument()
  })

  it('rejects more than 4 hours but accepts exactly 4 hours', async () => {
    const day = await openBike()
    await click(day, '08:00', '12:00')
    expect(screen.getByRole('alert')).toHaveTextContent(MAX_DURATION_MESSAGE)
    expect(summary()).toHaveTextContent('08:00–08:30')

    await click(day, '11:30')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(summary()).toHaveTextContent('08:00–12:00 (4 h)')
  })

  it('starts over on another day and after a completed range', async () => {
    renderApp('/resources/1')
    const tue = await screen.findByRole('region', { name: TUESDAY })
    const wed = screen.getByRole('region', { name: '2026-11-18' })
    await click(tue, '10:00', '11:00')
    await click(wed, '09:00')
    expect(summary()).toHaveTextContent('Wed, 18 Nov 2026, 09:00–09:30')
    expect(slotButton(tue, '10:00')).toHaveAttribute('aria-pressed', 'false')
  })

  it('clears with the button and with Escape', async () => {
    const day = await openBike()
    await click(day, '10:00', '11:00')
    await userEvent.click(screen.getByRole('button', { name: 'Clear selection' }))
    expect(summary()).toBeNull()
    expect(screen.queryByRole('button', { name: 'Book…' })).not.toBeInTheDocument()

    await click(day, '10:00')
    await userEvent.keyboard('{Escape}')
    expect(summary()).toBeNull()
  })

  it('works with the keyboard (Enter and Space on focused slots)', async () => {
    const day = await openBike()
    slotButton(day, '10:00').focus()
    await userEvent.keyboard('{Enter}')
    slotButton(day, '11:00').focus()
    await userEvent.keyboard(' ')
    expect(summary()).toHaveTextContent('10:00–11:30')
    expect(slotButton(day, '10:30')).toHaveAttribute('aria-pressed', 'true')
    expect(screen.getByRole('button', { name: 'Book…' })).toBeInTheDocument()
  })

  it('offers no button for past, booked or own-booked slots', async () => {
    addForeignBooking(1, `${TUESDAY}T09:00:00+01:00`, `${TUESDAY}T09:30:00+01:00`)
    renderApp('/resources/1')
    const mon = await screen.findByRole('region', { name: '2026-11-16' })
    expect(within(mon).queryByRole('button', { name: /^08:00–/ })).not.toBeInTheDocument() // PAST
    expect(within(mon).getByRole('button', { name: /^10:30–/ })).toBeInTheDocument()
    const tue = screen.getByRole('region', { name: TUESDAY })
    expect(within(tue).queryByRole('button', { name: /^09:00–/ })).not.toBeInTheDocument() // BOOKED
  })

  it('clears the selection when the week changes', async () => {
    const day = await openBike()
    await click(day, '10:00')
    await userEvent.click(screen.getByRole('button', { name: /Next week/ }))
    await screen.findByRole('region', { name: '2026-11-24' })
    expect(summary()).toBeNull()
  })
})

describe('confirm dialog', () => {
  it('shows resource, date, time in the resource zone, duration and a labelled price estimate', async () => {
    const { dialog } = await selectAndOpenDialog(['10:00', '11:00'])
    const text = within(dialog)
    expect(text.getByRole('heading', { name: 'Confirm booking' })).toBeInTheDocument()
    expect(dialog).toHaveTextContent('Cargo Bike 1')
    expect(dialog).toHaveTextContent('Tue, 17 Nov 2026')
    expect(dialog).toHaveTextContent('10:00–11:30 (Europe/Zurich)')
    expect(dialog).toHaveTextContent('1 h 30 min')
    expect(dialog).toHaveTextContent('Estimated price')
    expect(dialog).toHaveTextContent('CHF 7.50')
    expect(dialog).toHaveTextContent('estimate')
  })

  it('closes with Cancel and Escape without sending anything', async () => {
    let posts = 0
    server.use(http.post(`${API}/bookings`, () => { posts++; return new HttpResponse(null, { status: 500 }) }))
    await selectAndOpenDialog(['10:00', '10:00'])
    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Book…' }))
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(posts).toBe(0)
    expect(summary()).toHaveTextContent('10:00–10:30') // selection survives
  })
})

describe('booking flow', () => {
  it('posts the range, shows the server price with a link, and reloads availability', async () => {
    let body: unknown
    server.use(http.post(`${API}/bookings`, async ({ request }) => {
      body = await request.clone().json()
      return undefined // fall through to the default handler
    }))
    const { day } = await selectAndOpenDialog(['10:00', '11:00'])
    await userEvent.click(screen.getByRole('button', { name: 'Confirm booking' }))

    const notice = await screen.findByRole('status')
    expect(notice).toHaveTextContent('Booked Cargo Bike 1')
    expect(notice).toHaveTextContent('10:00–11:30')
    expect(notice).toHaveTextContent('CHF 7.50')
    expect(within(notice).getByRole('link', { name: 'View my bookings' })).toHaveAttribute('href', '/bookings')
    expect(body).toEqual({
      resourceId: 1, start: `${TUESDAY}T10:00:00+01:00`, end: `${TUESDAY}T11:30:00+01:00`,
    })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()

    // Availability was reloaded: the three slots are now the user's own booking.
    const tuesday = () => within(screen.getByRole('region', { name: TUESDAY }))
    await waitFor(() => expect(tuesday().getAllByText('Your booking')).toHaveLength(3))
    expect(screen.getByRole('region', { name: TUESDAY })).not.toBe(day) // re-rendered from fresh data
    expect(tuesday().queryByRole('button', { name: /^10:00–/ })).not.toBeInTheDocument()
    expect(summary()).toBeNull()
  })

  it('on BOOKING_OVERLAP reloads availability, keeps the user on the page and shows the message', async () => {
    const { dialog } = await selectAndOpenDialog(['10:00', '11:00'])
    expect(dialog).toBeInTheDocument()
    addForeignBooking(1, `${TUESDAY}T10:30:00+01:00`, `${TUESDAY}T11:00:00+01:00`) // someone was faster
    await userEvent.click(screen.getByRole('button', { name: 'Confirm booking' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Someone just booked this time. Please choose another slot.',
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Cargo Bike 1' })).toBeInTheDocument()
    await waitFor(() =>
      expect(within(screen.getByRole('region', { name: TUESDAY })).getAllByText('Booked')).toHaveLength(1),
    )
    expect(summary()).toBeNull()
  })

  it('keeps the dialog open and shows the message for other errors, so the user can retry', async () => {
    server.use(http.post(`${API}/bookings`, () =>
      HttpResponse.json({ code: 'OUTSIDE_AVAILABILITY', detail: 'x' }, { status: 409 }),
    ))
    const { dialog } = await selectAndOpenDialog(['10:00', '10:00'])
    await userEvent.click(screen.getByRole('button', { name: 'Confirm booking' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('This time is not bookable for you.')
    expect(screen.getByRole('button', { name: 'Confirm booking' })).toBeEnabled()
  })

  it('shows the detail of a VALIDATION_ERROR', async () => {
    server.use(http.post(`${API}/bookings`, () =>
      HttpResponse.json({ code: 'VALIDATION_ERROR', detail: 'start must be in the future.' }, { status: 400 }),
    ))
    const { dialog } = await selectAndOpenDialog(['10:00', '10:00'])
    await userEvent.click(screen.getByRole('button', { name: 'Confirm booking' }))
    expect(await within(dialog).findByRole('alert')).toHaveTextContent('start must be in the future.')
  })

  it('does not submit twice: the button is disabled while sending and only one POST goes out', async () => {
    let posts = 0
    server.use(http.post(`${API}/bookings`, async () => {
      posts++
      await delay(150)
      return HttpResponse.json({
        id: 1, resource: { id: 1, name: 'Cargo Bike 1', type: 'MOBILE', position: null },
        start: `${TUESDAY}T10:00:00+01:00`, end: `${TUESDAY}T10:30:00+01:00`, status: 'BOOKED',
        price: '2.50', currency: 'CHF', cancellable: true, createdAt: `${TUESDAY}T09:00:00+01:00`,
      }, { status: 201 })
    }))
    await selectAndOpenDialog(['10:00', '10:00'])
    const confirm = screen.getByRole('button', { name: 'Confirm booking' })
    await userEvent.dblClick(confirm)

    expect(confirm).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
    await screen.findByText(/Booked Cargo Bike 1/)
    expect(posts).toBe(1)
  })
})
