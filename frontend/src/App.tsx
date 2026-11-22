import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardLayout from './components/DashboardLayout'
import DiaryPage from './pages/DiaryPage'
import TestsPage from './pages/TestsPage'
import HomePage from './pages/HomePage'
import PsychologistPage from './pages/PsychologistPage'
import AccountPage from './pages/AccountPage'
import PrivacyPage from './pages/PrivacyPage'
import AdminPage from './pages/AdminPage'
import AgendaPage from './pages/AgendaPage'

function Protected({ children }: { children: JSX.Element }) {
  const { token } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  return children
}

function IndexRoute() {
  const { user } = useAuth()
  if (user?.role === 'ADMIN') return <AdminPage />
  if (user?.role === 'PSYCHOLOGIST') return <PsychologistPage />
  return <HomePage />
}

export default function App() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/" element={<Protected><DashboardLayout /></Protected>}>
        <Route index element={<IndexRoute />} />
        <Route path="diario" element={<DiaryPage />} />
        <Route path="pacientes" element={<PsychologistPage />} />
        <Route path="testes" element={<TestsPage />} />
        <Route path="agenda" element={<AgendaPage />} />
        <Route path="conta" element={<AccountPage />} />
        <Route path="privacidade" element={<PrivacyPage />} />
        <Route path="admin" element={user?.role === 'ADMIN' ? <AdminPage /> : <Navigate to="/" replace />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
