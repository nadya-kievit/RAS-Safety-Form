import { Link, useLocation } from 'react-router-dom'
import { FilePlus2, Files } from 'lucide-react'
import { useAuth } from '../context/auth.js'
import { useEffect, useState } from 'react'
import { getUserSubmissions } from '../services/submissionService.js'
import { formatDateTime } from '../utils/date.js'

function FramerHomePage() {
  const { user } = useAuth()
  const location = useLocation()
  const [submissions, setSubmissions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    getUserSubmissions(user.id)
      .then((result) => {
        if (!ignore) setSubmissions(result.slice(0, 5))
      })
      .catch(() => {
        if (!ignore) setError('Could not load your submissions.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => { ignore = true }
  }, [user.id])

  return (
    <section className="framer-home">
      {location.state?.message && (
        <p className="message success" role="status">{location.state.message}</p>
      )}
      <div className="framer-welcome">
        <p>Welcome back,</p>
        <h1 className="page-title">{user.firstName} {user.lastName}</h1>
      </div>
      <div className="framer-home-actions">
        <Link className="framer-action" to="/framer/safety-form/new">
          <span className="framer-action-icon primary" aria-hidden="true">
            <FilePlus2 />
          </span>
          <span className="framer-action-copy">
            <strong>Safety Form</strong>
            <span>Start a new construction safety form</span>
          </span>
        </Link>
        <Link className="framer-action" to="/framer/submissions">
          <span className="framer-action-icon" aria-hidden="true">
            <Files />
          </span>
          <span className="framer-action-copy">
            <strong>My Submissions</strong>
            <span>View and manage your submitted forms</span>
          </span>
        </Link>
      </div>

      <section className="recent-submissions">
        <h2>Recent submissions</h2>

        <div className="recent-submissions-list">
          {error ? (
            <p className="message error" role="alert">{error}</p>
          ) : isLoading ? (
            <p className="empty-state">Loading submissions...</p>
          ) : submissions.length === 0 ? (
            <p className="empty-state">No submissions yet.</p>
          ) : (
            submissions.map((submission) => (
              <div
                className="recent-submission-row"
                key={submission.id}
              >
                <div>
                  <h3>{submission.site?.name || `Site ${submission.siteId}`}</h3>
                  <p>{formatDateTime(submission.submittedAt)}</p>
                </div>

                <Link
                  to={`/framer/submissions/${submission.id}`}
                  className="recent-submission-link"
                >
                  View
                </Link>
              </div>
            ))
          )}
        </div>
      </section>
    </section>
  )
}

export default FramerHomePage
