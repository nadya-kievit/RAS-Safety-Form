import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../context/auth.js'
import { getSubmissionDetail } from '../services/submissionService.js'
import { formatDateTime } from '../utils/date.js'

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
      <section className="content-page">
        <p className="message error" role="alert">{error || 'Submission not found.'}</p>
        <Link to={backPath}>Back to submissions</Link>
      </section>
    )
  }

  return (
    <article className="content-page submission-detail-page">
      <h1>Safety Form</h1>

      <dl className="details panel">
        <dt>Site</dt>
        <dd>{submission.site?.name || `Site ${submission.siteId}`}</dd>
        <dt>Submitted</dt>
        <dd>{formatDateTime(submission.submittedAt)}</dd>
        <dt>Submitted by</dt>
        <dd>{submission.user?.firstName} {submission.user?.lastName}</dd>
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
                <figcaption>
                  <span>{photo.filename}</span>
                  <a href={photo.viewUrl} target="_blank" rel="noreferrer">View</a>
                </figcaption>
              </figure>
            ))}
          </div>
        )}
      </section>

      <section className="panel notes-panel">
        <h2>Notes</h2>
        <p>{submission.notes || 'None'}</p>
      </section>
    </article>
  )
}

export default SubmissionDetailPage
