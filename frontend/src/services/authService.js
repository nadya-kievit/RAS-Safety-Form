import { apiRequest } from './api.js'
import { mapUser } from './mappers.js'

export async function login(credentials) {
  const user = await apiRequest('/auth/login', {
    method: 'POST',
    body: JSON.stringify(credentials),
  })
  return mapUser(user)
}

export function logout() {
  return apiRequest('/auth/logout', { method: 'POST' })
}

export async function getCurrentUser() {
  return mapUser(await apiRequest('/auth/me'))
}
