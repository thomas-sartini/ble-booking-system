import { apiLogin, expect, findFreeRange, seedSession, selectRange, shot, showDay, slotButton, test } from './helpers'

// Two real sessions, same slot: the second one must get BOOKING_OVERLAP and a reloaded grid.
test('conflict: the second user gets the overlap message and a reloaded grid', async ({ browser, request, tracker }) => {
  const studentSession = await apiLogin(request, 'student')
  const staffSession = await apiLogin(request, 'staff')
  const range = await findFreeRange(request, studentSession, 2, 2) // Project Room D2.14 (both roles eligible)

  const studentPage = await (await browser.newContext()).newPage()
  const staffPage = await (await browser.newContext()).newPage()
  tracker.track(studentPage, 'student')
  tracker.track(staffPage, 'staff')

  try {
    for (const [page, session] of [[studentPage, studentSession], [staffPage, staffSession]] as const) {
      await seedSession(page, session, '/resources/2')
    }
    const studentDay = await showDay(studentPage, range.date)
    const staffDay = await showDay(staffPage, range.date)

    // both pick the same slots and open the dialog
    await selectRange(studentDay, range)
    await studentPage.getByRole('button', { name: 'Book…' }).click()
    await expect(studentPage.getByRole('dialog')).toBeVisible()
    await selectRange(staffDay, range)
    await staffPage.getByRole('button', { name: 'Book…' }).click()

    // staff is faster
    await staffPage.getByRole('dialog').getByRole('button', { name: 'Confirm booking' }).click()
    await expect(staffPage.getByRole('status').filter({ hasText: 'Booked Project Room D2.14' })).toBeVisible()

    // the student confirms the stale selection
    await studentPage.getByRole('dialog').getByRole('button', { name: 'Confirm booking' }).click()
    await expect(studentPage.getByRole('alert')).toContainText('Someone just booked this time. Please choose another slot.')
    await expect(studentPage.getByRole('dialog')).toHaveCount(0)
    await expect(studentPage.getByRole('heading', { name: 'Project Room D2.14' })).toBeVisible()
    await shot(studentPage, 'conflict-overlap')

    // the grid was reloaded: the slots are now taken by someone else
    const reloaded = studentPage.getByRole('region', { name: range.date })
    await expect(reloaded.getByText('Booked', { exact: true })).toHaveCount(2)
    for (const time of range.times) await expect(slotButton(reloaded, time)).toHaveCount(0)
    await expect(studentPage.locator('.selection-summary')).toHaveCount(0)

    // and the student has no booking
    const bookings = await request.get('http://localhost:8080/bookings', {
      headers: { Authorization: `Bearer ${studentSession.accessToken}` }, params: { scope: 'upcoming' },
    })
    const ids = ((await bookings.json()) as { id: number }[]).map((b) => b.id)
    expect(tracker.ids.filter((t) => t.user === 'student' && ids.includes(t.id))).toHaveLength(0)
  } finally {
    await studentPage.context().close()
    await staffPage.context().close()
  }
})
