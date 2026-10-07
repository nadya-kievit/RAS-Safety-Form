import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../context/auth.js'
import AdminUsersPage from './AdminUsersPage.jsx'

vi.mock('../services/userService.js', () => ({
  getUsers: vi.fn().mockResolvedValue([]),
  createUser: vi.fn(),
  setUserActive: vi.fn(),
}))

describe('AdminUsersPage create form', () => {
  it('shows the password requirements above the password fields', async () => {
    const user = userEvent.setup()
    render(
      <AuthContext.Provider value={{ user: { id: 1, role: 'admin' } }}>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </AuthContext.Provider>,
    )

    await user.click(await screen.findByRole('button', { name: /Create user/ }))

    const requirements = screen.getByText(/Use 8–20 characters/)
    const passwordField = screen.getByLabelText('Temporary password')
    expect(
      requirements.compareDocumentPosition(passwordField) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy()
  })
})
