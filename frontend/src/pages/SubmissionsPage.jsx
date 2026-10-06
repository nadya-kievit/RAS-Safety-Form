import { useEffect, useState } from 'react'
import SubmissionList from '../components/submissions/SubmissionList.jsx'
import { useAuth } from '../context/auth.js'
import { getUserSubmissions } from '../services/submissionService.js'

function SubmissionsPage() {
  const { user } = useAuth()
  const [submissions, setSubmissions] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    getUserSubmissions(user.id)
      .then((result) => {
        if (!ignore) setSubmissions(result)
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
    <section className="content-page submissions-page">
      <h1 className="page-title">My Submissions</h1>
      {isLoading && <p>Loading...</p>}
      {error && <p className="message error" role="alert">{error}</p>}
      {!isLoading && !error && (
        <SubmissionList
          submissions={submissions}
          detailBasePath="/framer/submissions"
        />
      )}
    </section>
  )
}

export default SubmissionsPage
