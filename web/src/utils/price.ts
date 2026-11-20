/** Money is a decimal string with 2 places ("12.50"). Computed in integer cents to avoid float errors. */
function toCents(amount: string): number {
  const [whole, frac = ''] = amount.split('.')
  return Number(whole) * 100 + Number(frac.padEnd(2, '0').slice(0, 2))
}

/** pricePerHour × minutes / 60, rounded HALF_UP to 2 decimals (booking-api.md D11). */
export function priceFor(pricePerHour: string, minutes: number): string {
  const cents = Math.floor((toCents(pricePerHour) * minutes * 2 + 60) / 120)
  return `${Math.floor(cents / 100)}.${String(cents % 100).padStart(2, '0')}`
}
