import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AdminSitesPage from './AdminSitesPage.jsx'

vi.mock('../services/siteService.js', () => ({
  getAllSites: vi.fn(),
  getChecklistForSite: vi.fn(),
  createSite: vi.fn(),
  renameSite: vi.fn(),
  setSiteActive: vi.fn(),
  updateChecklist: vi.fn(),
}))

const siteService = await import('../services/siteService.js')

const kestrel = { id: 1, name: 'Kestrel Ridge', active: true }
const harbour = { id: 2, name: 'Harbour View', active: false }

function renderPage() {
  render(
    <MemoryRouter>
      <AdminSitesPage />
    </MemoryRouter>,
  )
}

// The page renders both a table and a phone card list, so scope queries to the table.
function tableRow(name) {
  return within(screen.getByRole('table')).getByRole('row', { name: new RegExp(name) })
}

describe('AdminSitesPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    siteService.getAllSites.mockResolvedValue([kestrel, harbour])
    siteService.getChecklistForSite.mockResolvedValue({
      id: 4,
      name: 'Kestrel Checklist',
      items: [
        { id: 10, item: 'Hard hat worn' },
        { id: 11, item: 'Vest worn' },
      ],
    })
  })

  it('lists active and inactive sites', async () => {
    renderPage()

    expect(await within(await screen.findByRole('table')).findByText('Kestrel Ridge'))
      .toBeInTheDocument()
    expect(within(tableRow('Kestrel Ridge')).getByText('Active')).toBeInTheDocument()
    expect(within(tableRow('Harbour View')).getByText('Inactive')).toBeInTheDocument()
  })

  it('activates and deactivates sites', async () => {
    const user = userEvent.setup()
    siteService.setSiteActive.mockResolvedValue({ ...kestrel, active: false })
    renderPage()
    await screen.findByRole('table')

    await user.click(within(tableRow('Kestrel Ridge')).getByRole('button', { name: 'Deactivate' }))

    expect(siteService.setSiteActive).toHaveBeenCalledWith(1, false)
    expect(await screen.findByText('Kestrel Ridge was deactivated.')).toBeInTheDocument()
  })

  it('creates a site with checklist items and ignores blank rows', async () => {
    const user = userEvent.setup()
    siteService.createSite.mockResolvedValue({ id: 3, name: 'Maple Court', active: true })
    renderPage()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: /Create site/ }))
    await user.type(screen.getByLabelText('Site name'), 'Maple Court')
    await user.type(screen.getByLabelText('Checklist item 1'), 'Hard hat worn')
    await user.click(screen.getByRole('button', { name: /Add item/ }))
    await user.click(screen.getByRole('button', { name: /Add item/ }))
    await user.type(screen.getByLabelText('Checklist item 2'), '  Vest worn ')
    await user.click(screen.getByRole('button', { name: 'Create site' }))

    expect(siteService.createSite).toHaveBeenCalledWith({
      name: 'Maple Court',
      checklistItems: ['Hard hat worn', 'Vest worn'],
    })
    expect(await screen.findByText('Maple Court was created.')).toBeInTheDocument()
  })

  it('requires a site name and at least one checklist item', async () => {
    const user = userEvent.setup()
    renderPage()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: /Create site/ }))
    await user.type(screen.getByLabelText('Site name'), 'Maple Court')
    await user.click(screen.getByRole('button', { name: 'Create site' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Add at least one checklist item.')
    expect(siteService.createSite).not.toHaveBeenCalled()
  })

  it('renames a site and edits its checklist items', async () => {
    const user = userEvent.setup()
    siteService.renameSite.mockResolvedValue({ ...kestrel, name: 'Kestrel Heights' })
    siteService.updateChecklist.mockResolvedValue({ id: 4, name: 'Kestrel Checklist', items: [] })
    renderPage()
    await screen.findByRole('table')

    await user.click(within(tableRow('Kestrel Ridge')).getByRole('button', { name: 'Edit' }))
    const siteName = await screen.findByLabelText('Site name')
    expect(siteName).toHaveValue('Kestrel Ridge')
    await screen.findByDisplayValue('Hard hat worn')
    await user.clear(siteName)
    await user.type(siteName, 'Kestrel Heights')
    await user.clear(screen.getByLabelText('Checklist item 1'))
    await user.type(screen.getByLabelText('Checklist item 1'), 'Hard hat and gloves')
    await user.click(screen.getByRole('button', { name: 'Remove checklist item 2' }))
    await user.click(screen.getByRole('button', { name: /Add item/ }))
    await user.type(screen.getByLabelText('Checklist item 2'), 'Fall protection')
    await user.click(screen.getByRole('button', { name: 'Save changes' }))

    expect(siteService.renameSite).toHaveBeenCalledWith(1, 'Kestrel Heights')
    expect(siteService.updateChecklist).toHaveBeenCalledWith(1, {
      name: 'Kestrel Checklist',
      items: [
        { id: 10, item: 'Hard hat and gloves' },
        { id: null, item: 'Fall protection' },
      ],
    })
    expect(await screen.findByText('Kestrel Heights was updated.')).toBeInTheDocument()
  })

  it('shows server errors such as duplicate names', async () => {
    const user = userEvent.setup()
    siteService.createSite.mockRejectedValue(new Error('A site with that name already exists'))
    renderPage()
    await screen.findByRole('table')

    await user.click(screen.getByRole('button', { name: /Create site/ }))
    await user.type(screen.getByLabelText('Site name'), 'Kestrel Ridge')
    await user.type(screen.getByLabelText('Checklist item 1'), 'Hard hat worn')
    await user.click(screen.getByRole('button', { name: 'Create site' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('already exists')
  })
})
