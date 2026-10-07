import { lazy, Suspense, useEffect, useMemo, useState } from 'react'
import { ChevronDown, ChevronRight, SlidersHorizontal } from 'lucide-react'
import SubmissionList from '../components/submissions/SubmissionList.jsx'
import { getActiveSites } from '../services/siteService.js'
import { getAllSubmissions } from '../services/submissionService.js'
import { getUsers } from '../services/userService.js'

const emptyFilters = { siteId: '', userId: '', startDate: '', endDate: '' }
const SubmissionActivityChart = lazy(() => (
  import('../components/submissions/SubmissionActivityChart.jsx')
))

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
  const [areFiltersOpen, setAreFiltersOpen] = useState(false)
  const [showAllRecords, setShowAllRecords] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let ignore = false
    Promise.all([getAllSubmissions(), getActiveSites(), getUsers()])
      .then(([submissionResult, siteResult, userResult]) => {
        if (ignore) return
        setSubmissions(submissionResult)
        setSites(siteResult)
        setWorkers(userResult
          .filter((worker) => worker.active && worker.role === 'framer')
          .sort((a, b) =>
            `${a.lastName} ${a.firstName}`.localeCompare(`${b.lastName} ${b.firstName}`),
          ))
      })
      .catch((requestError) => {
        if (!ignore) {
          setError(requestError.message || 'Could not load submissions.')
        }
      })
      .finally(() => {
        if (!ignore) setIsLoading(false)
      })
    return () => { ignore = true }
  }, [])

  const summary = useMemo(() => {
    const activeWorkerIds = new Set(workers.map((worker) => worker.id))
    const activeSiteIds = new Set(sites.map((site) => site.id))
    const workerIds = new Set()
    const siteIds = new Set()
    submissions.forEach((submission) => {
      if (activeWorkerIds.has(submission.userId)) workerIds.add(submission.userId)
      if (activeSiteIds.has(submission.siteId)) siteIds.add(submission.siteId)
    })
    return {
      submissions: submissions.length,
      workers: workerIds.size,
      sites: siteIds.size,
    }
  }, [sites, submissions, workers])

  const activity = useMemo(() => {
    const endDate = new Date()
    endDate.setHours(0, 0, 0, 0)
    const dailyActivity = new Map()
    submissions.forEach((submission) => {
      if (!submission.formDate) return
      const key = localDateKey(submission.formDate)
      const day = dailyActivity.get(key) || { count: 0, workers: new Map() }
      const workerId = submission.userId || `unknown-${submission.id}`
      const workerName = [submission.user?.firstName, submission.user?.lastName]
        .filter(Boolean)
        .join(' ') || 'Unknown worker'
      const worker = day.workers.get(workerId) || {
        id: workerId,
        name: workerName,
        count: 0,
      }
      worker.count += 1
      day.count += 1
      day.workers.set(workerId, worker)
      dailyActivity.set(key, day)
    })

    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date(endDate)
      date.setDate(endDate.getDate() - 6 + index)
      const key = localDateKey(date)
      const dayActivity = dailyActivity.get(key)
      return {
        key,
        day: new Intl.DateTimeFormat(undefined, { weekday: 'short' }).format(date),
        date: new Intl.DateTimeFormat(undefined, {
          month: 'short',
          day: 'numeric',
        }).format(date),
        count: dayActivity?.count || 0,
        workers: Array.from(dayActivity?.workers.values() || []),
      }
    })
  }, [submissions])

  function updateFilter(name, value) {
    setFilters((current) => ({ ...current, [name]: value }))
  }

  async function loadFilteredSubmissions(nextFilters) {
    setIsLoading(true)
    setError('')
    try {
      setSubmissions(await getAllSubmissions(nextFilters))
    } catch (requestError) {
      setError(requestError.message || 'Could not load filtered submissions.')
    } finally {
      setIsLoading(false)
    }
  }

  function handleFilter(event) {
    event.preventDefault()
    setAreFiltersOpen(false)
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

      <section className={`admin-filter-panel${areFiltersOpen ? ' is-open' : ''}`}>
        <button
          className="admin-filter-toggle"
          type="button"
          aria-expanded={areFiltersOpen}
          aria-controls="admin-filters"
          onClick={() => setAreFiltersOpen((current) => !current)}
        >
          <SlidersHorizontal aria-hidden="true" />
          <span>Filters</span>
          <ChevronDown className="admin-filter-toggle-chevron" aria-hidden="true" />
        </button>

        <form id="admin-filters" className="panel admin-filter-grid" onSubmit={handleFilter}>
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
      </section>

      {error && <p className="message error" role="alert">{error}</p>}

      <section className="admin-totals" aria-label="Submission totals">
        <article className="panel admin-total-card">
          <strong>{summary.submissions}</strong>
          <span>Submissions</span>
        </article>
        <article className="panel admin-total-card">
          <strong>
            {summary.workers}
            <small>/{workers.length}</small>
          </strong>
          <span>Workers</span>
        </article>
        <article className="panel admin-total-card">
          <strong>
            {summary.sites}
            <small>/{sites.length}</small>
          </strong>
          <span>Sites</span>
        </article>
      </section>

      <section className="panel admin-activity" aria-labelledby="activity-heading">
        <h2 id="activity-heading">Submission activity</h2>
        <Suspense fallback={<div className="activity-chart activity-chart-loading">Loading chart...</div>}>
          <SubmissionActivityChart activity={activity} />
        </Suspense>
      </section>

      <section
        className={`admin-records${showAllRecords ? ' show-all-records' : ''}`}
        aria-labelledby="records-heading"
      >
        <div className="admin-records-heading">
          <h2 id="records-heading">Submission records</h2>
          {submissions.length > 3 && (
            <button
              className="admin-records-view-all"
              type="button"
              onClick={() => setShowAllRecords((current) => !current)}
            >
              {showAllRecords ? 'Show less' : 'View all'}
              <ChevronRight aria-hidden="true" />
            </button>
          )}
        </div>
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
