import { apiFetch } from './apiClient'

export interface DreamRequest {
  title: string
  category: string
  description?: string
  targetDate?: string
  status?: string
}

export interface DreamResponse {
  id: number
  title: string
  description: string | null
  category: string
  targetDate: string | null
  status: string
  createdAt: string
  updatedAt: string
}

/**
 * Reusable TypeScript API service for Dream CRUD operations.
 * Automatically sends credentials and CSRF tokens on mutating requests.
 */
export const dreamApi = {
  /**
   * Fetch all dreams from the backend.
   */
  async getAll(): Promise<DreamResponse[]> {
    return apiFetch<DreamResponse[]>('/dreams', {
      method: 'GET'
    })
  },

  /**
   * Fetch a single dream by ID.
   */
  async getById(id: number): Promise<DreamResponse> {
    return apiFetch<DreamResponse>(`/dreams/${id}`, {
      method: 'GET'
    })
  },

  /**
   * Create a new dream via POST /api/dreams.
   */
  async create(data: DreamRequest): Promise<DreamResponse> {
    return apiFetch<DreamResponse>('/dreams', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    })
  },

  /**
   * Update an existing dream via PUT /api/dreams/{id}.
   */
  async update(id: number, data: DreamRequest): Promise<DreamResponse> {
    return apiFetch<DreamResponse>(`/dreams/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    })
  },

  /**
   * Delete a dream via DELETE /api/dreams/{id}.
   */
  async delete(id: number): Promise<void> {
    return apiFetch<void>(`/dreams/${id}`, {
      method: 'DELETE'
    })
  }
}
