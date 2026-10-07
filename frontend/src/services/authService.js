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

export async function updateProfile(profile) {
  const user = await apiRequest('/auth/me', {
    method: 'PATCH',
    body: JSON.stringify({
      username: profile.username,
      first_name: profile.firstName,
      last_name: profile.lastName,
    }),
  })
  return mapUser(user)
}

export async function changePassword(passwords) {
  const user = await apiRequest('/auth/me/password', {
    method: 'POST',
    body: JSON.stringify({
      current_password: passwords.currentPassword,
      new_password: passwords.newPassword,
      confirm_new_password: passwords.confirmNewPassword,
    }),
  })
  return mapUser(user)
}
