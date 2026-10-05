import { Link } from 'react-router-dom'
import { useAuth } from '../context/auth.js'

function NotFoundPage() {
  const { user } = useAuth()
  const home = user ? (user.role === 'admin' ? '/admin' : '/framer') : '/login'

  return (
    <main className="page-container">
      <h1>Page not found</h1>
      <Link to={home}>Go back</Link>
    </main>
  )
}

export default NotFoundPage
