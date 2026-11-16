import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { login } = useAuth()
  const nav = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setLoading(true)
    try { await login(email, password); nav('/') }
    catch (err: any) { setError(err?.response?.data?.message || 'Não foi possível entrar. Verifique seus dados.') }
    finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen grid lg:grid-cols-2 bg-gradient-to-br from-brand-700 via-brand-600 to-teal-500">
      <div className="hidden lg:flex flex-col justify-center p-16 text-white">
        <div className="h-14 w-14 rounded-2xl bg-white/20 grid place-items-center text-2xl font-bold mb-6">Ψ</div>
        <h1 className="text-4xl font-bold leading-tight mb-4">Cuidar da mente é um ato de coragem.</h1>
        <p className="text-brand-50 text-lg max-w-md">Acompanhe seu diário, registre como você se sente e participe de testes com suporte do seu psicólogo em um ambiente seguro e acolhedor.</p>
      </div>
      <div className="flex items-center justify-center p-6">
        <form onSubmit={submit} className="glass w-full max-w-md rounded-3xl p-8">
          <h2 className="text-2xl font-bold text-slate-900 mb-1">Bem-vindo(a) de volta</h2>
          <p className="text-slate-500 mb-6">Entre para continuar seu acompanhamento.</p>
          {error && <div className="mb-4 rounded-xl bg-red-50 text-red-700 px-4 py-3 text-sm">{error}</div>}
          <label htmlFor="login-email" className="block text-sm font-medium mb-1">E-mail</label>
          <input id="login-email" className="w-full rounded-xl border border-slate-200 px-4 py-3 mb-4 focus:outline-none focus:ring-2 focus:ring-brand-300" value={email} onChange={e=>setEmail(e.target.value)} type="email" required />
          <label htmlFor="login-password" className="block text-sm font-medium mb-1">Senha</label>
          <input id="login-password" className="w-full rounded-xl border border-slate-200 px-4 py-3 mb-6 focus:outline-none focus:ring-2 focus:ring-brand-300" value={password} onChange={e=>setPassword(e.target.value)} type="password" required />
          <button disabled={loading} className="w-full rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold py-3 transition disabled:opacity-60">{loading ? 'Entrando...' : 'Entrar'}</button>
          <p className="text-sm text-slate-600 mt-5 text-center">Não tem conta? <Link to="/register" className="text-brand-700 font-semibold">Cadastre-se</Link></p>
        </form>
      </div>
    </div>
  )
}
