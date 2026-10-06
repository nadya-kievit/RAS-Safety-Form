import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import SubmissionList from '../components/submissions/SubmissionList.jsx'
import { getActiveSites } from '../services/siteService.js'
import { getAllSubmissions } from '../services/submissionService.js'
import { todayInputValue } from '../utils/date.js'

const emptyFilters = { siteId: '', userId: '', startDate: '', endDate: '' }

function AdminDashboardPage() {
  const [submissions, setSubmissions] = useState([])
  const [sites, setSites] = useState([])
  const [workers, setWorkers] = useState([])
  const [filters, setFilters] = useState(emptyFilters)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    Promise.all([getAllSubmissions(), getActiveSites()])
      .then(([submissionResult, siteResult]) => {
        if (ignore) return
        setSubmissions(submissionResult)
        setSites(siteResult)
        const uniqueWorkers = new Map()
        submissionResult.forEach((submission) => {
          if (submission.user) uniqueWorkers.set(submission.user.id, submission.user)
        })
        setWorkers(Array.from(uniqueWorkers.values()).sort((a, b) =>
          `${a.lastName} ${a.firstName}`.localeCompare(`${b.lastName} ${b.firstName}`),
        ))
      })
      .catch(() => {
        if (!ignore) setError('Could not load submissions.')
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => { ignore = true }
  }, [])

  const summary = useMemo(() => {
    const today = todayInputValue()
    const perSite = new Map()
    submissions.forEach((submission) => {
      const siteName = submission.site?.name || `Site ${submission.siteId}`
      perSite.set(siteName, (perSite.get(siteName) || 0) + 1)
    })
    return {
      today: submissions.filter((submission) =>
        submission.submittedAt?.slice(0, 10) === today,
      ).length,
      perSite: Array.from(perSite.entries()),
    }
  }, [submissions])

  function updateFilter(name, value) {
    setFilters((current) => ({ ...current, [name]: value }))
  }

  async function loadFilteredSubmissions(nextFilters) {
    setIsLoading(true)
    setError('')
    try {
      setSubmissions(await getAllSubmissions(nextFilters))
    } catch {
      setError('Could not load filtered submissions.')
    } finally {
      setIsLoading(false)
    }
  }

  function handleFilter(event) {
    event.preventDefault()
    loadFilteredSubmissions(filters)
  }

  function handleReset() {
    setFilters(emptyFilters)
    loadFilteredSubmissions(emptyFilters)
  }

  return (
    <section className="content-page admin-dashboard-page">
      <div className="page-heading">
        <h1 className="page-title">Submissions</h1>
        <Link className="button" to="/admin/users">Manage users</Link>
      </div>

      <form className="panel filter-grid" onSubmit={handleFilter}>
        <div className="field-group">
          <label htmlFor="filter-site">Site</label>
          <select
            id="filter-site"
            value={filters.siteId}
            onChange={(event) => updateFilter('siteId', event.target.value)}
          >
            <option value="">All sites</option>
            {sites.map((site) => (
              <option key={site.id} value={site.id}>{site.name}</option>
            ))}
          </select>
        </div>

        <div className="field-group">
          <label htmlFor="filter-worker">Worker</label>
          <select
            id="filter-worker"
            value={filters.userId}
            onChange={(event) => updateFilter('userId', event.target.value)}
          >
            <option value="">All workers</option>
            {workers.map((worker) => (
              <option key={worker.id} value={worker.id}>
                {worker.firstName} {worker.lastName}
              </option>
            ))}
          </select>
        </div>

        <div className="field-group">
          <label htmlFor="start-date">Start date</label>
          <input
            id="start-date"
            type="date"
            value={filters.startDate}
            onChange={(event) => updateFilter('startDate', event.target.value)}
          />
        </div>

        <div className="field-group">
          <label htmlFor="end-date">End date</label>
          <input
            id="end-date"
            type="date"
            value={filters.endDate}
            onChange={(event) => updateFilter('endDate', event.target.value)}
          />
        </div>

        <div className="form-actions">
          <button type="submit" disabled={isLoading}>Apply filters</button>
          <button type="button" className="secondary" onClick={handleReset} disabled={isLoading}>
            Reset
          </button>
        </div>
      </form>

      <section className="summary panel" aria-labelledby="summary-heading">
        <h2 id="summary-heading">Current results summary</h2>
        <p><strong>Submissions today:</strong> {summary.today}</p>
        <h3>Submissions per site</h3>
        {summary.perSite.length === 0 ? (
          <p>No submissions in the current results.</p>
        ) : (
          <ul className="compact-list">
            {summary.perSite.map(([siteName, count]) => (
              <li key={siteName}>{siteName}: {count}</li>
            ))}
          </ul>
        )}
      </section>

      {isLoading && <p>Loading...</p>}
      {error && <p className="message error" role="alert">{error}</p>}
      {!isLoading && !error && (
        <SubmissionList
          submissions={submissions}
          detailBasePath="/admin/submissions"
          showWorker
        />
      )}
    </section>
  )
}

export default AdminDashboardPage
