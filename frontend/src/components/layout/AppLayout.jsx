import { useEffect, useRef, useState } from 'react'
import { LoaderCircle, Menu, X } from 'lucide-react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/auth.js'

const REFRESH_THRESHOLD = 64
const MAXIMUM_PULL = 92

function AppLayout() {
  const { user } = useAuth()
  const location = useLocation()
  const [menuPath, setMenuPath] = useState(null)
  const [pullDistance, setPullDistance] = useState(0)
  const [isPulling, setIsPulling] = useState(false)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const touchStartX = useRef(0)
  const pullStartY = useRef(0)
  const pullDistanceRef = useRef(0)
  const isPullingRef = useRef(false)
  const isRefreshingRef = useRef(false)
  const refreshTimerRef = useRef(null)
  const isFramer = user.role === 'framer'
  const isFramerHome = isFramer && location.pathname === '/framer'
  const homePath = isFramer ? '/framer' : '/admin'
  const profilePath = isFramer ? '/framer/profile' : '/admin/profile'
  const isMenuOpen = menuPath === location.pathname

  useEffect(() => {
    const phoneQuery = window.matchMedia('(max-width: 600px)')
    const scrollTop = () => Math.max(
      window.scrollY,
      document.documentElement.scrollTop,
      document.body.scrollTop,
    )

    function resetPull() {
      isPullingRef.current = false
      pullDistanceRef.current = 0
      setIsPulling(false)
      setPullDistance(0)
    }

    function handleTouchStart(event) {
      touchStartX.current = event.touches[0].clientX
      pullStartY.current = event.touches[0].clientY
      if (!phoneQuery.matches || isRefreshingRef.current || scrollTop() > 0) return
      isPullingRef.current = true
      setIsPulling(true)
    }

    function handleTouchMove(event) {
      if (phoneQuery.matches) {
        const horizontalDistance = event.touches[0].clientX - touchStartX.current
        const verticalDistance = event.touches[0].clientY - pullStartY.current

        if (Math.abs(horizontalDistance) > Math.abs(verticalDistance)) {
          if (event.cancelable) event.preventDefault()
          if (isPullingRef.current) resetPull()
          return
        }
      }

      if (!isPullingRef.current) return
      if (scrollTop() > 0) {
        resetPull()
        return
      }

      const distance = event.touches[0].clientY - pullStartY.current
      if (distance <= 0) {
        pullDistanceRef.current = 0
        setPullDistance(0)
        return
      }

      if (event.cancelable) event.preventDefault()
      const resistedDistance = Math.min(MAXIMUM_PULL, distance * .45)
      pullDistanceRef.current = resistedDistance
      setPullDistance(resistedDistance)
    }

    function handleTouchEnd() {
      if (!isPullingRef.current) return
      isPullingRef.current = false
      setIsPulling(false)

      if (pullDistanceRef.current >= REFRESH_THRESHOLD) {
        isRefreshingRef.current = true
        setIsRefreshing(true)
        setPullDistance(REFRESH_THRESHOLD)
        refreshTimerRef.current = window.setTimeout(() => {
          window.location.reload()
        }, 450)
        return
      }

      pullDistanceRef.current = 0
      setPullDistance(0)
    }

    document.addEventListener('touchstart', handleTouchStart, { passive: true })
    document.addEventListener('touchmove', handleTouchMove, { passive: false })
    document.addEventListener('touchend', handleTouchEnd, { passive: true })
    document.addEventListener('touchcancel', resetPull, { passive: true })

    return () => {
      document.removeEventListener('touchstart', handleTouchStart)
      document.removeEventListener('touchmove', handleTouchMove)
      document.removeEventListener('touchend', handleTouchEnd)
      document.removeEventListener('touchcancel', resetPull)
      if (refreshTimerRef.current) window.clearTimeout(refreshTimerRef.current)
    }
  }, [])

  return (
    <div className="app-shell branded-shell">
      <header className="framer-site-header">
        <button
          className="mobile-menu-button"
          type="button"
          aria-label={isMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          aria-controls="primary-navigation"
          aria-expanded={isMenuOpen}
          onClick={() => setMenuPath((current) => (
            current === location.pathname ? null : location.pathname
          ))}
        >
          {isMenuOpen ? <X aria-hidden="true" /> : <Menu aria-hidden="true" />}
        </button>
        <NavLink
          className="header-brand"
          to={homePath}
          aria-label="RAS home"
          onClick={() => setMenuPath(null)}
        >
          <img src="/ras-logo.png" alt="RAS logo" className="header-logo" />
        </NavLink>
        <nav
          id="primary-navigation"
          className={`primary-nav${isMenuOpen ? ' is-open' : ''}`}
          aria-label="Main navigation"
          onClick={() => setMenuPath(null)}
        >
          {isFramer ? (
            <>
              <NavLink end to="/framer">Dashboard</NavLink>
              <NavLink to="/framer/safety-form/new">New Form</NavLink>
              <NavLink to="/framer/submissions">My Submissions</NavLink>
              <NavLink to="/framer/profile">My Account</NavLink>
            </>
          ) : (
            <>
              <NavLink
                end
                to="/admin"
                className={({ isActive }) => (
                  isActive || location.pathname.startsWith('/admin/submissions/')
                    ? 'active'
                    : undefined
                )}
              >
                Submissions
              </NavLink>
              <NavLink to="/admin/users">Manage Users</NavLink>
              <NavLink to="/admin/profile">My Account</NavLink>
            </>
          )}
        </nav>
          <div className="profile-control">
            <NavLink
              className="profile-button"
              to={profilePath}
              aria-label={`Open ${isFramer ? 'Framer' : 'Admin'} profile`}
              onClick={() => setMenuPath(null)}
            >
              <span className="profile-avatar">
                <svg viewBox="0 0 64 64" aria-hidden="true">
                  <circle cx="32" cy="22" r="11" />
                  <path d="M14 53c0-11 8-18 18-18s18 7 18 18H14Z" />
                </svg>
              </span>
              <span className="profile-role">{isFramer ? 'Framer' : 'Admin'}</span>
            </NavLink>
          </div>
      </header>
      <div
        className={`pull-refresh-indicator${isPulling ? ' is-pulling' : ''}${isRefreshing ? ' is-refreshing' : ''}`}
        style={{
          '--pull-distance': `${pullDistance}px`,
          '--pull-rotation': `${Math.min(pullDistance / REFRESH_THRESHOLD, 1) * 270}deg`,
          opacity: pullDistance > 0 ? Math.min(pullDistance / 24, 1) : 0,
        }}
        role="status"
        aria-live="polite"
      >
        <LoaderCircle aria-hidden="true" />
        {isRefreshing && <span className="sr-only">Refreshing page</span>}
      </div>
      <main className={`page-container${isFramerHome ? ' framer-page-container' : ''}`}>
        <Outlet />
      </main>
    </div>
  )
}

export default AppLayout
