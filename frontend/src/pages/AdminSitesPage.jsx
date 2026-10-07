import { useEffect, useRef, useState } from 'react'
import { ChevronRight, Plus, X } from 'lucide-react'
import { Link } from 'react-router-dom'
import {
  createSite,
  getAllSites,
  getChecklistForSite,
  renameSite,
  setSiteActive,
  updateChecklist,
} from '../services/siteService.js'

function sortSites(sites) {
  return [...sites].sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { sensitivity: 'base' }),
  )
}

function SiteStatus({ site, className = '' }) {
  return (
    <span className={`status-badge ${className} ${site.active ? 'active' : 'inactive'}`}>
      {site.active ? 'Active' : 'Inactive'}
    </span>
  )
}

function AdminSitesPage() {
  const [sites, setSites] = useState([])
  // null = site list, 'create' = new site form, otherwise the site being edited.
  const [editing, setEditing] = useState(null)
  const [siteName, setSiteName] = useState('')
  const [checklistName, setChecklistName] = useState('')
  const [items, setItems] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingChecklist, setIsLoadingChecklist] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [updatingSiteId, setUpdatingSiteId] = useState(null)
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')
  const nextItemKey = useRef(0)

  function newItem(text = '', id = null) {
    nextItemKey.current += 1
    return { key: nextItemKey.current, id, text }
  }

  useEffect(() => {
    let ignore = false
    getAllSites()
      .then((result) => {
        if (!ignore) setSites(sortSites(result))
      })
      .catch((requestError) => {
        if (!ignore) setError(requestError.message || 'Could not load sites.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => { ignore = true }
  }, [])

  function closeForm() {
    setEditing(null)
    setSiteName('')
    setChecklistName('')
    setItems([])
    setFormError('')
  }

  function openCreateForm() {
    setSuccess('')
    setSiteName('')
    setItems([newItem()])
    setEditing('create')
  }

  async function openEditForm(site) {
    setSuccess('')
    setError('')
    setFormError('')
    setSiteName(site.name)
    setChecklistName('')
    setItems([])
    setEditing(site)
    setIsLoadingChecklist(true)
    try {
      const checklist = await getChecklistForSite(site.id)
      setChecklistName(checklist.name)
      setItems(checklist.items.map((item) => newItem(item.item, item.id)))
    } catch (requestError) {
      setFormError(requestError.message || 'Could not load the checklist.')
    } finally {
      setIsLoadingChecklist(false)
    }
  }

  function updateItem(key, text) {
    setItems((current) => current.map((item) => (
      item.key === key ? { ...item, text } : item
    )))
  }

  function removeItem(key) {
    setItems((current) => current.filter((item) => item.key !== key))
  }

  function addItem() {
    setItems((current) => [...current, newItem()])
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError('')

    const name = siteName.trim()
    const filledItems = items
      .map((item) => ({ ...item, text: item.text.trim() }))
      .filter((item) => item.text)
    if (!name) {
      setFormError('Enter a site name.')
      return
    }
    if (filledItems.length === 0) {
      setFormError('Add at least one checklist item.')
      return
    }

    setIsSaving(true)
    try {
      if (editing === 'create') {
        const created = await createSite({
          name,
          checklistItems: filledItems.map((item) => item.text),
        })
        setSites((current) => sortSites([...current, created]))
        setSuccess(`${created.name} was created.`)
      } else {
        let updatedSite = editing
        if (name !== editing.name) {
          updatedSite = await renameSite(editing.id, name)
        }
        await updateChecklist(editing.id, {
          name: checklistName.trim() || `${name} Safety Checklist`,
          items: filledItems.map((item) => ({ id: item.id, item: item.text })),
        })
        setSites((current) => sortSites(current.map((site) => (
          site.id === updatedSite.id ? updatedSite : site
        ))))
        setSuccess(`${updatedSite.name} was updated.`)
      }
      closeForm()
    } catch (requestError) {
      setFormError(requestError.message || 'Could not save the site.')
    } finally {
      setIsSaving(false)
    }
  }

  async function handleActivation(site) {
    setError('')
    setSuccess('')
    setUpdatingSiteId(site.id)
    try {
      const updated = await setSiteActive(site.id, !site.active)
      setSites((current) => current.map((item) => (
        item.id === updated.id ? updated : item
      )))
      setSuccess(`${updated.name} was ${updated.active ? 'activated' : 'deactivated'}.`)
    } catch (requestError) {
      setError(requestError.message || 'Could not update the site.')
    } finally {
      setUpdatingSiteId(null)
    }
  }

  const isFormOpen = editing !== null
  const isCreating = editing === 'create'

  return (
    <section className={`content-page users-page sites-page${isFormOpen ? ' create-user-page' : ''}`}>
      <div className="page-heading">
        <div className="page-title-group">
          <h1 className="page-title">
            {isCreating ? 'Create Site' : isFormOpen ? 'Edit Site' : 'Manage Sites'}
          </h1>
          {isFormOpen && (
            <nav className="page-breadcrumb" aria-label="Breadcrumb">
              <Link to="/admin/sites" onClick={closeForm}>Manage Sites</Link>
              <ChevronRight aria-hidden="true" />
              <span aria-current="page">{isCreating ? 'Create Site' : 'Edit Site'}</span>
            </nav>
          )}
        </div>
        {!isFormOpen && (
          <button className="users-create-button" type="button" onClick={openCreateForm}>
            <Plus aria-hidden="true" />
            <span>Create site</span>
          </button>
        )}
      </div>

      {success && <p className="message success" role="status">{success}</p>}
      {error && <p className="message error" role="alert">{error}</p>}

      {isFormOpen && (
        <form className="panel form-stack" onSubmit={handleSubmit}>
          <h2 className="sr-only">{isCreating ? 'Create a new site' : 'Edit site'}</h2>
          {formError && <p className="message error" role="alert">{formError}</p>}

          <div className="field-group">
            <label htmlFor="site-name">Site name</label>
            <input
              id="site-name"
              value={siteName}
              onChange={(event) => setSiteName(event.target.value)}
              maxLength="150"
              required
            />
          </div>

          {!isCreating && (
            <div className="field-group">
              <label htmlFor="checklist-name">Checklist name</label>
              <input
                id="checklist-name"
                value={checklistName}
                onChange={(event) => setChecklistName(event.target.value)}
                maxLength="150"
                disabled={isLoadingChecklist}
              />
            </div>
          )}

          <fieldset>
            <legend>Checklist items</legend>
            {isLoadingChecklist ? (
              <p>Loading checklist...</p>
            ) : (
              <>
                <ul className="checklist-editor">
                  {items.map((item, index) => (
                    <li key={item.key}>
                      <input
                        aria-label={`Checklist item ${index + 1}`}
                        value={item.text}
                        onChange={(event) => updateItem(item.key, event.target.value)}
                        maxLength="500"
                      />
                      <button
                        className="secondary checklist-remove-button"
                        type="button"
                        aria-label={`Remove checklist item ${index + 1}`}
                        onClick={() => removeItem(item.key)}
                      >
                        <X aria-hidden="true" />
                      </button>
                    </li>
                  ))}
                </ul>
                <button className="secondary checklist-add-button" type="button" onClick={addItem}>
                  <Plus aria-hidden="true" />
                  <span>Add item</span>
                </button>
              </>
            )}
          </fieldset>

          <div className="form-actions">
            <button type="submit" disabled={isSaving || isLoadingChecklist}>
              {isSaving ? 'Saving...' : isCreating ? 'Create site' : 'Save changes'}
            </button>
            <button className="secondary" type="button" onClick={closeForm}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {!isFormOpen && isLoading && <p>Loading sites...</p>}
      {!isFormOpen && !isLoading && sites.length === 0 && !error && <p>No sites found.</p>}
      {!isFormOpen && !isLoading && sites.length > 0 && (
        <>
          <div className="table-scroll data-table-card users-table-card sites-table-card">
            <table>
              <thead>
                <tr>
                  <th>Site</th>
                  <th>Status</th>
                  <th><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {sites.map((site) => (
                  <tr key={site.id}>
                    <td>{site.name}</td>
                    <td><SiteStatus site={site} /></td>
                    <td>
                      <div className="site-actions">
                        <button
                          className="secondary compact-button user-status-button"
                          type="button"
                          onClick={() => openEditForm(site)}
                        >
                          Edit
                        </button>
                        <button
                          className="secondary compact-button user-status-button"
                          type="button"
                          onClick={() => handleActivation(site)}
                          disabled={updatingSiteId === site.id}
                        >
                          {updatingSiteId === site.id
                            ? 'Updating...'
                            : site.active ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="users-card-list" role="list">
            {sites.map((site) => (
              <article className="user-card" key={site.id} role="listitem">
                <div className="user-card-identity">
                  <strong>{site.name}</strong>
                </div>
                <div className="site-card-meta">
                  <SiteStatus site={site} className="user-card-status" />
                  <button
                    className="user-card-action site-card-edit"
                    type="button"
                    onClick={() => openEditForm(site)}
                  >
                    Edit
                  </button>
                  <button
                    className="user-card-action"
                    type="button"
                    onClick={() => handleActivation(site)}
                    disabled={updatingSiteId === site.id}
                  >
                    {updatingSiteId === site.id
                      ? 'Updating...'
                      : site.active ? 'Deactivate' : 'Activate'}
                  </button>
                </div>
              </article>
            ))}
          </div>
        </>
      )}
    </section>
  )
}

export default AdminSitesPage
