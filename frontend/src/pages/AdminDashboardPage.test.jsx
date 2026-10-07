import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminDashboardPage from './AdminDashboardPage.jsx'

vi.mock('../services/siteService.js', () => ({ getActiveSites: vi.fn() }))
vi.mock('../services/submissionService.js', () => ({ getAllSubmissions: vi.fn() }))
vi.mock('../services/userService.js', () => ({ getUsers: vi.fn() }))
vi.mock('../components/submissions/SubmissionActivityChart.jsx', () => ({
  default: () => <div>chart</div>,
}))

const { getActiveSites } = await import('../services/siteService.js')
const { getAllSubmissions } = await import('../services/submissionService.js')
const { getUsers } = await import('../services/userService.js')

function user(id, firstName, lastName, role, active = true) {
  return { id, firstName, lastName, username: `${firstName}${id}`, role, active }
}

describe('AdminDashboardPage worker filter', () => {
  beforeEach(() => {
    getActiveSites.mockResolvedValue([{ id: 1, name: 'Kestrel Ridge' }])
    getAllSubmissions.mockResolvedValue([{
      id: 1,
      userId: 10,
      siteId: 1,
      formDate: '2026-10-01T15:30:00Z',
      user: user(10, 'Alex', 'Active', 'framer'),
      site: { id: 1, name: 'Kestrel Ridge' },
    }])
    getUsers.mockResolvedValue([
      user(10, 'Alex', 'Active', 'framer'),
      user(11, 'Sam', 'Silent', 'framer'),
      user(12, 'Dee', 'Departed', 'framer', false),
      user(13, 'Ada', 'Admin', 'admin'),
    ])
  })

  it('lists every active framer, including those without submissions', async () => {
    render(
      <MemoryRouter>
        <AdminDashboardPage />
      </MemoryRouter>,
    )

    const workerSelect = await screen.findByLabelText('Worker')
    const options = within(workerSelect).getAllByRole('option').map((option) => option.textContent)

    expect(options).toEqual(['All workers', 'Alex Active', 'Sam Silent'])
  })
})
