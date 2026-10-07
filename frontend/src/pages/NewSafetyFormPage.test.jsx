import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthContext } from '../context/auth.js'
import NewSafetyFormPage from './NewSafetyFormPage.jsx'

vi.mock('../services/siteService.js', () => ({
  getActiveSites: vi.fn(),
  getChecklistForSite: vi.fn(),
}))
vi.mock('../services/submissionService.js', () => ({
  submitSafetyForm: vi.fn(),
}))

const { getActiveSites, getChecklistForSite } = await import('../services/siteService.js')
const { submitSafetyForm } = await import('../services/submissionService.js')

const JPEG = [0xff, 0xd8, 0xff, 0xe0, 0x00, 0x10, 0x4a, 0x46, 0x49, 0x46, 0x00, 0x01]

async function openFormAndConfirmChecklist(user) {
  render(
    <AuthContext.Provider value={{ user: { id: 7, role: 'framer' } }}>
      <MemoryRouter>
        <NewSafetyFormPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
  await user.selectOptions(await screen.findByLabelText('Site'), '3')
  await user.click(await screen.findByLabelText('Hard hat worn'))
  await user.click(screen.getByLabelText('Vest worn'))
}

describe('NewSafetyFormPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    URL.createObjectURL = vi.fn(() => 'blob:preview')
    URL.revokeObjectURL = vi.fn()
    getActiveSites.mockResolvedValue([{ id: 3, name: 'Kestrel Ridge' }])
    getChecklistForSite.mockResolvedValue({
      id: 4,
      name: 'Checklist',
      items: [
        { id: 10, item: 'Hard hat worn' },
        { id: 11, item: 'Vest worn' },
      ],
    })
    submitSafetyForm.mockResolvedValue({})
  })

  it('requires at least one photo before submitting', async () => {
    const user = userEvent.setup()
    await openFormAndConfirmChecklist(user)

    await user.click(screen.getByRole('button', { name: 'Submit' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Add at least one photo before submitting.',
    )
    expect(submitSafetyForm).not.toHaveBeenCalled()
  })

  it('submits the confirmed checklist, exact time instant and every selected photo', async () => {
    const user = userEvent.setup()
    await openFormAndConfirmChecklist(user)
    const photos = [1, 2].map((n) => new File(
      [new Uint8Array(JPEG)],
      `site-${n}.jpg`,
      { type: 'image/jpeg', lastModified: n },
    ))
    await user.upload(screen.getByLabelText('Photos'), photos)
    await screen.findByAltText('Preview of site-2.jpg')

    await user.click(screen.getByRole('button', { name: 'Submit' }))

    await waitFor(() => expect(submitSafetyForm).toHaveBeenCalledTimes(1))
    const submission = submitSafetyForm.mock.calls[0][0]
    expect(submission.userId).toBe(7)
    expect(submission.siteId).toBe(3)
    expect(submission.checkedItemIds.sort()).toEqual([10, 11])
    expect(submission.photos.map((photo) => photo.name)).toEqual(['site-1.jpg', 'site-2.jpg'])
    expect(submission.formDate).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:00\.000Z$/)
  })

  it('does not submit when a removed photo leaves none selected', async () => {
    const user = userEvent.setup()
    await openFormAndConfirmChecklist(user)
    await user.upload(
      screen.getByLabelText('Photos'),
      new File([new Uint8Array(JPEG)], 'only.jpg', { type: 'image/jpeg' }),
    )
    await user.click(await screen.findByRole('button', { name: 'Remove only.jpg' }))

    await user.click(screen.getByRole('button', { name: 'Submit' }))

    expect(submitSafetyForm).not.toHaveBeenCalled()
  })
})
