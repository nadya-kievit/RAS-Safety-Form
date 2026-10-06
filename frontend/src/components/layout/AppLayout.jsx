import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/auth.js'
import { UserRound, ChevronDown } from 'lucide-react'

function AppLayout() {
  const { user } = useAuth()
  const location = useLocation()
  const isFramer = user.role === 'framer'
  const isFramerHome = isFramer && location.pathname === '/framer'
  const homePath = isFramer ? '/framer' : '/admin'
  const profilePath = isFramer ? '/framer/profile' : '/admin/profile'

  return (
    <div className="app-shell branded-shell">
      <header className="framer-site-header">
        <NavLink className="header-brand" to={homePath} aria-label="RAS home">
          <img src="/ras-logo.png" alt="RAS logo" className="header-logo" />
        </NavLink>
        <nav className="primary-nav" aria-label="Main navigation">
          {isFramer ? (
            <>
              <NavLink end to="/framer">Home</NavLink>
              <NavLink to="/framer/safety-form/new">New form</NavLink>
              <NavLink to="/framer/submissions">My submissions</NavLink>
            </>
          ) : (
            <>
              <NavLink end to="/admin">Submissions</NavLink>
              <NavLink to="/admin/users">Users</NavLink>
            </>
          )}
        </nav>
        {isFramer ? (
          <div className="profile-control">
            <NavLink
              className="profile-button"
              to={profilePath}
              aria-label="Open Framer profile"
            >
              <span className="profile-avatar">
                <UserRound size={25} strokeWidth={2.2} aria-hidden="true" />
              </span>
              <ChevronDown className="profile-chevron" size={19} aria-hidden="true" />
              <span className="profile-role">Framer</span>
            </NavLink>
          </div>
        ) : (
          <NavLink
            className="profile-icon-link"
            to={profilePath}
            aria-label="Open Admin profile"
          >
            <span className="profile-icon-circle">
              <svg viewBox="0 0 64 64" aria-hidden="true">
                <circle cx="32" cy="22" r="11" />
                <path d="M14 53c0-11 8-18 18-18s18 7 18 18H14Z" />
              </svg>
            </span>
            <span className="profile-role-label">Admin</span>
          </NavLink>
        )}
      </header>
      <main className={`page-container${isFramerHome ? ' framer-page-container' : ''}`}>
        <Outlet />
      </main>
    </div>
  )
}

export default AppLayout
