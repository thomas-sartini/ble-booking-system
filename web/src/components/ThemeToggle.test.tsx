import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { ThemeProvider } from './ThemeProvider'
import { ThemeToggle } from './ThemeToggle'

afterEach(() => document.documentElement.classList.remove('dark'))

const open = async () => {
  render(<ThemeProvider><ThemeToggle /></ThemeProvider>)
  await userEvent.click(screen.getByRole('button', { name: 'Change theme' }))
}

describe('ThemeToggle', () => {
  it('starts with the system theme (light in jsdom) and offers light, dark and system', async () => {
    await open()
    expect(screen.getByRole('menuitemradio', { name: 'System' })).toBeChecked()
    expect(screen.getByRole('menuitemradio', { name: 'Light' })).toBeInTheDocument()
    expect(document.documentElement).not.toHaveClass('dark')
  })

  it('switches to dark, sets the dark class and remembers the choice', async () => {
    await open()
    await userEvent.click(screen.getByRole('menuitemradio', { name: 'Dark' }))
    expect(document.documentElement).toHaveClass('dark')
    expect(localStorage.getItem('theme')).toBe('dark')
  })

  it('restores a saved choice and can switch back to light', async () => {
    localStorage.setItem('theme', 'dark')
    await open()
    expect(document.documentElement).toHaveClass('dark')
    expect(screen.getByRole('menuitemradio', { name: 'Dark' })).toBeChecked()
    await userEvent.click(screen.getByRole('menuitemradio', { name: 'Light' }))
    expect(document.documentElement).not.toHaveClass('dark')
  })
})
