import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/layout/AppLayout.jsx'
import ProtectedRoute from './components/layout/ProtectedRoute.jsx'
import { useAuth } from './context/auth.js'
import AdminDashboardPage from './pages/AdminDashboardPage.jsx'
import AdminUsersPage from './pages/AdminUsersPage.jsx'
import FramerHomePage from './pages/FramerHomePage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import NewSafetyFormPage from './pages/NewSafetyFormPage.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import SubmissionDetailPage from './pages/SubmissionDetailPage.jsx'
import SubmissionsPage from './pages/SubmissionsPage.jsx'

function HomeRedirect() {
  const { user } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return <Navigate to={user.role === 'admin' ? '/admin' : '/framer'} replace />
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<HomeRedirect />} />

        <Route element={<ProtectedRoute allowedRoles={['framer']} />}>
          <Route element={<AppLayout />}>
            <Route path="/framer" element={<FramerHomePage />} />
            <Route path="/framer/safety-form/new" element={<NewSafetyFormPage />} />
            <Route path="/framer/submissions" element={<SubmissionsPage />} />
            <Route path="/framer/profile" element={<ProfilePage />} />
            <Route
              path="/framer/submissions/:submissionId"
              element={<SubmissionDetailPage />}
            />
          </Route>
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['admin']} />}>
          <Route element={<AppLayout />}>
            <Route path="/admin" element={<AdminDashboardPage />} />
            <Route path="/admin/users" element={<AdminUsersPage />} />
            <Route path="/admin/profile" element={<ProfilePage />} />
            <Route
              path="/admin/submissions/:submissionId"
              element={<SubmissionDetailPage />}
            />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
