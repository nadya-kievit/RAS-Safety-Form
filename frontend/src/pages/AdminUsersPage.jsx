import { useEffect, useState } from 'react'
import { ChevronDown, ChevronRight } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/auth.js'
import {
  createUser,
  getUsers,
  setUserActive,
} from '../services/userService.js'

const emptyUser = {
  firstName: '',
  lastName: '',
  username: '',
  password: '',
  role: 'framer',
}

function sortUsers(users) {
  return [...users].sort((a, b) =>
    `${a.lastName} ${a.firstName}`.localeCompare(
      `${b.lastName} ${b.firstName}`,
      undefined,
      { sensitivity: 'base' },
    ),
  )
}

function displayRole(role) {
  return role === 'admin' ? 'Admin' : 'Framer'
}

function AdminUsersPage() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState([])
  const [newUser, setNewUser] = useState(emptyUser)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isCreating, setIsCreating] = useState(false)
  const [updatingUserId, setUpdatingUserId] = useState(null)
  const [error, setError] = useState('')
  const [formError, setFormError] = useState('')
  const [success, setSuccess] = useState('')

  useEffect(() => {
    let ignore = false
    getUsers()
      .then((result) => {
        if (!ignore) setUsers(sortUsers(result))
      })
      .catch((requestError) => {
        if (!ignore) setError(requestError.message || 'Could not load users.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => { ignore = true }
  }, [])

  function updateField(event) {
    const { name, value } = event.target
    setNewUser((current) => ({ ...current, [name]: value }))
  }

  function closeCreateForm() {
    setNewUser(emptyUser)
    setFormError('')
    setShowCreateForm(false)
  }

  async function handleCreate(event) {
    event.preventDefault()
    setFormError('')
    setSuccess('')
    setIsCreating(true)

    try {
      const created = await createUser(newUser)
      setUsers((current) => sortUsers([...current, created]))
      setSuccess(`User ${created.username} was created successfully.`)
      closeCreateForm()
    } catch (requestError) {
      setFormError(requestError.message || 'Could not create the user.')
    } finally {
      setIsCreating(false)
    }
  }

  async function handleActivation(user) {
    setError('')
    setSuccess('')
    setUpdatingUserId(user.id)

    try {
      const updated = await setUserActive(user.id, !user.active)
      setUsers((current) => current.map((item) =>
        item.id === updated.id ? updated : item,
      ))
      setSuccess(`${updated.username} was ${updated.active ? 'activated' : 'deactivated'}.`)
    } catch (requestError) {
      setError(requestError.message || 'Could not update the user.')
    } finally {
      setUpdatingUserId(null)
    }
  }

  return (
    <section className={`content-page users-page${showCreateForm ? ' create-user-page' : ''}`}>
      <div className="page-heading">
        <div className="page-title-group">
          <h1 className="page-title">{showCreateForm ? 'Create User' : 'Users'}</h1>
          {showCreateForm && (
            <nav className="page-breadcrumb" aria-label="Breadcrumb">
              <Link to="/admin/users" onClick={closeCreateForm}>Users</Link>
              <ChevronRight aria-hidden="true" />
              <span aria-current="page">Create User</span>
            </nav>
          )}
        </div>
        {!showCreateForm && (
          <button className="users-create-button" type="button" onClick={() => setShowCreateForm(true)}>
            Create user
          </button>
        )}
      </div>

      {success && <p className="message success" role="status">{success}</p>}
      {error && <p className="message error" role="alert">{error}</p>}

      {showCreateForm && (
        <form className="panel form-stack" onSubmit={handleCreate}>
          <h2 className="sr-only">Create a new user</h2>
          {formError && <p className="message error" role="alert">{formError}</p>}

          <div className="two-column-form">
            <div className="field-group">
              <label htmlFor="new-user-first-name">First name</label>
              <input
                id="new-user-first-name"
                name="firstName"
                value={newUser.firstName}
                onChange={updateField}
                maxLength="100"
                required
              />
            </div>
            <div className="field-group">
              <label htmlFor="new-user-last-name">Last name</label>
              <input
                id="new-user-last-name"
                name="lastName"
                value={newUser.lastName}
                onChange={updateField}
                maxLength="100"
                required
              />
            </div>
            <div className="field-group">
              <label htmlFor="new-user-username">Username</label>
              <input
                id="new-user-username"
                name="username"
                autoComplete="off"
                value={newUser.username}
                onChange={updateField}
                maxLength="100"
                required
              />
            </div>
            <div className="field-group">
              <label htmlFor="new-user-role">Role</label>
              <div className="select-control create-user-select">
                <select
                  id="new-user-role"
                  name="role"
                  value={newUser.role}
                  onChange={updateField}
                >
                  <option value="framer">Framer</option>
                  <option value="admin">Admin</option>
                </select>
                <ChevronDown aria-hidden="true" />
              </div>
            </div>
          </div>

          <div className="field-group">
            <label htmlFor="new-user-password">Password</label>
            <input
              id="new-user-password"
              name="password"
              type="password"
              autoComplete="new-password"
              value={newUser.password}
              onChange={updateField}
              minLength="8"
              maxLength="100"
              required
            />
          </div>

          <div className="form-actions">
            <button type="submit" disabled={isCreating}>
              {isCreating ? 'Creating...' : 'Create user'}
            </button>
            <button className="secondary" type="button" onClick={closeCreateForm}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {!showCreateForm && isLoading && <p>Loading users...</p>}
      {!showCreateForm && !isLoading && users.length === 0 && !error && <p>No users found.</p>}
      {!showCreateForm && !isLoading && users.length > 0 && (
        <div className="table-scroll data-table-card users-table-card">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Username</th>
                <th>Role</th>
                <th>Status</th>
                <th><span className="sr-only">Actions</span></th>
              </tr>
            </thead>
            <tbody>
              {users.map((user) => {
                const isCurrentUser = user.id === currentUser.id
                return (
                  <tr key={user.id}>
                    <td>{user.firstName} {user.lastName}</td>
                    <td>{user.username}{isCurrentUser ? ' (you)' : ''}</td>
                    <td>{displayRole(user.role)}</td>
                    <td>
                      <span className={`status-badge ${user.active ? 'active' : 'inactive'}`}>
                        {user.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>
                      <button
                        className="secondary compact-button user-status-button"
                        type="button"
                        onClick={() => handleActivation(user)}
                        disabled={updatingUserId === user.id || isCurrentUser}
                        title={isCurrentUser ? 'You cannot deactivate your own account' : undefined}
                      >
                        {updatingUserId === user.id
                          ? 'Updating...'
                          : user.active ? 'Deactivate' : 'Activate'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}

export default AdminUsersPage
