import { useEffect, useMemo, useState } from 'react'
import * as authService from '../services/authService.js'
import { AuthContext } from './auth.js'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [isAuthLoading, setIsAuthLoading] = useState(true)

  useEffect(() => {
    let ignore = false

    authService.getCurrentUser()
      .then((authenticatedUser) => {
        if (!ignore) setUser(authenticatedUser)
      })
      .catch(() => {
        if (!ignore) setUser(null)
      })
      .finally(() => {
        if (!ignore) setIsAuthLoading(false)
      })

    return () => { ignore = true }
  }, [])

  useEffect(() => {
    function clearExpiredSession() {
      setUser(null)
    }

    window.addEventListener('ras:authentication-required', clearExpiredSession)
    return () => window.removeEventListener('ras:authentication-required', clearExpiredSession)
  }, [])

  async function login(credentials) {
    const authenticatedUser = await authService.login(credentials)
    setUser(authenticatedUser)
    return authenticatedUser
  }

  async function logout() {
    try {
      await authService.logout()
    } catch {
      // Clear local state even when the backend session has already expired.
    } finally {
      setUser(null)
    }
  }

  async function updateProfile(profile) {
    const updatedUser = await authService.updateProfile(profile)
    setUser(updatedUser)
    return updatedUser
  }

  const value = useMemo(
    () => ({ user, isAuthLoading, login, logout, updateProfile }),
    [user, isAuthLoading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
