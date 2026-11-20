import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { delay, http, HttpResponse } from 'msw'
import { beforeEach, describe, expect, it } from 'vitest'
import { resourceSummaries } from '../mocks/fixtures'
import { server } from '../mocks/server'
import { API, renderApp, signIn } from '../test/utils'

beforeEach(() => signIn())

function recordQueries() {
  const queries: string[] = []
  server.use(http.get(`${API}/resources`, ({ request }) => {
    queries.push(new URL(request.url).search)
    return HttpResponse.json(resourceSummaries)
  }))
  return queries
}

describe('ResourceCatalogPage', () => {
  it('shows a loading state, then the seed resources as links', async () => {
    server.use(http.get(`${API}/resources`, async () => {
      await delay(30)
      return HttpResponse.json(resourceSummaries)
    }))
    renderApp('/resources')

    expect(screen.getByRole('status')).toHaveTextContent('Loading resources')
    const link = await screen.findByRole('link', { name: /Cargo Bike 1/ })
    expect(link).toHaveAttribute('href', '/resources/1')
    expect(link).toHaveTextContent('View slots')
    const bike = screen.getByRole('article', { name: 'Cargo Bike 1' })
    expect(bike).toHaveTextContent('Mobile')
    expect(bike).toHaveTextContent('Bike shed, Building A')
    expect(bike).toHaveTextContent('CHF 5.00 / hour')
    expect(screen.getByText('3 resources')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Reset filters' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Project Room D2.14/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /3D Printer Lab/ })).toBeInTheDocument()
    expect(screen.queryByText(/Old Beamer/)).not.toBeInTheDocument()
  })

  it('shows the empty state', async () => {
    server.use(http.get(`${API}/resources`, () => HttpResponse.json([])))
    renderApp('/resources')
    expect(await screen.findByText('No resources found')).toBeInTheDocument()
  })

  it('shows the error state and recovers on Retry', async () => {
    let fail = true
    server.use(http.get(`${API}/resources`, () =>
      fail ? HttpResponse.text('boom', { status: 500 }) : HttpResponse.json(resourceSummaries),
    ))
    renderApp('/resources')

    expect(await screen.findByRole('alert')).toHaveTextContent('Something went wrong. Please try again.')
    fail = false
    await userEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(await screen.findByRole('link', { name: /Cargo Bike 1/ })).toBeInTheDocument()
  })

  it('sends the type filter as a query parameter', async () => {
    const queries = recordQueries()
    renderApp('/resources')
    await screen.findByRole('link', { name: /Cargo Bike 1/ })
    expect(queries).toEqual([''])

    const types = screen.getByRole('radiogroup', { name: 'Type' })
    await userEvent.click(within(types).getByRole('radio', { name: 'Mobile' }))
    await waitFor(() => expect(queries).toContain('?type=MOBILE'))
    expect(within(types).getByRole('radio', { name: 'Mobile' })).toBeChecked()
    expect(await screen.findByRole('button', { name: 'Reset filters' })).toBeInTheDocument()

    await userEvent.click(within(types).getByRole('radio', { name: 'Fixed' }))
    await waitFor(() => expect(queries).toContain('?type=FIXED'))

    await userEvent.click(screen.getByRole('button', { name: 'Reset filters' }))
    await waitFor(() => expect(queries.at(-1)).toBe(''))
    expect(within(types).getByRole('radio', { name: 'All types' })).toBeChecked()
    expect(screen.queryByRole('button', { name: 'Reset filters' })).not.toBeInTheDocument()

    // Clicking the active type again keeps it selected (the filter is never "none").
    await userEvent.click(within(types).getByRole('radio', { name: 'All types' }))
    expect(within(types).getByRole('radio', { name: 'All types' })).toBeChecked()
  })

  it('debounces the search box and sends q once', async () => {
    const queries = recordQueries()
    renderApp('/resources')
    await screen.findByRole('link', { name: /Cargo Bike 1/ })

    await userEvent.type(screen.getByRole('searchbox', { name: 'Search resources' }), 'room')
    await waitFor(() => expect(queries).toContain('?q=room'))
    expect(queries.filter((q) => q.startsWith('?q='))).toEqual(['?q=room'])
  })

  it('ignores a stale response that arrives after a newer one', async () => {
    server.use(http.get(`${API}/resources`, async ({ request }) => {
      const type = new URL(request.url).searchParams.get('type')
      if (type === 'MOBILE') {
        await delay(150)
        return HttpResponse.json([resourceSummaries[0]])
      }
      return HttpResponse.json([resourceSummaries[1]])
    }))
    renderApp('/resources')
    await screen.findByRole('link', { name: /Project Room/ })

    const types = screen.getByRole('radiogroup', { name: 'Type' })
    await userEvent.click(within(types).getByRole('radio', { name: 'Mobile' }))
    await userEvent.click(within(types).getByRole('radio', { name: 'Fixed' }))
    await screen.findByRole('link', { name: /Project Room/ })
    await delay(250)
    expect(screen.queryByRole('link', { name: /Cargo Bike/ })).not.toBeInTheDocument()
  })
})
