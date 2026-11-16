import { API, apiLogin, expect, seedSession, test } from './helpers'

// "Expired" is simulated with an access token whose signature is wrong. The backend answers it
// exactly like an expired one (401, WWW-Authenticate invalid_token). A really expired token would
// need a 15 minute wait or the signing key.
const broken = (token: string) => `${token.slice(0, -4)}AAAA`

test('session: an invalid access token is refreshed transparently', async ({ page, request }) => {
  const session = await apiLogin(request, 'student')
  const refreshCalls: string[] = []
  page.on('request', (r) => {
    if (new URL(r.url()).pathname === '/auth/refresh') refreshCalls.push(r.method())
  })

  await seedSession(page, { ...session, accessToken: broken(session.accessToken) }, '/resources')

  await expect(page.getByRole('link', { name: /Cargo Bike 1/ })).toBeVisible()
  expect(refreshCalls).toEqual(['POST']) // exactly one refresh, no login screen
  const stored = await page.evaluate(() => ({
    access: localStorage.getItem('authToken'), refresh: localStorage.getItem('refreshToken'),
  }))
  expect(stored.access).not.toBe(broken(session.accessToken))
  // (the new access token can equal the first one byte for byte if it is issued in the same second)
  expect(stored.refresh).not.toBe(session.refreshToken) // rotated

  // the new access token really works
  const ok = await request.get(`${API}/resources`, { headers: { Authorization: `Bearer ${stored.access}` } })
  expect(ok.status()).toBe(200)

  // and the page keeps working with it
  await page.getByRole('link', { name: /Cargo Bike 1/ }).click()
  await expect(page.getByRole('heading', { name: 'Cargo Bike 1' })).toBeVisible()
  expect(refreshCalls).toHaveLength(1)
})

test('session: with an invalid refresh token the user is sent to the login page', async ({ page, request }) => {
  const session = await apiLogin(request, 'student')
  await seedSession(
    page,
    { ...session, accessToken: broken(session.accessToken), refreshToken: 'not-a-valid-refresh-token' },
    '/resources',
  )
  await expect(page.getByRole('heading', { name: 'Sign in' })).toBeVisible()
  expect(await page.evaluate(() => localStorage.getItem('authToken'))).toBeNull()
  expect(await page.evaluate(() => localStorage.getItem('refreshToken'))).toBeNull()
})
