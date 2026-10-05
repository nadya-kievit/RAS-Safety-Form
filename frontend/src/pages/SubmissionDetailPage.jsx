import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/auth.js'
import { getSubmissionDetail } from '../services/submissionService.js'
import { formatDate, formatDateTime } from '../utils/date.js'

function SubmissionDetailPage() {
  const { submissionId } = useParams()
  const { user } = useAuth()
  const [submission, setSubmission] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    getSubmissionDetail(submissionId)
      .then((result) => {
        if (ignore) return
        if (user.role === 'framer' && result.userId !== user.id) {
          setError('You are not authorized to view this submission.')
          return
        }
        setSubmission(result)
      })
      .catch(() => {
        if (!ignore) setError('Could not load the submission.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => { ignore = true }
  }, [submissionId, user.id, user.role])

  const backPath = user.role === 'admin' ? '/admin' : '/framer/submissions'

  if (isLoading) return <p>Loading...</p>

  if (error || !submission) {
    return (
      <section>
        <p className="message error" role="alert">{error || 'Submission not found.'}</p>
        <Link to={backPath}>Back to submissions</Link>
      </section>
    )
  }

  return (
    <article>
      <h1>Safety Form Submission</h1>
      <Link to={backPath}>Back to submissions</Link>

      <dl className="details panel">
        {user.role === 'admin' && (
          <>
            <dt>Worker</dt>
            <dd>{submission.user?.firstName} {submission.user?.lastName}</dd>
          </>
        )}
        <dt>Site</dt>
        <dd>{submission.site?.name || `Site ${submission.siteId}`}</dd>
        <dt>Form date</dt>
        <dd>{formatDate(submission.formDate)}</dd>
        <dt>Submitted</dt>
        <dd>{formatDateTime(submission.submittedAt)}</dd>
        <dt>Notes</dt>
        <dd>{submission.notes || 'None'}</dd>
      </dl>

      <section className="panel">
        <h2>Safety checklist</h2>
        <ul>
          {submission.checklist.items.map((item) => <li key={item.id}>{item.item}</li>)}
        </ul>
      </section>

      <section className="panel">
        <h2>Photos</h2>
        {submission.photos.length === 0 ? (
          <p>No photos are attached.</p>
        ) : (
          <div className="photo-list">
            {submission.photos.map((photo) => (
              <figure key={photo.id}>
                <img src={photo.viewUrl} alt={photo.filename} />
                <figcaption>{photo.filename}</figcaption>
              </figure>
            ))}
          </div>
        )}
      </section>
    </article>
  )
}

export default SubmissionDetailPage
