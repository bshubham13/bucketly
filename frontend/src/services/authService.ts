import { apiFetch, initCsrf } from './apiClient'

export interface RegisterRequest {
  fullName: string
  email: string
  password: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface AuthResponse {
  id: number
  email: string
  fullName: string
  createdAt: string
}

export interface LogoutResponse {
  message: string
}

export const authService = {
  /**
   * Initializes the CSRF cookie by calling the backend's public health endpoint.
   */
  async initCsrf(): Promise<void> {
    await initCsrf()
  },

  /**
   * Restores the current user's authenticated session via GET /api/auth/me.
   * Returns AuthResponse if authenticated, or null if unauthenticated (HTTP 401).
   */
  async getMe(): Promise<AuthResponse | null> {
    try {
      return await apiFetch<AuthResponse>('/auth/me', {
        method: 'GET'
      })
    } catch (err: unknown) {
      if (err && typeof err === 'object' && 'status' in err && (err as { status: number }).status === 401) {
        return null
      }
      throw err
    }
  },

  /**
   * Authenticates user credentials via POST /api/auth/login.
   * The backend sets the HttpOnly bucketly_token cookie in the response.
   */
  async login(request: LoginRequest): Promise<AuthResponse> {
    return apiFetch<AuthResponse>('/auth/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    })
  },

  /**
   * Registers a new account via POST /api/auth/register.
   */
  async register(request: RegisterRequest): Promise<AuthResponse> {
    return apiFetch<AuthResponse>('/auth/register', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(request)
    })
  },

  /**
   * Logs out the user via POST /api/auth/logout.
   * The backend clears the HttpOnly bucketly_token cookie.
   */
  async logout(): Promise<LogoutResponse> {
    return apiFetch<LogoutResponse>('/auth/logout', {
      method: 'POST'
    })
  }
}

