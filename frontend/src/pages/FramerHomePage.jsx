import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/auth.js'

function FramerHomePage() {
  const { user } = useAuth()
  const location = useLocation()

  return (
    <section className="framer-home">
      {location.state?.message && (
        <p className="message success" role="status">{location.state.message}</p>
      )}
      <div className="framer-welcome">
        <p>Welcome back</p>
        <h1>{user.firstName} {user.lastName}</h1>
      </div>
      <div className="framer-home-actions">
        <Link className="framer-action primary" to="/framer/safety-form/new">
          Complete Safety Form
        </Link>
        <Link className="framer-action secondary" to="/framer/submissions">
          My Submissions
        </Link>
      </div>
    </section>
  )
}

export default FramerHomePage
