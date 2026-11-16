import {
  apiLogin, credentials, dateLabel, expect, findFreeRange, listBookings, seedSession, selectRange, shot,
  showDay, slotButton, test,
} from './helpers'

// US-02.01 + US-02.02 + US-02.03 as a student, against the real backend.
test('student: browse, book, see it, cancel, slot is free again', async ({ page, request, tracker }) => {
  tracker.track(page, 'student')
  const session = await apiLogin(request, 'student')
  const range = await findFreeRange(request, session, 1, 3) // Cargo Bike 1, 1.5 h = CHF 7.50

  // Login through the real form (the only test that does this).
  const { email, password } = credentials('student')
  await page.goto('/login')
  await page.getByPlaceholder('firstname.lastname@ble.edu').fill(email)
  await page.getByPlaceholder('••••••••••••').fill(password)
  await page.getByRole('button', { name: 'Sign In' }).click()
  await expect(page.getByRole('heading', { name: 'Resources' })).toBeVisible()
  await expect(page.getByText('Student User')).toBeVisible()

  // US-02.01: catalog visibility (D4) and filters
  await expect(page.getByRole('link', { name: /Cargo Bike 1/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /Project Room D2.14/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /3D Printer Lab/ })).toHaveCount(0)
  await expect(page.getByRole('link', { name: /Old Beamer/ })).toHaveCount(0)
  await shot(page, 'catalog-student')

  await page.getByRole('button', { name: 'Mobile', exact: true }).click()
  await expect(page.getByRole('link', { name: /Project Room/ })).toHaveCount(0)
  await expect(page.getByRole('link', { name: /Cargo Bike 1/ })).toBeVisible()
  await page.getByRole('button', { name: 'All', exact: true }).click()
  await page.getByRole('searchbox', { name: 'Search resources' }).fill('room')
  await expect(page.getByRole('link', { name: /Cargo Bike/ })).toHaveCount(0)
  await expect(page.getByRole('link', { name: /Project Room D2.14/ })).toBeVisible()
  await page.getByRole('searchbox', { name: 'Search resources' }).fill('')

  // details + availability
  await page.getByRole('link', { name: /Cargo Bike 1/ }).click()
  await expect(page.getByRole('heading', { name: 'Cargo Bike 1' })).toBeVisible()
  await expect(page.getByText('CHF 5.00 / h')).toBeVisible()
  await expect(page.getByText('Monday').first()).toBeVisible()
  await expect(page.getByText('08:00–18:00').first()).toBeVisible()

  // US-02.02: select, confirm dialog, book
  const day = await showDay(page, range.date)
  await selectRange(day, range)
  await expect(page.locator('.selection-summary')).toContainText(`${range.first}–`)
  await shot(page, 'selection')
  await page.getByRole('button', { name: 'Book…' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toContainText('Cargo Bike 1')
  await expect(dialog).toContainText(dateLabel(range.date))
  await expect(dialog).toContainText('1 h 30 min')
  await expect(dialog).toContainText('Estimated price')
  await expect(dialog).toContainText('CHF 7.50')
  await shot(page, 'confirm-dialog')
  await dialog.getByRole('button', { name: 'Confirm booking' }).click()

  const notice = page.getByRole('status').filter({ hasText: 'Booked Cargo Bike 1' })
  await expect(notice).toContainText('CHF 7.50')
  await shot(page, 'booking-confirmed')
  await expect(page.getByRole('region', { name: range.date }).getByText('Your booking')).toHaveCount(3)
  await expect(slotButton(page.getByRole('region', { name: range.date }), range.first)).toHaveCount(0)

  // US-02.03: shows under Upcoming
  await notice.getByRole('link', { name: 'View my bookings' }).click()
  await expect(page.getByRole('heading', { name: 'My bookings' })).toBeVisible()
  const row = page.getByRole('tabpanel').getByRole('listitem').filter({ hasText: 'Cargo Bike 1' })
  await expect(row).toHaveCount(1)
  await expect(row).toContainText(dateLabel(range.date))
  await expect(row).toContainText(`${range.first}–`)
  await expect(row).toContainText('Booked')
  await expect(row).toContainText('CHF 7.50')
  await shot(page, 'my-bookings-upcoming')

  // cancel
  await row.getByRole('button', { name: /^Cancel booking of/ }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Yes, cancel booking' }).click()
  await expect(page.getByText('Booking cancelled.')).toBeVisible()
  await expect(row).toHaveCount(0)

  // moves to Past as CANCELLED, and it cannot be cancelled again
  await page.getByRole('tab', { name: 'Past' }).click()
  const past = page.getByRole('tabpanel').getByRole('listitem').filter({ hasText: 'Cargo Bike 1' })
  await expect(past.first()).toContainText('Cancelled')
  await expect(past.first().getByRole('button')).toHaveCount(0)
  await shot(page, 'my-bookings-past-cancelled')
  const mine = await listBookings(request, session, 'past')
  expect(mine.some((b) => tracker.ids.some((t) => t.id === b.id) && b.status === 'CANCELLED')).toBe(true)

  // the slots are available again
  await page.getByRole('link', { name: 'Resources', exact: true }).click()
  await expect(page.getByRole('heading', { name: 'Resources' })).toBeVisible()
  await page.getByRole('link', { name: /Cargo Bike 1/ }).click()
  const again = await showDay(page, range.date)
  for (const time of range.times) await expect(slotButton(again, time)).toBeVisible()
  await expect(again.getByText('Your booking')).toHaveCount(0)

  // signing out clears the session
  await page.getByRole('button', { name: 'Sign out' }).click()
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()
  expect(await page.evaluate(() => localStorage.getItem('authToken'))).toBeNull()
})

test('student: a staff-only resource behaves as not existing (D4)', async ({ page, request }) => {
  const session = await apiLogin(request, 'student')
  await seedSession(page, session, '/resources/3')
  await expect(page.getByRole('alert')).toContainText('This resource or booking is not available.')
})

test('student: a booking of another user is not visible (D13)', async ({ request }) => {
  const student = await apiLogin(request, 'student')
  const response = await request.get('http://localhost:8080/bookings/999999', {
    headers: { Authorization: `Bearer ${student.accessToken}` },
  })
  expect(response.status()).toBe(404)
})
