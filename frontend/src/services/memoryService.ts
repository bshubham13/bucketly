import { apiFetch } from './apiClient'

export interface MemoryCreateRequest {
  dreamId: number
  completedDate: string
  story?: string
  photoPath?: string
  originalFilename?: string
}

export interface MemoryUpdateRequest {
  completedDate: string
  story?: string
  photoPath?: string
  originalFilename?: string
}

export interface MemoryResponse {
  id: number
  dreamId: number
  dreamTitle: string
  dreamCategory: string
  completedDate: string
  story: string | null
  photoPath: string | null
  originalFilename: string | null
  createdAt: string
  updatedAt: string
}

/**
 * Reusable TypeScript API service for Memory Wall CRUD operations.
 * Automatically sends credentials and CSRF tokens on mutating requests.
 */
export const memoryApi = {
  /**
   * Fetch all memories from the backend.
   */
  async getAll(): Promise<MemoryResponse[]> {
    return apiFetch<MemoryResponse[]>('/memories', {
      method: 'GET'
    })
  },

  /**
   * Fetch a single memory by ID.
   */
  async getById(id: number): Promise<MemoryResponse> {
    return apiFetch<MemoryResponse>(`/memories/${id}`, {
      method: 'GET'
    })
  },

  /**
   * Create a new memory via POST /api/memories.
   */
  async create(data: MemoryCreateRequest): Promise<MemoryResponse> {
    return apiFetch<MemoryResponse>('/memories', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    })
  },

  /**
   * Update an existing memory via PUT /api/memories/{id}.
   */
  async update(id: number, data: MemoryUpdateRequest): Promise<MemoryResponse> {
    return apiFetch<MemoryResponse>(`/memories/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    })
  },

  /**
   * Delete a memory via DELETE /api/memories/{id}.
   */
  async delete(id: number): Promise<void> {
    return apiFetch<void>(`/memories/${id}`, {
      method: 'DELETE'
    })
  }
}
