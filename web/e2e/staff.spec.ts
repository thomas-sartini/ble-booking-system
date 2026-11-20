import { apiLogin, dateLabel, expect, findFreeRange, seedSession, selectRange, shot, showDay, test } from './helpers'

test('staff: sees the 3D Printer Lab and can book it', async ({ page, request, tracker }) => {
  tracker.track(page, 'staff')
  const session = await apiLogin(request, 'staff')
  const range = await findFreeRange(request, session, 3, 2) // 1 h at CHF 12.50/h

  await seedSession(page, session)
  await expect(page.getByText('Staff User')).toBeVisible()
  await expect(page.getByRole('link', { name: /Cargo Bike 1/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /Project Room D2.14/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /3D Printer Lab/ })).toBeVisible()
  await expect(page.getByRole('link', { name: /Old Beamer/ })).toHaveCount(0)
  await shot(page, 'catalog-staff')

  await page.getByRole('link', { name: /3D Printer Lab/ }).click()
  await expect(page.getByRole('heading', { name: '3D Printer Lab' })).toBeVisible()
  await expect(page.getByText('09:00–17:00').first()).toBeVisible()

  const day = await showDay(page, range.date)
  await selectRange(day, range)
  await page.getByRole('button', { name: 'Book…' }).click()
  const dialog = page.getByRole('dialog')
  await expect(dialog).toContainText('3D Printer Lab')
  await expect(dialog).toContainText(dateLabel(range.date))
  await expect(dialog).toContainText('CHF 12.50')
  await dialog.getByRole('button', { name: 'Confirm booking' }).click()

  const notice = page.locator('[data-sonner-toast]').filter({ hasText: 'Booked 3D Printer Lab' })
  await expect(notice).toContainText('CHF 12.50')
  await expect(page.getByRole('region', { name: range.date }).getByText('Your booking')).toHaveCount(2)

  await notice.getByRole('link', { name: 'View my bookings' }).click()
  await expect(page.getByRole('tabpanel').getByRole('row').filter({ hasText: '3D Printer Lab' })).toHaveCount(1)
  // the tracker cancels the booking afterwards
})
