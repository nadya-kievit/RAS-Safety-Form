import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import PasswordInput from '../components/forms/PasswordInput.jsx'
import PasswordRequirements from '../components/forms/PasswordRequirements.jsx'
import { useAuth } from '../context/auth.js'
import {
  PASSWORD_MAX_LENGTH,
  PASSWORD_MIN_LENGTH,
  validateNewPassword,
} from '../utils/password.js'

const emptyPasswords = {
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
}

function RequiredPasswordChangePage() {
  const { user, isAuthLoading, changePassword, logout } = useAuth()
  const navigate = useNavigate()
  const [passwords, setPasswords] = useState(emptyPasswords)
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthLoading) {
    return <div className="auth-loading" role="status">Restoring your session...</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!user.mustChangePassword) {
    return <Navigate to={user.role === 'admin' ? '/admin' : '/framer'} replace />
  }

  function updateField(event) {
    const { name, value } = event.target
    setPasswords((current) => ({ ...current, [name]: value }))
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')

    const passwordError = validateNewPassword(
      passwords.newPassword,
      passwords.confirmNewPassword,
    )
    if (passwordError) {
      setError(passwordError)
      return
    }

    setIsSubmitting(true)
    try {
      const updatedUser = await changePassword(passwords)
      navigate(updatedUser.role === 'admin' ? '/admin' : '/framer', { replace: true })
    } catch (requestError) {
      setError(requestError.message || 'Could not update your password.')
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleSignOut() {
    await logout()
    navigate('/login', { replace: true })
  }

  return (
    <main className="auth-page">
      <form className="panel login-card" onSubmit={handleSubmit}>
        <h1 className="page-title">Create new password</h1>

        {error && <p className="message error" role="alert">{error}</p>}

        <div className="field-group">
          <label htmlFor="current-password">Temporary password</label>
          <PasswordInput
            id="current-password"
            name="currentPassword"
            autoComplete="current-password"
            value={passwords.currentPassword}
            onChange={updateField}
            required
          />
        </div>

        <div className="field-group">
          <label htmlFor="new-password">New password</label>
          <PasswordInput
            id="new-password"
            name="newPassword"
            autoComplete="new-password"
            minLength={PASSWORD_MIN_LENGTH}
            maxLength={PASSWORD_MAX_LENGTH}
            value={passwords.newPassword}
            onChange={updateField}
            required
          />
          <PasswordRequirements />
        </div>

        <div className="field-group">
          <label htmlFor="confirm-new-password">Confirm new password</label>
          <PasswordInput
            id="confirm-new-password"
            name="confirmNewPassword"
            autoComplete="new-password"
            minLength={PASSWORD_MIN_LENGTH}
            maxLength={PASSWORD_MAX_LENGTH}
            value={passwords.confirmNewPassword}
            onChange={updateField}
            required
          />
        </div>

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Updating...' : 'Update password'}
        </button>
      </form>
    </main>
  )
}

export default RequiredPasswordChangePage
