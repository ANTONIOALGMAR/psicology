import { useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function AccountPage() {
  const { user } = useAuth()
  const [name, setName] = useState(user?.name || '')
  const [saving, setSaving] = useState(false)
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [pwMsg, setPwMsg] = useState('')
  const [pwErr, setPwErr] = useState('')
  const [changing, setChanging] = useState(false)

  const saveProfile = async () => {
    setMsg(''); setErr(''); setSaving(true)
    try {
      const res = await api.put('/auth/me', { name })
      const stored = localStorage.getItem('user')
      if (stored) {
        const u = JSON.parse(stored)
        u.name = res.data.name
        localStorage.setItem('user', JSON.stringify(u))
      }
      setMsg('Perfil atualizado com sucesso. Recarregue a página para ver o nome atualizado no topo.')
    } catch (e: any) {
      setErr(e?.response?.data?.message || 'Não foi possível atualizar o perfil.')
    } finally { setSaving(false) }
  }

  const changePassword = async () => {
    setPwMsg(''); setPwErr(''); setChanging(true)
    try {
      await api.post('/auth/change-password', { currentPassword, newPassword })
      setPwMsg('Senha alterada com sucesso.')
      setCurrentPassword(''); setNewPassword('')
    } catch (e: any) {
      setPwErr(e?.response?.data?.message || 'Não foi possível alterar a senha. Verifique a senha atual.')
    } finally { setChanging(false) }
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Minha conta</h1>
        <p className="text-slate-600">Gerencie seu perfil e segurança.</p>
      </div>

      <div className="glass rounded-3xl p-6">
        <h2 className="text-lg font-semibold mb-4">Perfil</h2>
        <div className="grid gap-4">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Nome</label>
            <input className="w-full rounded-xl border border-slate-200 px-4 py-2.5" value={name} onChange={e=>setName(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">E-mail</label>
            <input className="w-full rounded-xl border border-slate-200 px-4 py-2.5 bg-slate-50" value={user?.email || ''} disabled />
            <p className="text-xs text-slate-500 mt-1">O e-mail não pode ser alterado.</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Perfil de acesso</label>
            <input className="w-full rounded-xl border border-slate-200 px-4 py-2.5 bg-slate-50" value={user?.role === 'PATIENT' ? 'Paciente' : user?.role === 'PSYCHOLOGIST' ? 'Psicólogo(a)' : 'Administrador'} disabled />
          </div>
          {msg && <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-xl px-3 py-2">{msg}</div>}
          {err && <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{err}</div>}
          <div>
            <button onClick={saveProfile} disabled={saving || !name.trim()} className="rounded-xl bg-brand-600 hover:bg-brand-700 text-white px-5 py-2.5 font-semibold disabled:opacity-60">{saving ? 'Salvando...' : 'Salvar perfil'}</button>
          </div>
        </div>
      </div>

      <div className="glass rounded-3xl p-6">
        <h2 className="text-lg font-semibold mb-4">Alterar senha</h2>
        <div className="grid gap-4 max-w-md">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Senha atual</label>
            <input type="password" className="w-full rounded-xl border border-slate-200 px-4 py-2.5" value={currentPassword} onChange={e=>setCurrentPassword(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Nova senha</label>
            <input type="password" className="w-full rounded-xl border border-slate-200 px-4 py-2.5" value={newPassword} onChange={e=>setNewPassword(e.target.value)} placeholder="Mínimo 8 caracteres, com maiúscula, minúscula, número e especial" />
            <p className="text-xs text-slate-500 mt-1">Mínimo 8 caracteres, incluindo maiúscula, minúscula, número e caractere especial.</p>
          </div>
          {pwMsg && <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-xl px-3 py-2">{pwMsg}</div>}
          {pwErr && <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{pwErr}</div>}
          <div>
            <button onClick={changePassword} disabled={changing || !currentPassword || !/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,128}$/.test(newPassword)} className="rounded-xl bg-slate-800 hover:bg-slate-900 text-white px-5 py-2.5 font-semibold disabled:opacity-60">{changing ? 'Alterando...' : 'Alterar senha'}</button>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-600">
        <strong>Dica de segurança:</strong> use uma senha forte e não compartilhe suas credenciais. Em produção, recomenda-se alterar a senha do administrador padrão após o primeiro acesso.
      </div>
    </div>
  )
}
