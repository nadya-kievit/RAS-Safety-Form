import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/auth.js'

function FramerHomePage() {
  const { user } = useAuth()
  const location = useLocation()

  return (
    <section>
      <h1>Welcome, {user.firstName}</h1>
      {location.state?.message && (
        <p className="message success" role="status">{location.state.message}</p>
      )}
      <div className="action-list">
        <Link className="button" to="/framer/safety-form/new">
          Complete Safety Form
        </Link>
        <Link className="button secondary" to="/framer/submissions">
          My Submissions
        </Link>
      </div>
    </section>
  )
}

export default FramerHomePage
