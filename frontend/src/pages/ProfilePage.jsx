import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/auth.js'
import { changePassword } from '../services/authService.js'

const emptyPasswords = {
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
}

function displayRole(role) {
  return role === 'admin' ? 'Admin' : 'Framer'
}

function ProfilePage() {
  const { user, updateProfile, logout } = useAuth()
  const navigate = useNavigate()
  const [isEditing, setIsEditing] = useState(false)
  const [profile, setProfile] = useState({
    username: user.username,
    firstName: user.firstName,
    lastName: user.lastName,
  })
  const [passwords, setPasswords] = useState(emptyPasswords)
  const [profileError, setProfileError] = useState('')
  const [profileSuccess, setProfileSuccess] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const [passwordSuccess, setPasswordSuccess] = useState('')
  const [isSavingProfile, setIsSavingProfile] = useState(false)
  const [isSavingPassword, setIsSavingPassword] = useState(false)

  function updateProfileField(event) {
    const { name, value } = event.target
    setProfile((current) => ({ ...current, [name]: value }))
  }

  function updatePasswordField(event) {
    const { name, value } = event.target
    setPasswords((current) => ({ ...current, [name]: value }))
  }

  function startEditing() {
    setProfile({
      username: user.username,
      firstName: user.firstName,
      lastName: user.lastName,
    })
    setProfileError('')
    setProfileSuccess('')
    setIsEditing(true)
  }

  function cancelEditing() {
    setProfileError('')
    setIsEditing(false)
  }

  async function handleProfileSubmit(event) {
    event.preventDefault()
    setProfileError('')
    setProfileSuccess('')
    setIsSavingProfile(true)

    try {
      await updateProfile(profile)
      setProfileSuccess('Profile updated successfully.')
      setIsEditing(false)
    } catch (requestError) {
      setProfileError(requestError.message || 'Could not update your profile.')
    } finally {
      setIsSavingProfile(false)
    }
  }

  async function handlePasswordSubmit(event) {
    event.preventDefault()
    setPasswordError('')
    setPasswordSuccess('')

    if (passwords.newPassword !== passwords.confirmNewPassword) {
      setPasswordError('New passwords do not match.')
      return
    }

    setIsSavingPassword(true)
    try {
      await changePassword(passwords)
      setPasswords(emptyPasswords)
      setPasswordSuccess('Password changed successfully.')
    } catch (requestError) {
      setPasswordError(requestError.message || 'Could not change your password.')
    } finally {
      setIsSavingPassword(false)
    }
  }

  async function handleLogout() {
    try {
      await logout()
    } finally {
      navigate('/login', { replace: true })
    }
  }

  return (
    <section className="content-page profile-page">
      <div className="page-heading">
        <h1>My Account</h1>
        {!isEditing && (
          <button type="button" onClick={startEditing}>Edit profile</button>
        )}
      </div>

      {profileSuccess && <p className="message success" role="status">{profileSuccess}</p>}

      {!isEditing ? (
        <div className="panel">
          <dl className="details">
            <dt>Username</dt><dd>{user.username}</dd>
            <dt>First name</dt><dd>{user.firstName}</dd>
            <dt>Last name</dt><dd>{user.lastName}</dd>
            <dt>Role</dt><dd>{displayRole(user.role)}</dd>
          </dl>
        </div>
      ) : (
        <form className="panel form-stack" onSubmit={handleProfileSubmit}>
          <h2>Edit profile information</h2>
          {profileError && <p className="message error" role="alert">{profileError}</p>}

          <div className="profile-fields-grid">
            <div className="field-group">
              <label htmlFor="profile-first-name">First name</label>
              <input
                id="profile-first-name"
                name="firstName"
                value={profile.firstName}
                onChange={updateProfileField}
                maxLength="100"
                required
              />
            </div>
            <div className="field-group">
              <label htmlFor="profile-last-name">Last name</label>
              <input
                id="profile-last-name"
                name="lastName"
                value={profile.lastName}
                onChange={updateProfileField}
                maxLength="100"
                required
              />
            </div>
            <div className="field-group">
              <label htmlFor="profile-username">Username</label>
              <input
                id="profile-username"
                name="username"
                value={profile.username}
                onChange={updateProfileField}
                maxLength="100"
                required
              />
            </div>
            <div className="field-group">
              <label htmlFor="profile-role">Role</label>
              <input id="profile-role" value={displayRole(user.role)} disabled />
            </div>
          </div>

          <div className="form-actions">
            <button type="submit" disabled={isSavingProfile}>
              {isSavingProfile ? 'Saving...' : 'Save changes'}
            </button>
            <button className="secondary" type="button" onClick={cancelEditing}>
              Cancel
            </button>
          </div>
        </form>
      )}

      <form className="panel form-stack" onSubmit={handlePasswordSubmit}>
        <h2>Change password</h2>
        {passwordError && <p className="message error" role="alert">{passwordError}</p>}
        {passwordSuccess && <p className="message success" role="status">{passwordSuccess}</p>}

        <div className="field-group">
          <label htmlFor="current-password">Current password</label>
          <input
            id="current-password"
            name="currentPassword"
            type="password"
            autoComplete="current-password"
            value={passwords.currentPassword}
            onChange={updatePasswordField}
            required
          />
        </div>
        <div className="field-group">
          <label htmlFor="new-password">New password</label>
          <input
            id="new-password"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            value={passwords.newPassword}
            onChange={updatePasswordField}
            minLength="8"
            required
          />
          <small>Use at least 8 characters.</small>
        </div>
        <div className="field-group">
          <label htmlFor="confirm-new-password">Confirm new password</label>
          <input
            id="confirm-new-password"
            name="confirmNewPassword"
            type="password"
            autoComplete="new-password"
            value={passwords.confirmNewPassword}
            onChange={updatePasswordField}
            minLength="8"
            required
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={isSavingPassword}>
            {isSavingPassword ? 'Changing...' : 'Change password'}
          </button>
        </div>
      </form>

      <section className="panel profile-logout" aria-labelledby="logout-heading">
        <div>
          <h2 id="logout-heading">Sign out</h2>
          <p>Sign out of your RAS Safety Forms account.</p>
        </div>
        <button type="button" onClick={handleLogout}>Logout</button>
      </section>
    </section>
  )
}

export default ProfilePage
