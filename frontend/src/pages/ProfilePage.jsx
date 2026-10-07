import { useState } from 'react'
import { ChevronDown, ChevronUp } from 'lucide-react'
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
  const [isPasswordOpen, setIsPasswordOpen] = useState(false)
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
      <h1 className="page-title">My Account</h1>

      <div className="account-card">
        <div className="account-section-header">
          <h2>Profile details</h2>
          {!isEditing && (
            <button className="secondary account-edit-button" type="button" onClick={startEditing}>
              Edit profile
            </button>
          )}
        </div>

        {profileSuccess && <p className="message success" role="status">{profileSuccess}</p>}

        {!isEditing ? (
          <dl className="account-details">
            <dt>Username</dt><dd>{user.username}</dd>
            <dt>First name</dt><dd>{user.firstName}</dd>
            <dt>Last name</dt><dd>{user.lastName}</dd>
            <dt>Role</dt><dd>{displayRole(user.role)}</dd>
          </dl>
        ) : (
          <form className="account-profile-form form-stack" onSubmit={handleProfileSubmit}>
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

        <button
          className="account-password-toggle"
          type="button"
          aria-expanded={isPasswordOpen}
          aria-controls="password-change-form"
          onClick={() => setIsPasswordOpen((current) => !current)}
        >
          <span>Password</span>
          {isPasswordOpen
            ? <ChevronUp aria-hidden="true" />
            : <ChevronDown aria-hidden="true" />}
        </button>

        {isPasswordOpen && (
          <form
            id="password-change-form"
            className="account-password-form form-stack"
            onSubmit={handlePasswordSubmit}
          >
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
                {isSavingPassword ? 'Updating...' : 'Update password'}
              </button>
            </div>
          </form>
        )}

        <div className="account-footer">
          <button className="account-signout-button" type="button" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </div>
    </section>
  )
}

export default ProfilePage
