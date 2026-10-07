import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../context/auth.js'
import { ApiError } from '../services/api.js'
import LoginPage from './LoginPage.jsx'

async function submitLogin(login) {
  const user = userEvent.setup()
  render(
    <AuthContext.Provider value={{ user: null, isAuthLoading: false, login }}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
  await user.type(screen.getByLabelText('Username'), 'alex')
  await user.type(screen.getByLabelText('Password'), 'secret-pass1!')
  await user.click(screen.getByRole('button', { name: 'Login' }))
}

describe('LoginPage', () => {
  it('tells deactivated users their account has been deactivated', async () => {
    const login = vi.fn().mockRejectedValue(
      new ApiError('Your account has been deactivated', 403, {}),
    )

    await submitLogin(login)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Your account has been deactivated',
    )
  })

  it('shows a generic message for wrong credentials', async () => {
    const login = vi.fn().mockRejectedValue(
      new ApiError('Invalid username or password', 401, {}),
    )

    await submitLogin(login)

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid username or password.')
  })

  it('surfaces the rate limit message', async () => {
    const login = vi.fn().mockRejectedValue(
      new ApiError('Too many failed login attempts. Try again later.', 429, {}),
    )

    await submitLogin(login)

    expect(await screen.findByRole('alert')).toHaveTextContent('Too many failed login attempts')
  })
})
