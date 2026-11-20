import type { DayName, WeeklyWindow } from '../services/types'

const DAY_ORDER: DayName[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
const DAY_SHORT: Record<DayName, string> = {
  MONDAY: 'Mon', TUESDAY: 'Tue', WEDNESDAY: 'Wed', THURSDAY: 'Thu', FRIDAY: 'Fri', SATURDAY: 'Sat', SUNDAY: 'Sun',
}

/** Folds the weekly windows into one line: "Mon-Fri 07:00-21:00 · Sat 09:00-14:00". */
export function foldWindows(windows: WeeklyWindow[]): string {
  const byDay = DAY_ORDER.map((day) => ({
    day,
    times: windows.filter((w) => w.dayOfWeek === day).map((w) => `${w.start}-${w.end}`).join(', '),
  })).filter((d) => d.times)
  const parts: string[] = []
  for (let i = 0; i < byDay.length;) {
    let j = i
    // Merge only directly adjacent days with identical times.
    while (
      j + 1 < byDay.length
      && byDay[j + 1].times === byDay[i].times
      && DAY_ORDER.indexOf(byDay[j + 1].day) === DAY_ORDER.indexOf(byDay[j].day) + 1
    ) j++
    const label = j > i ? `${DAY_SHORT[byDay[i].day]}-${DAY_SHORT[byDay[j].day]}` : DAY_SHORT[byDay[i].day]
    parts.push(`${label} ${byDay[i].times}`)
    i = j + 1
  }
  return parts.join(' · ')
}
