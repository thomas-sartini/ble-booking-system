import { describe, expect, it } from 'vitest'
import type { WeeklyWindow } from '../services/types'
import { foldWindows } from './hours'

const w = (dayOfWeek: WeeklyWindow['dayOfWeek'], start: string, end: string): WeeklyWindow => ({ dayOfWeek, start, end })

describe('foldWindows', () => {
  it('merges adjacent days with equal times and keeps different days apart', () => {
    const weekdays = (['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY'] as const).map((d) => w(d, '07:00', '21:00'))
    expect(foldWindows([...weekdays, w('SATURDAY', '09:00', '14:00')])).toBe('Mon-Fri 07:00-21:00 · Sat 09:00-14:00')
  })

  it('does not merge days that are not adjacent', () => {
    expect(foldWindows([w('MONDAY', '08:00', '10:00'), w('WEDNESDAY', '08:00', '10:00')])).toBe('Mon 08:00-10:00 · Wed 08:00-10:00')
  })

  it('returns an empty string without windows', () => {
    expect(foldWindows([])).toBe('')
  })
})
