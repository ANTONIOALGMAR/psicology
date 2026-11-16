import { Link, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const nav = [
  { to: '/', label: 'Início' },
  { to: '/diario', label: 'Meu Diário' },
  { to: '/testes', label: 'Testes' },
]

export default function DashboardLayout() {
  const { user, logout } = useAuth()
  return (
    <div className="min-h-screen bg-gradient-to-br from-brand-50 via-slate-50 to-teal-50">
      <header className="sticky top-0 z-10 border-b border-white/60 bg-white/70 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Link to="/" className="flex items-center gap-2">
            <div className="h-9 w-9 rounded-xl bg-brand-500 text-white grid place-items-center font-bold shadow-soft">Ψ</div>
            <span className="font-semibold text-brand-900">PsiCare</span>
          </Link>
          <nav className="flex items-center gap-2">
            {nav.map(i => (
              <NavLink key={i.to} to={i.to} end={i.to==='/'}
                className={({isActive}) => `px-3 py-2 rounded-lg text-sm font-medium transition ${isActive ? 'bg-brand-100 text-brand-800' : 'text-slate-600 hover:bg-white'}`}>
                {i.label}
              </NavLink>
            ))}
          </nav>
          <div className="flex items-center gap-3">
            <div className="text-right hidden sm:block">
              <div className="text-sm font-medium">{user?.name}</div>
              <div className="text-xs text-slate-500">{user?.role === 'PATIENT' ? 'Paciente' : user?.role === 'PSYCHOLOGIST' ? 'Psicólogo(a)' : 'Admin'}</div>
            </div>
            <button onClick={logout} className="rounded-lg border border-slate-200 px-3 py-2 text-sm hover:bg-slate-100">Sair</button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-8"><Outlet /></main>
    </div>
  )
}
