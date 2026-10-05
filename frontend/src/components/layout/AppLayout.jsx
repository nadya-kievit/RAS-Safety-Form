import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/auth.js'

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
        <img src="/ras-logo.png" alt="RAS logo" className="header-logo" />
        <NavLink
          className="profile-icon-link"
          to={profilePath}
          aria-label="Open profile"
        >
          <svg viewBox="0 0 64 64" aria-hidden="true">
            <circle cx="32" cy="22" r="11" />
            <path d="M14 53c0-11 8-18 18-18s18 7 18 18H14Z" />
          </svg>
        </NavLink>
      </header>
      <main className={`page-container${isFramerHome ? ' framer-page-container' : ''}`}>
        <Outlet />
      </main>
    </div>
  )
}

export default AppLayout
