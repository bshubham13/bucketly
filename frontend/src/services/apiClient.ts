/**
 * Shared API and CSRF utilities for Bucketly Frontend.
 */

export const API_BASE_URL = 'http://localhost:8080/api'

export class ApiError extends Error {
  status: number
  validationErrors?: Record<string, string>

  constructor(message: string, status: number, validationErrors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.validationErrors = validationErrors
  }
}

/**
 * Reads and decodes the XSRF-TOKEN cookie issued by Spring Security's CookieCsrfTokenRepository.
 * Returns null if not present or blank.
 */
export function getCsrfToken(): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/)
  if (!match || !match[1]) return null
  return decodeURIComponent(match[1]).trim()
}

/**
 * Initializes the CSRF cookie by performing a safe GET request to the public /api/health endpoint.
 */
export async function initCsrf(): Promise<void> {
  try {
    await fetch(`${API_BASE_URL}/health`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        Accept: 'application/json'
      }
    })
  } catch {
    // If backend is unreachable, downstream calls will catch and present friendly errors
  }
}

/**
 * Ensures a CSRF token is present in document.cookie. If missing, attempts initialization.
 * Retries once if the token is still missing after initialization.
 * Throws ApiError if the token cannot be obtained.
 */
export async function ensureCsrfToken(): Promise<string> {
  let token = getCsrfToken()
  if (token) return token

  // Attempt 1: Seed from /api/health
  await initCsrf()
  token = getCsrfToken()
  if (token) return token

  // Attempt 2: Brief wait and retry
  await new Promise((resolve) => setTimeout(resolve, 150))
  await initCsrf()
  token = getCsrfToken()
  if (token) return token

  throw new ApiError('CSRF protection token could not be initialized. Please check backend connection.', 403)
}

/**
 * Handles HTTP response parsing, error unwrapping, and empty body handling.
 */
export async function handleApiResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let errorMessage = `HTTP ${res.status}: ${res.statusText}`
    let validationErrors: Record<string, string> | undefined

    try {
      const errorBody = await res.json()
      if (errorBody.message) {
        errorMessage = errorBody.message
      } else if (errorBody.error) {
        errorMessage = errorBody.error
      }
      if (errorBody.validationErrors && typeof errorBody.validationErrors === 'object') {
        validationErrors = errorBody.validationErrors
      }
    } catch {
      // Body was not JSON; use default status message
    }

    throw new ApiError(errorMessage, res.status, validationErrors)
  }

  // HTTP 204 No Content has no body
  if (res.status === 204) {
    return {} as T
  }

  return res.json()
}

/**
 * Reusable fetch wrapper that:
 * 1. Automatically includes `credentials: 'include'` for HttpOnly cookies.
 * 2. On mutating requests (POST, PUT, DELETE, PATCH), ensures a valid X-XSRF-TOKEN header is sent.
 * 3. Never reads or exposes HttpOnly auth cookies.
 * 4. Never logs sensitive credentials, passwords, or tokens.
 */
export async function apiFetch<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method || 'GET').toUpperCase()
  const isMutating = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)

  const headers = new Headers(options.headers || {})
  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json')
  }

  if (isMutating) {
    const csrfToken = await ensureCsrfToken()
    headers.set('X-XSRF-TOKEN', csrfToken)
  }

  const url = endpoint.startsWith('http')
    ? endpoint
    : `${API_BASE_URL}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`

  const res = await fetch(url, {
    ...options,
    method,
    headers,
    credentials: 'include'
  })

  return handleApiResponse<T>(res)
}

