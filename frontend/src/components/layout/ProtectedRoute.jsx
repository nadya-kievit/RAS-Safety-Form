import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../context/auth.js'

function ProtectedRoute({ allowedRoles }) {
  const { user, isAuthLoading } = useAuth()

  if (isAuthLoading) {
    return <div className="auth-loading" role="status">Restoring your session...</div>
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (user.mustChangePassword) {
    return <Navigate to="/change-password" replace />
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to={user.role === 'admin' ? '/admin' : '/framer'} replace />
  }

  return <Outlet />
}

export default ProtectedRoute
