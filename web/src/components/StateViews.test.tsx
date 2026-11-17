import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { ReactElement } from 'react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { BookingStatusBadge } from './BookingStatusBadge'
import { EmptyView, ErrorView, LoadingView } from './StateViews'

const wrap = (ui: ReactElement) => render(<MemoryRouter>{ui}</MemoryRouter>)

describe('StateViews', () => {
  it('loading shows the label as status and skeleton rows hidden from assistive technology', () => {
    const { container } = wrap(<LoadingView label="Loading things…" rows={3} />)
    expect(screen.getByRole('status')).toHaveTextContent('Loading things…')
    expect(container.querySelectorAll('[data-slot=skeleton]')).toHaveLength(3)
  })

  it('empty shows title, text and a call to action', () => {
    wrap(<EmptyView message="Nothing here" text="Try later" action={{ label: 'Browse resources', to: '/resources' }} />)
    expect(screen.getByText('Nothing here')).toBeInTheDocument()
    expect(screen.getByText('Try later')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Browse resources' })).toHaveAttribute('href', '/resources')
  })

  it('FORBIDDEN shows the access-denied view with the given message and a link back', () => {
    wrap(<ErrorView code="FORBIDDEN" message="Your account is not active. Please contact an administrator." />)
    expect(screen.getByRole('alert')).toHaveTextContent('Access denied')
    expect(screen.getByRole('alert')).toHaveTextContent('Your account is not active.')
    expect(screen.getByRole('link', { name: 'Back to resource catalog' })).toHaveAttribute('href', '/resources')
  })

  it('NETWORK_ERROR says the server cannot be reached and offers Retry', async () => {
    const onRetry = vi.fn()
    wrap(<ErrorView code="NETWORK_ERROR" message="Something went wrong. Please try again." onRetry={onRetry} />)
    expect(screen.getByRole('alert')).toHaveTextContent("Can't reach the server")
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(onRetry).toHaveBeenCalledOnce()
  })

  it('other errors show the message and Retry', () => {
    wrap(<ErrorView message="Something went wrong. Please try again." onRetry={() => {}} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Something went wrong')
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument()
  })
})

describe('BookingStatusBadge', () => {
  it('shows a text label for every status, with the icon hidden from assistive technology', () => {
    const { container } = render(
      <>
        <BookingStatusBadge status="BOOKED" /><BookingStatusBadge status="CHECKED_IN" />
        <BookingStatusBadge status="COMPLETED" /><BookingStatusBadge status="CANCELLED" /><BookingStatusBadge status="NO_SHOW" />
      </>,
    )
    for (const label of ['Booked', 'Checked in', 'Completed', 'Cancelled', 'No show']) {
      expect(screen.getByText(label)).toBeInTheDocument()
    }
    for (const svg of container.querySelectorAll('svg')) expect(svg).toHaveAttribute('aria-hidden', 'true')
  })
})
