import { describe, expect, it } from 'vitest'
import { addDays, mondayOf, todayIn } from './week'

describe('week helpers', () => {
  it('finds the Monday of a week for every weekday', () => {
    for (const d of ['2026-11-16', '2026-11-17', '2026-11-20', '2026-11-22']) {
      expect(mondayOf(d)).toBe('2026-11-16')
    }
    expect(mondayOf('2026-11-23')).toBe('2026-11-23')
  })

  it('adds days across month and DST boundaries', () => {
    expect(addDays('2026-11-30', 1)).toBe('2026-12-01')
    expect(addDays('2026-10-25', 7)).toBe('2026-11-01')
    expect(addDays('2026-11-16', -1)).toBe('2026-11-15')
  })

  it('computes today in the given zone, not the browser zone', () => {
    const instant = new Date('2026-11-16T23:30:00Z')
    expect(todayIn('Europe/Zurich', instant)).toBe('2026-11-17')
    expect(todayIn('America/New_York', instant)).toBe('2026-11-16')
  })
})
