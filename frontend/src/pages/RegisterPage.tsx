import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RegisterPage() {
  const { register } = useAuth()
  const nav = useNavigate()
  const [sp] = useSearchParams()
  const [form, setForm] = useState({ name: '', email: '', password: '', inviteCode: sp.get('convite') || '', role: 'PATIENT' })
  const [consents, setConsents] = useState({ terms: false, privacy: false })
  const [useInvite, setUseInvite] = useState(!!sp.get('convite'))
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault(); setError(''); setLoading(true)
    try {
      await register({ ...form, inviteCode: useInvite ? form.inviteCode : undefined,
        acceptTerms: consents.terms, acceptPrivacy: consents.privacy })
      nav('/')
    } catch (err: any) { setError(err?.response?.data?.message || err?.message || 'Não foi possível criar a conta.') }
    finally { setLoading(false) }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-6 bg-gradient-to-br from-brand-50 via-slate-50 to-teal-100">
      <form onSubmit={submit} className="glass w-full max-w-lg rounded-3xl p-8">
        <h2 className="text-2xl font-bold text-slate-900 mb-1">Crie sua conta</h2>
        <p className="text-slate-500 mb-6">Cadastro aberto para pacientes. Se você recebeu um convite, ative a opção abaixo.</p>
        {error && <div className="mb-4 rounded-xl bg-red-50 text-red-700 px-4 py-3 text-sm">{error}</div>}
        <div className="grid sm:grid-cols-2 gap-4">
          <div className="sm:col-span-2">
            <label className="block text-sm font-medium mb-1">Nome completo</label>
            <input className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:outline-none focus:ring-2 focus:ring-brand-300" value={form.name} onChange={e=>setForm({...form,name:e.target.value})} required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">E-mail</label>
            <input type="email" className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:outline-none focus:ring-2 focus:ring-brand-300" value={form.email} onChange={e=>setForm({...form,email:e.target.value})} required />
          </div>
          <div>
            <label className="block text-sm font-medium mb-1">Senha</label>
            <input type="password" minLength={6} className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:outline-none focus:ring-2 focus:ring-brand-300" value={form.password} onChange={e=>setForm({...form,password:e.target.value})} required />
          </div>
        </div>
        <div className="mt-5 rounded-2xl border border-slate-200 p-4 bg-slate-50/60">
          <label className="flex items-center gap-2 font-medium text-sm cursor-pointer">
            <input type="checkbox" checked={useInvite} onChange={e=>setUseInvite(e.target.checked)} />
            Tenho um código de convite
          </label>
          {useInvite && (
            <div className="mt-3">
              <label className="block text-sm font-medium mb-1">Código do convite</label>
              <input className="w-full rounded-xl border border-slate-200 px-4 py-3 focus:outline-none focus:ring-2 focus:ring-brand-300 uppercase tracking-widest" value={form.inviteCode} onChange={e=>setForm({...form,inviteCode:e.target.value})} placeholder="EX: A1B2C3D4" />
              <p className="text-xs text-slate-500 mt-1">O convite pode definir seu perfil (paciente, psicólogo, etc.).</p>
            </div>
          )}
        </div>
        <div className="mt-5 space-y-2 rounded-2xl border border-slate-200 p-4 bg-slate-50/60">
          <label className="flex items-start gap-2 text-sm cursor-pointer">
            <input type="checkbox" className="mt-0.5" required checked={consents.terms} onChange={e => setConsents({ ...consents, terms: e.target.checked })} />
            <span>Li e aceito os termos de uso do sistema.</span>
          </label>
          <label className="flex items-start gap-2 text-sm cursor-pointer">
            <input type="checkbox" className="mt-0.5" required checked={consents.privacy} onChange={e => setConsents({ ...consents, privacy: e.target.checked })} />
            <span>Estou ciente da política de privacidade e do tratamento dos meus dados sensíveis (LGPD).</span>
          </label>
        </div>
        <button disabled={loading || !consents.terms || !consents.privacy} className="w-full mt-6 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold py-3 transition disabled:opacity-60">{loading ? 'Criando conta...' : 'Criar conta'}</button>
        <p className="text-sm text-slate-600 mt-5 text-center">Já tem conta? <Link to="/login" className="text-brand-700 font-semibold">Entrar</Link></p>
      </form>
    </div>
  )
}
