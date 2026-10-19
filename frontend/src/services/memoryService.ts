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

const API_BASE_URL = 'http://localhost:8080/api/memories'

/**
 * Handles HTTP response parsing and standardized error extraction.
 */
async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let errorMessage = `HTTP ${res.status}: ${res.statusText}`
    try {
      const errorBody = await res.json()
      if (errorBody.message) {
        errorMessage = errorBody.message
      } else if (errorBody.error) {
        errorMessage = errorBody.error
      }
    } catch {
      // Body was not JSON; use default message
    }
    throw new Error(errorMessage)
  }

  // HTTP 204 No Content has no body
  if (res.status === 204) {
    return {} as T
  }

  return res.json()
}

/**
 * Reusable TypeScript API service for Memory Wall CRUD operations using native fetch.
 */
export const memoryApi = {
  /**
   * Fetch all memories from the backend.
   */
  async getAll(): Promise<MemoryResponse[]> {
    const res = await fetch(API_BASE_URL, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
      }
    })
    return handleResponse<MemoryResponse[]>(res)
  },

  /**
   * Fetch a single memory by ID.
   */
  async getById(id: number): Promise<MemoryResponse> {
    const res = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
      }
    })
    return handleResponse<MemoryResponse>(res)
  },

  /**
   * Create a new memory via POST /api/memories.
   */
  async create(data: MemoryCreateRequest): Promise<MemoryResponse> {
    const res = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify(data)
    })
    return handleResponse<MemoryResponse>(res)
  },

  /**
   * Update an existing memory via PUT /api/memories/{id}.
   */
  async update(id: number, data: MemoryUpdateRequest): Promise<MemoryResponse> {
    const res = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify(data)
    })
    return handleResponse<MemoryResponse>(res)
  },

  /**
   * Delete a memory via DELETE /api/memories/{id}.
   */
  async delete(id: number): Promise<void> {
    const res = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'DELETE',
      headers: {
        Accept: 'application/json'
      }
    })
    await handleResponse<void>(res)
  }
}

