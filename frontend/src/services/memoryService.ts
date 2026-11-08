import { API_BASE_URL, ApiError, apiFetch } from './apiClient'

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
  },

  /**
   * Delete an existing memory's photo via DELETE /api/memories/{id}/photo.
   * Preserves the memory text and milestone.
   */
  async deletePhoto(id: number): Promise<MemoryResponse> {
    return apiFetch<MemoryResponse>(`/memories/${id}/photo`, {
      method: 'DELETE'
    })
  },

  /**
   * Upload a photo for an existing memory via POST /api/memories/{id}/photo.
   * Uses FormData and lets the browser automatically set the multipart/form-data boundary.
   */
  async uploadPhoto(id: number, file: File): Promise<MemoryResponse> {
    const formData = new FormData()
    formData.append('file', file)

    return apiFetch<MemoryResponse>(`/memories/${id}/photo`, {
      method: 'POST',
      body: formData
    })
  },

  /**
   * Fetch an authenticated photo as a binary Blob via GET /api/memories/{id}/photo.
   * Includes credentials to forward the HttpOnly JWT session cookie.
   */
  async getPhotoBlob(id: number): Promise<Blob> {
    const res = await fetch(`${API_BASE_URL}/memories/${id}/photo`, {
      method: 'GET',
      credentials: 'include'
    })

    if (!res.ok) {
      let errorMessage = `Failed to load photo (HTTP ${res.status})`
      try {
        const errorBody = await res.json()
        if (errorBody.message) {
          errorMessage = errorBody.message
        } else if (errorBody.error) {
          errorMessage = errorBody.error
        }
      } catch {
        // Response was not JSON
      }
      throw new ApiError(errorMessage, res.status)
    }

    return res.blob()
  }
}
