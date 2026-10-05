import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../../context/auth.js'

function ProtectedRoute({ allowedRoles }) {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to={user.role === 'admin' ? '/admin' : '/framer'} replace />
  }

  return <Outlet />
}

export default ProtectedRoute
