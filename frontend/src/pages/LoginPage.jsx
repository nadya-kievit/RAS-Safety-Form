import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import PasswordInput from '../components/forms/PasswordInput.jsx'
import { useAuth } from '../context/auth.js'

function LoginPage() {
  const { user, isAuthLoading, login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthLoading) {
    return <div className="auth-loading" role="status">Restoring your session...</div>
  }

  if (user) {
    return <Navigate to={user.mustChangePassword
      ? '/change-password'
      : user.role === 'admin' ? '/admin' : '/framer'} replace />
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)

    try {
      const authenticatedUser = await login({ username, password })
      const destination = authenticatedUser.mustChangePassword
        ? '/change-password'
        : authenticatedUser.role === 'admin' ? '/admin' : '/framer'
      navigate(destination, {
        replace: true,
      })
    } catch (requestError) {
      setError(requestError.status === 401
        ? 'Invalid username or password.'
        : requestError.message || 'Could not log in.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <form className="panel login-card" onSubmit={handleSubmit}>
        <h1 className="page-title">Login</h1>
        <p className="login-subtitle">RAS Safety Forms</p>
        {error && <p className="message error" role="alert">{error}</p>}

        <div className="field-group">
          <label htmlFor="username">Username</label>
          <input
            id="username"
            name="username"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
        </div>

        <div className="field-group">
          <label htmlFor="password">Password</label>
          <PasswordInput
            id="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </div>

        <button type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Logging in...' : 'Login'}
        </button>
      </form>
    </main>
  )
}

export default LoginPage
