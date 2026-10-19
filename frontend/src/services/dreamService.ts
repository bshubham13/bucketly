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

const API_BASE_URL = 'http://localhost:8080/api/dreams'

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
 * Reusable TypeScript API service for Dream CRUD operations using native fetch.
 */
export const dreamApi = {
  /**
   * Fetch all dreams from the backend.
   */
  async getAll(): Promise<DreamResponse[]> {
    const res = await fetch(API_BASE_URL, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
      }
    })
    return handleResponse<DreamResponse[]>(res)
  },

  /**
   * Fetch a single dream by ID.
   */
  async getById(id: number): Promise<DreamResponse> {
    const res = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'GET',
      headers: {
        Accept: 'application/json'
      }
    })
    return handleResponse<DreamResponse>(res)
  },

  /**
   * Create a new dream via POST /api/dreams.
   */
  async create(data: DreamRequest): Promise<DreamResponse> {
    const res = await fetch(API_BASE_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify(data)
    })
    return handleResponse<DreamResponse>(res)
  },

  /**
   * Update an existing dream via PUT /api/dreams/{id}.
   */
  async update(id: number, data: DreamRequest): Promise<DreamResponse> {
    const res = await fetch(`${API_BASE_URL}/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json'
      },
      body: JSON.stringify(data)
    })
    return handleResponse<DreamResponse>(res)
  },

  /**
   * Delete a dream via DELETE /api/dreams/{id}.
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

