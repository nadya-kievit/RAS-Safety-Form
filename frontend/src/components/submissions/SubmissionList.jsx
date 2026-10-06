import { Link, useNavigate } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import { formatDateTime } from '../../utils/date.js'

function SubmissionList({ submissions, detailBasePath, showWorker = false }) {
  const navigate = useNavigate()

  if (submissions.length === 0) {
    return <p>No submissions found.</p>
  }

  if (!showWorker) {
    return (
      <div className="submission-list-card">
        {submissions.map((submission) => (
          <div className="submission-list-row" key={submission.id}>
            <div>
              <h2>{submission.site?.name || `Site ${submission.siteId}`}</h2>
              <p>{formatDateTime(submission.submittedAt)}</p>
            </div>
            <Link
              className="recent-submission-link"
              to={`${detailBasePath}/${submission.id}`}
            >
              View
            </Link>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="table-scroll admin-submissions-table">
      <table>
        <thead>
          <tr>
            {showWorker && <th>Worker</th>}
            <th>Site</th>
            <th>Submitted</th>
            <th><span className="sr-only">Actions</span></th>
          </tr>
        </thead>
        <tbody>
          {submissions.map((submission) => {
            const detailPath = `${detailBasePath}/${submission.id}`
            const accessibleLabel = `View submission from ${submission.user?.firstName || 'worker'} at ${submission.site?.name || `Site ${submission.siteId}`}`

            return (
              <tr
                key={submission.id}
                role="link"
                tabIndex={0}
                aria-label={accessibleLabel}
                onClick={() => navigate(detailPath)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault()
                    navigate(detailPath)
                  }
                }}
              >
                {showWorker && (
                  <td>{submission.user?.firstName} {submission.user?.lastName}</td>
                )}
                <td>{submission.site?.name || `Site ${submission.siteId}`}</td>
                <td>{formatDateTime(submission.submittedAt)}</td>
                <td className="submission-table-action" aria-hidden="true">
                  <ChevronRight />
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default SubmissionList
