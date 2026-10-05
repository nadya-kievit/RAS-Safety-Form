import { useMemo, useState } from 'react'
import * as authService from '../services/authService.js'
import { AuthContext } from './auth.js'

const STORAGE_KEY = 'ras-authenticated-user'
function readStoredUser() {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY)
    return stored ? JSON.parse(stored) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser)

  async function login(credentials) {
    const authenticatedUser = await authService.login(credentials)
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(authenticatedUser))
    setUser(authenticatedUser)
    return authenticatedUser
  }

  async function logout() {
    try {
      await authService.logout()
    } catch {
      // Clear local state even when the backend session has already expired.
    } finally {
      sessionStorage.removeItem(STORAGE_KEY)
      setUser(null)
    }
  }

  async function updateProfile(profile) {
    const updatedUser = await authService.updateProfile(profile)
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(updatedUser))
    setUser(updatedUser)
    return updatedUser
  }

  const value = useMemo(
    () => ({ user, login, logout, updateProfile }),
    [user],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
