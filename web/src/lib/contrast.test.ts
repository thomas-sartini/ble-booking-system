/// <reference types="node" />
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'

// The theme variables in the Tailwind entry CSS are the only colour definitions: check them directly.
const css = readFileSync(join(process.cwd(), 'src/index.css'), 'utf8') // vitest runs from web/
const THEMES = {
  light: css.slice(css.indexOf(':root {'), css.indexOf('.dark {')),
  dark: css.slice(css.indexOf('.dark {'), css.indexOf('@layer base')),
}
type Theme = keyof typeof THEMES

function oklch(theme: Theme, name: string): [number, number, number] {
  const match = THEMES[theme].match(new RegExp(`--${name}:\\s*oklch\\(([^)]+)\\)`))
  if (!match) throw new Error(`theme variable --${name} not found as oklch()`)
  const [l, c, h] = match[1].split(/\s+/).map(Number)
  return [l, c, h]
}

/** oklch -> linear sRGB (clamped), then WCAG relative luminance. */
function luminance([l, c, h]: [number, number, number]): number {
  const a = c * Math.cos((h * Math.PI) / 180)
  const b = c * Math.sin((h * Math.PI) / 180)
  const l_ = (l + 0.3963377774 * a + 0.2158037573 * b) ** 3
  const m_ = (l - 0.1055613458 * a - 0.0638541728 * b) ** 3
  const s_ = (l - 0.0894841775 * a - 1.291485548 * b) ** 3
  const clamp = (v: number) => Math.min(1, Math.max(0, v))
  const r = clamp(4.0767416621 * l_ - 3.3077115913 * m_ + 0.2309699292 * s_)
  const g = clamp(-1.2684380046 * l_ + 2.6097574011 * m_ - 0.3413193965 * s_)
  const bl = clamp(-0.0041960863 * l_ - 0.7034186147 * m_ + 1.707614701 * s_)
  return 0.2126 * r + 0.7152 * g + 0.0722 * bl
}

function contrast(theme: Theme, fg: string, bg: string): number {
  const [hi, lo] = [luminance(oklch(theme, fg)), luminance(oklch(theme, bg))].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}

// [foreground, background] for every text pair the UI uses.
const TEXT_PAIRS: [string, string][] = [
  ['foreground', 'background'], ['foreground', 'card'], ['foreground', 'muted'], ['foreground', 'accent'],
  ['muted-foreground', 'background'], ['muted-foreground', 'card'], ['muted-foreground', 'muted'],
  ['primary-foreground', 'primary'], ['secondary-foreground', 'secondary'],
  ['success-foreground', 'success'], ['info-foreground', 'info'], ['warning-foreground', 'warning'],
  ['danger-foreground', 'danger'], ['danger-foreground', 'card'],
  ['sidebar-foreground', 'sidebar'], ['sidebar-accent-foreground', 'sidebar-accent'],
]

describe.each(Object.keys(THEMES) as Theme[])('%s theme contrast (WCAG AA)', (theme) => {
  it.each(TEXT_PAIRS)('%s on %s is at least 4.5:1', (fg, bg) => {
    expect(contrast(theme, fg, bg)).toBeGreaterThanOrEqual(4.5)
  })

  it('white text on the destructive button colour is at least 4.5:1', () => {
    const white = (luminance([1, 0, 0]) + 0.05) / (luminance(oklch(theme, 'destructive')) + 0.05)
    expect(white).toBeGreaterThanOrEqual(4.5)
  })

  it('the focus ring is at least 3:1 against page and card (non-text contrast)', () => {
    expect(contrast(theme, 'ring', 'background')).toBeGreaterThanOrEqual(3)
    expect(contrast(theme, 'ring', 'card')).toBeGreaterThanOrEqual(3)
  })
})
