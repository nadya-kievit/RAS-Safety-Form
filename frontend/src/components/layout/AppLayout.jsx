import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/auth.js'

function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    try {
      await logout()
    } finally {
      navigate('/login', { replace: true })
    }
  }

  return (
    <div className="app-shell">
      <header className="site-header">
        <NavLink className="site-title" to={user.role === 'admin' ? '/admin' : '/framer'}>
          RAS Safety Forms
        </NavLink>
        <nav aria-label="Main navigation">
          {user.role === 'framer' && (
            <>
              <NavLink to="/framer">Home</NavLink>
              <NavLink to="/framer/safety-form/new">New form</NavLink>
              <NavLink to="/framer/submissions">My submissions</NavLink>
            </>
          )}
          {user.role === 'admin' && <NavLink to="/admin">Submissions</NavLink>}
          <button className="button-link" type="button" onClick={handleLogout}>
            Logout
          </button>
        </nav>
      </header>
      <main className="page-container">
        <Outlet />
      </main>
    </div>
  )
}

export default AppLayout
