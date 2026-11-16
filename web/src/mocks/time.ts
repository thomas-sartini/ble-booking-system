/** UTC offset ("+01:00") of the zone at the given instant, DST aware. */
export function offsetAt(ms: number, timeZone: string): string {
  const parts = new Intl.DateTimeFormat('en-US', { timeZone, timeZoneName: 'longOffset' }).formatToParts(new Date(ms))
  const name = parts.find((p) => p.type === 'timeZoneName')?.value ?? 'GMT'
  return name === 'GMT' ? '+00:00' : name.replace('GMT', '')
}

/** Zone offset on a local date (taken at noon). */
export function zoneOffset(date: string, timeZone: string): string {
  return offsetAt(Date.parse(`${date}T12:00:00Z`), timeZone)
}

/** ISO-8601 text with the zone's offset, like the backend returns: 2026-11-17T09:00:00+01:00 */
export function toZoneIso(ms: number, timeZone: string): string {
  const offset = offsetAt(ms, timeZone)
  const sign = offset.startsWith('-') ? -1 : 1
  const [h, m] = offset.slice(1).split(':').map(Number)
  const local = new Date(ms + sign * (h * 60 + m) * 60_000)
  return `${local.toISOString().slice(0, 19)}${offset}`
}
