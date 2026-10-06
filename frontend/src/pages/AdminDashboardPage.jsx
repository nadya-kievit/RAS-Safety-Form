import { useEffect, useMemo, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import SubmissionList from '../components/submissions/SubmissionList.jsx'
import { getActiveSites } from '../services/siteService.js'
import { getAllSubmissions } from '../services/submissionService.js'

const emptyFilters = { siteId: '', userId: '', startDate: '', endDate: '' }

function localDateKey(value) {
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 10)
}

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
    const workerIds = new Set()
    const siteIds = new Set()
    submissions.forEach((submission) => {
      if (submission.userId) workerIds.add(submission.userId)
      if (submission.siteId) siteIds.add(submission.siteId)
    })
    return {
      submissions: submissions.length,
      workers: workerIds.size,
      sites: siteIds.size,
    }
  }, [submissions])

  const activity = useMemo(() => {
    const endDate = new Date()
    endDate.setHours(0, 0, 0, 0)
    const counts = new Map()
    submissions.forEach((submission) => {
      if (!submission.submittedAt) return
      const key = localDateKey(submission.submittedAt)
      counts.set(key, (counts.get(key) || 0) + 1)
    })

    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(endDate)
      date.setDate(endDate.getDate() - 6 + index)
      return {
        key: localDateKey(date),
        day: new Intl.DateTimeFormat(undefined, { weekday: 'short' }).format(date),
        date: new Intl.DateTimeFormat(undefined, {
          month: 'short',
          day: 'numeric',
        }).format(date),
        count: counts.get(localDateKey(date)) || 0,
      }
    })
  }, [submissions])

  const activityMaximum = Math.max(...activity.map((day) => day.count), 10)

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
      </div>

      <form className="panel admin-filter-grid" onSubmit={handleFilter}>
        <div className="field-group">
          <label htmlFor="filter-site">Site</label>
          <div className="admin-select-control">
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
            <ChevronDown aria-hidden="true" />
          </div>
        </div>

        <div className="field-group">
          <label htmlFor="filter-worker">Worker</label>
          <div className="admin-select-control">
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
            <ChevronDown aria-hidden="true" />
          </div>
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

      {error && <p className="message error" role="alert">{error}</p>}

      <section className="admin-totals" aria-label="Submission totals">
        <article className="panel admin-total-card">
          <strong>{summary.submissions}</strong>
          <span>Submissions</span>
        </article>
        <article className="panel admin-total-card">
          <strong>{summary.workers}</strong>
          <span>Workers</span>
        </article>
        <article className="panel admin-total-card">
          <strong>{summary.sites}</strong>
          <span>Sites</span>
        </article>
      </section>

      <section className="panel admin-activity" aria-labelledby="activity-heading">
        <h2 id="activity-heading">Submission activity</h2>
        <div className="activity-chart">
          <span className="activity-axis-title">Submissions</span>
          <div className="activity-plot">
            <div className="activity-y-ticks" aria-hidden="true">
              {[1, .8, .6, .4, .2, 0].map((ratio) => (
                <span key={ratio}>{Math.round(activityMaximum * ratio)}</span>
              ))}
            </div>
            <div className="activity-bars">
              {activity.map((day) => (
                <div className="activity-day" key={day.key}>
                  <div className="activity-bar-space">
                    <span
                      className="activity-bar"
                      style={{ height: `${(day.count / activityMaximum) * 100}%` }}
                      title={`${day.count} submissions`}
                    />
                  </div>
                  <span>{day.day}</span>
                  <span>{day.date}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      <section className="admin-records" aria-labelledby="records-heading">
        <h2 id="records-heading">Submission records</h2>
        {isLoading && <p className="empty-state">Loading...</p>}
        {!isLoading && !error && (
          <SubmissionList
            submissions={submissions}
            detailBasePath="/admin/submissions"
            showWorker
          />
        )}
      </section>
    </section>
  )
}

export default AdminDashboardPage
