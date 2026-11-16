// Pure date helpers on "yyyy-MM-dd" strings. They use UTC arithmetic internally,
// so the browser time zone never shifts a calendar date.

function parse(date: string): Date {
  const [y, m, d] = date.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d))
}

export function formatDate(d: Date): string {
  return d.toISOString().slice(0, 10)
}

export function addDays(date: string, days: number): string {
  const d = parse(date)
  d.setUTCDate(d.getUTCDate() + days)
  return formatDate(d)
}

/** Monday of the week containing `date` (weeks run Monday to Sunday). */
export function mondayOf(date: string): string {
  const offset = (parse(date).getUTCDay() + 6) % 7
  return addDays(date, -offset)
}

/** Today's calendar date in the given IANA zone, as yyyy-MM-dd. */
export function todayIn(timeZone: string, now: Date = new Date()): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(now)
}

export function weekDayLabel(date: string): string {
  return new Intl.DateTimeFormat('en-GB', { weekday: 'short', timeZone: 'UTC' }).format(parse(date))
}

export function shortDateLabel(date: string): string {
  return new Intl.DateTimeFormat('en-GB', { day: 'numeric', month: 'short', timeZone: 'UTC' }).format(parse(date))
}

/** "HH:mm" taken from the ISO text, so the resource's local time is shown regardless of browser zone. */
export function isoTime(iso: string): string {
  return iso.slice(11, 16)
}
