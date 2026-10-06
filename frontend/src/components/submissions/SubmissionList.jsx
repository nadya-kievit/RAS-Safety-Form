import { Link } from 'react-router-dom'
import { formatDateTime } from '../../utils/date.js'

function SubmissionList({ submissions, detailBasePath, showWorker = false }) {
  if (submissions.length === 0) {
    return <p>No submissions found.</p>
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
