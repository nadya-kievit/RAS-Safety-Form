import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

function jsonResponse(body, { status = 200, headers = {} } = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json', ...headers },
  })
}

describe('apiRequest', () => {
  let fetchMock
  let apiRequest

  beforeEach(async () => {
    vi.resetModules()
    fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)
    ;({ apiRequest } = await import('./api.js'))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('sends session cookies and routes requests under /api', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({ ok: true })))

    await apiRequest('/sites')

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/sites')
    expect(options.credentials).toBe('include')
  })

  it('echoes the CSRF token on state-changing requests only', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({}, { headers: { 'X-CSRF-Token': 'token-1' } }))
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})))

    await apiRequest('/auth/me')
    await apiRequest('/sites')
    await apiRequest('/sites', { method: 'POST', body: '{}' })
    await apiRequest('/sites/1', { method: 'PATCH', body: '{}' })

    const headersOf = (call) => fetchMock.mock.calls[call][1].headers
    expect(headersOf(1).get('X-CSRF-Token')).toBeNull()
    expect(headersOf(2).get('X-CSRF-Token')).toBe('token-1')
    expect(headersOf(3).get('X-CSRF-Token')).toBe('token-1')
  })

  it('adopts a newly issued token after signing in again', async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({}, { headers: { 'X-CSRF-Token': 'old' } }))
    fetchMock.mockResolvedValueOnce(jsonResponse({}, { headers: { 'X-CSRF-Token': 'new' } }))
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})))

    await apiRequest('/auth/me')
    await apiRequest('/auth/login', { method: 'POST', body: '{}' })
    await apiRequest('/sites', { method: 'POST', body: '{}' })

    expect(fetchMock.mock.calls[2][1].headers.get('X-CSRF-Token')).toBe('new')
  })

  it('does not set a JSON content type for multipart uploads', async () => {
    fetchMock.mockImplementation(() => Promise.resolve(jsonResponse({})))

    await apiRequest('/safety-forms/submit', { method: 'POST', body: new FormData() })

    expect(fetchMock.mock.calls[0][1].headers.get('Content-Type')).toBeNull()
  })

  it('throws the server message and announces expired sessions on 401', async () => {
    const listener = vi.fn()
    window.addEventListener('ras:authentication-required', listener)
    fetchMock.mockImplementation(() => Promise.resolve(
      jsonResponse({ message: 'Authentication required' }, { status: 401 }),
    ))

    await expect(apiRequest('/auth/me')).rejects.toMatchObject({
      status: 401,
      message: 'Authentication required',
    })
    expect(listener).toHaveBeenCalledTimes(1)
    window.removeEventListener('ras:authentication-required', listener)
  })
})
