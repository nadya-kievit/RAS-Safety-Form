import { apiRequest } from './api.js'
import { mapUser } from './mappers.js'

export async function getUsers() {
  const users = await apiRequest('/users')
  return users.map(mapUser)
}

export async function createUser(user) {
  const created = await apiRequest('/users', {
    method: 'POST',
    body: JSON.stringify({
      first_name: user.firstName,
      last_name: user.lastName,
      username: user.username,
      password: user.password,
      role: user.role,
    }),
  })
  return mapUser(created)
}

export async function setUserActive(userId, active) {
  const updated = await apiRequest(`/users/${userId}/active`, {
    method: 'PATCH',
    body: JSON.stringify({ active }),
  })
  return mapUser(updated)
}
