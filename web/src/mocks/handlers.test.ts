import { describe, expect, it } from 'vitest'
import { apiRequest } from '../services/apiClient'
import { beforeEach } from 'vitest'
import { setNow, signIn } from '../test/utils'
import { contractAvailability } from './fixtures'
import type { Availability } from '../services/types'

describe('mock handlers', () => {
  beforeEach(() => setNow())

  it('return the contract example for exactly that query', async () => {
    signIn()
    const result = await apiRequest<Availability>('/resources/3/availability', {
      query: { from: '2026-11-17', to: '2026-11-17' },
    })
    expect(result).toEqual(contractAvailability)
  })

  it('generate a Monday to Sunday grid with offset times for other weeks', async () => {
    signIn()
    const result = await apiRequest<Availability>('/resources/1/availability', {
      query: { from: '2026-01-12', to: '2026-01-18' },
    })
    expect(result.days.map((d) => d.date)).toHaveLength(7)
    expect(result.days[0].slots[0]).toMatchObject({ start: '2026-01-12T08:00:00+01:00' })
    expect(result.days[5].slots).toEqual([])
    // Summer time offset (dates in the past are still generated, as PAST)
    const summer = await apiRequest<Availability>('/resources/1/availability', {
      query: { from: '2026-07-01', to: '2026-07-01' },
    })
    expect(summer.days[0].slots[0].start).toBe('2026-07-01T08:00:00+02:00')
  })

  it('reject a range above 14 days, from > to, and unknown resources', async () => {
    signIn()
    await expect(
      apiRequest('/resources/1/availability', { query: { from: '2026-11-16', to: '2026-12-01' } }),
    ).rejects.toMatchObject({ status: 400, code: 'VALIDATION_ERROR' })
    await expect(
      apiRequest('/resources/1/availability', { query: { from: '2026-11-17', to: '2026-11-16' } }),
    ).rejects.toMatchObject({ code: 'VALIDATION_ERROR' })
    await expect(apiRequest('/resources/99/availability', { query: { from: '2026-11-16', to: '2026-11-16' } })).rejects.toMatchObject({
      status: 404, code: 'NOT_FOUND',
    })
    await expect(apiRequest('/resources', { query: { type: 'BOGUS' } })).rejects.toMatchObject({ code: 'VALIDATION_ERROR' })
  })
})
