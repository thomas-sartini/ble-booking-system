import { apiRequest } from './apiClient'
import type { Availability, ResourceDetails, ResourceSummary, ResourceType } from './types'

export const resourceService = {
  listResources(params: { type?: ResourceType; q?: string } = {}, signal?: AbortSignal) {
    return apiRequest<ResourceSummary[]>('/resources', {
      query: { type: params.type, q: params.q?.trim() || undefined },
      signal,
    })
  },

  getResource(id: number | string, signal?: AbortSignal) {
    return apiRequest<ResourceDetails>(`/resources/${id}`, { signal })
  },

  getAvailability(id: number | string, from: string, to: string, signal?: AbortSignal) {
    return apiRequest<Availability>(`/resources/${id}/availability`, { query: { from, to }, signal })
  },
}
