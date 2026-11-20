import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { renderApp, signIn } from '../test/utils'

beforeEach(() => signIn())

describe('AppLayout', () => {
  it('shows brand, name, role badge, breadcrumb and landmarks', async () => {
    renderApp('/bookings')
    expect(await screen.findByText('Sam Staff')).toBeInTheDocument()
    expect(screen.getByText('Staff')).toBeInTheDocument()
    expect(screen.getByText('University Resource Services')).toBeInTheDocument()
    const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' })
    expect(within(crumbs).getByText('My bookings')).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('main')).toBeInTheDocument()
    expect(screen.getByRole('banner')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Skip to main content' })).toHaveAttribute('href', '#main')
  })

  it('adds the resource level to the breadcrumb on the details page', async () => {
    renderApp('/resources/1')
    const crumbs = await screen.findByRole('navigation', { name: 'breadcrumb' })
    expect(within(crumbs).getByRole('link', { name: 'Resource catalog' })).toHaveAttribute('href', '/resources')
    expect(within(crumbs).getByText('Resource details')).toHaveAttribute('aria-current', 'page')
  })

  it('lists the catalog and bookings as links and Invoices as a disabled entry that is not a link', async () => {
    renderApp('/resources')
    const nav = await screen.findByRole('navigation', { name: 'Main' })
    expect(within(nav).getByRole('link', { name: 'Resource catalog' })).toHaveAttribute('href', '/resources')
    expect(within(nav).getByRole('link', { name: 'My bookings' })).toHaveAttribute('href', '/bookings')
    expect(within(nav).queryByRole('link', { name: /Invoices/ })).not.toBeInTheDocument()
    const invoices = within(nav).getByRole('button', { name: /Invoices/ })
    expect(invoices).toBeDisabled()
    expect(invoices).toHaveAttribute('aria-disabled', 'true')
    expect(nav).toHaveTextContent('Coming soon')
  })

  it('marks the active nav item', async () => {
    renderApp('/bookings')
    const nav = await screen.findByRole('navigation', { name: 'Main' })
    expect(within(nav).getByRole('link', { name: 'My bookings' })).toHaveAttribute('data-active', 'true')
    expect(within(nav).getByRole('link', { name: 'Resource catalog' })).toHaveAttribute('data-active', 'false')
  })

  it('signs out from the account menu', async () => {
    renderApp('/resources')
    await userEvent.click(await screen.findByRole('button', { name: /Sam Staff/ }))
    expect(await screen.findByRole('menuitem', { name: 'Sign out' })).toBeInTheDocument()
  })

  describe('below 768px', () => {
    const original = window.innerWidth
    beforeEach(() => { window.innerWidth = 500 })
    afterEach(() => { window.innerWidth = original })

    it('moves the navigation into a drawer opened by the menu button, and Esc closes it', async () => {
      renderApp('/resources')
      const button = await screen.findByRole('button', { name: 'Toggle navigation' })
      expect(screen.queryByRole('navigation', { name: 'Main' })).not.toBeInTheDocument()

      await userEvent.click(button)
      const drawer = await screen.findByRole('dialog')
      expect(within(drawer).getByRole('link', { name: 'My bookings' })).toBeInTheDocument()

      await userEvent.keyboard('{Escape}')
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
      await waitFor(() => expect(button).toHaveFocus()) // Radix restores focus on a timer
    })

    it('closes the drawer when a nav link is clicked', async () => {
      renderApp('/resources')
      await userEvent.click(await screen.findByRole('button', { name: 'Toggle navigation' }))
      await userEvent.click(within(await screen.findByRole('dialog')).getByRole('link', { name: 'My bookings' }))
      expect(await screen.findByRole('heading', { name: 'My bookings' })).toBeInTheDocument()
      expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    })
  })
})
