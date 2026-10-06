import { Link } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import { formatDateTime } from '../../utils/date.js'

function SubmissionList({ submissions, detailBasePath, showWorker = false }) {
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
              className="submission-list-link"
              to={`${detailBasePath}/${submission.id}`}
            >
              View
              <ChevronRight aria-hidden="true" />
            </Link>
          </div>
        ))}
      </div>
    )
  }

  return (
    <div className="table-scroll data-table-card">
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
          {submissions.map((submission) => (
            <tr key={submission.id}>
              {showWorker && (
                <td>{submission.user?.firstName} {submission.user?.lastName}</td>
              )}
              <td>{submission.site?.name || `Site ${submission.siteId}`}</td>
              <td>{formatDateTime(submission.submittedAt)}</td>
              <td>
                <Link to={`${detailBasePath}/${submission.id}`}>View</Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default SubmissionList
