const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')

const CSRF_HEADER = 'X-CSRF-Token'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS'])

// The session-bound CSRF token is issued by /auth/login and /auth/me and held in memory only.
let csrfToken = null

export class ApiError extends Error {
  constructor(message, status, details) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

export async function apiRequest(path, options = {}) {
  const headers = new Headers(options.headers)
  const isFormData = options.body instanceof FormData

  if (options.body && !isFormData && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const method = (options.method || 'GET').toUpperCase()
  if (csrfToken && !SAFE_METHODS.has(method)) {
    headers.set(CSRF_HEADER, csrfToken)
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  })

  const issuedToken = response.headers.get(CSRF_HEADER)
  if (issuedToken) csrfToken = issuedToken

  const contentType = response.headers.get('content-type') || ''
  const data = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    if (response.status === 401) {
      csrfToken = null
      window.dispatchEvent(new Event('ras:authentication-required'))
    }

    const message = data?.message || data || `Request failed (${response.status})`
    throw new ApiError(message, response.status, data)
  }

  return data
}
