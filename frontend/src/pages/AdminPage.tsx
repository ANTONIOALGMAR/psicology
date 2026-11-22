import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { AdminOverview, AdminQuestion, AdminTest, AdminUser, AuditEvent, Paged, PrivacyRequestView } from '../types'

type Tab = 'overview' | 'users' | 'tests' | 'audit' | 'requests'
const TABS: { key: Tab; label: string }[] = [
  { key: 'overview', label: 'Visão geral' },
  { key: 'users', label: 'Usuários' },
  { key: 'tests', label: 'Catálogo de testes' },
  { key: 'requests', label: 'Solicitações LGPD' },
  { key: 'audit', label: 'Auditoria' },
]
const roleLabel = (r: string) => r === 'ADMIN' ? 'Admin' : r === 'PSYCHOLOGIST' ? 'Psicólogo(a)' : 'Paciente'
const typeLabel = (t: string) => t === 'VOCATIONAL' ? 'Vocacional' : 'Psicológico'
const statusLabel = (s: string) => s === 'PENDING' ? 'Pendente' : s === 'APPROVED' ? 'Aprovada' : 'Rejeitada'
const fmt = (v?: string) => v ? new Date(v).toLocaleString('pt-BR') : '—'
const message = (e: any) => e?.response?.data?.message || 'Não foi possível concluir a operação.'

export default function AdminPage() {
  const qc = useQueryClient()
  const [tab, setTab] = useState<Tab>('overview')
  const [notice, setNotice] = useState<{ text: string; error?: boolean } | null>(null)

  const [userSearch, setUserSearch] = useState('')
  const [userRole, setUserRole] = useState('')
  const [newUser, setNewUser] = useState({ name: '', email: '', password: '', role: 'PATIENT' })
  const [resetFor, setResetFor] = useState<string | null>(null)
  const [newPassword, setNewPassword] = useState('')

  const [newTest, setNewTest] = useState({ type: 'PSYCHOLOGICAL', title: '', description: '' })
  const [questionFor, setQuestionFor] = useState<string | null>(null)
  const [question, setQuestion] = useState({ prompt: '', kind: 'SCALE', options: '' })

  const [auditAction, setAuditAction] = useState('')
  const [auditPage, setAuditPage] = useState(0)

  const overview = useQuery({ queryKey: ['admin-overview'], queryFn: async () => (await api.get<AdminOverview>('/admin/overview')).data })
  const users = useQuery({
    queryKey: ['admin-users', userSearch, userRole],
    queryFn: async () => (await api.get<Paged<AdminUser>>('/admin/users', { params: { q: userSearch, role: userRole, size: 100 } })).data,
  })
  const tests = useQuery({ queryKey: ['admin-tests'], queryFn: async () => (await api.get<AdminTest[]>('/admin/tests')).data })
  const requests = useQuery({ queryKey: ['admin-privacy-requests'], queryFn: async () => (await api.get<PrivacyRequestView[]>('/admin/privacy-requests')).data })
  const audit = useQuery({
    queryKey: ['admin-audit', auditAction, auditPage],
    queryFn: async () => (await api.get<Paged<AuditEvent>>('/admin/audit', { params: { action: auditAction, page: auditPage, size: 20 } })).data,
  })
  const questions = useQuery({
    queryKey: ['admin-questions', questionFor],
    enabled: !!questionFor,
    queryFn: async () => (await api.get<AdminQuestion[]>(`/admin/tests/${questionFor}/questions`)).data,
  })

  const ok = (text: string) => setNotice({ text })
  const fail = (e: any) => setNotice({ text: message(e), error: true })
  const refreshAll = () => {
    qc.invalidateQueries({ queryKey: ['admin-overview'] })
    qc.invalidateQueries({ queryKey: ['admin-users'] })
    qc.invalidateQueries({ queryKey: ['admin-tests'] })
    qc.invalidateQueries({ queryKey: ['admin-privacy-requests'] })
    qc.invalidateQueries({ queryKey: ['admin-audit'] })
  }

  const mutate = (fn: () => Promise<unknown>, success: string) => {
    setNotice(null)
    return fn().then(() => { ok(success); refreshAll() }).catch(fail)
  }

  const createUser = () => mutate(
    () => api.post('/admin/users', newUser),
    'Usuário criado com sucesso.'
  ).then(() => setNewUser({ name: '', email: '', password: '', role: 'PATIENT' }))

  const createTest = () => mutate(
    () => api.post('/admin/tests', newTest),
    'Teste criado com sucesso.'
  ).then(() => setNewTest({ type: 'PSYCHOLOGICAL', title: '', description: '' }))

  const addQuestion = () => {
    if (!questionFor) return
    const options = question.options.split('\n').map(line => line.trim()).filter(Boolean).map(line => {
      const [label, value] = line.split('=')
      return { label: label.trim(), value: Number(value ?? 0) || 0 }
    })
    mutate(
      () => api.post(`/admin/tests/${questionFor}/questions`, {
        prompt: question.prompt, kind: question.kind, options,
      }),
      'Questão adicionada.'
    ).then(() => { setQuestion({ prompt: '', kind: 'SCALE', options: '' }); qc.invalidateQueries({ queryKey: ['admin-questions', questionFor] }) })
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Administração</h1>
        <p className="text-slate-600">Gestão de usuários, catálogo, solicitações de privacidade e trilha de auditoria.</p>
      </div>

      <div className="flex gap-2 overflow-x-auto pb-1">
        {TABS.map(t => (
          <button key={t.key} onClick={() => setTab(t.key)}
            className={`px-3 py-2 rounded-lg text-sm font-medium whitespace-nowrap ${tab === t.key ? 'bg-brand-600 text-white' : 'bg-white text-slate-600 hover:bg-slate-100 border border-slate-200'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {notice && <div className={`text-sm rounded-xl px-3 py-2 border ${notice.error ? 'text-red-700 bg-red-50 border-red-200' : 'text-green-700 bg-green-50 border-green-200'}`}>{notice.text}</div>}

      {tab === 'overview' && (
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {[
            { label: 'Usuários', value: overview.data?.users },
            { label: 'Pacientes', value: overview.data?.patients },
            { label: 'Psicólogos', value: overview.data?.psychologists },
            { label: 'Administradores', value: overview.data?.admins },
            { label: 'Testes ativos', value: overview.data?.activeTests },
            { label: 'Solicitações LGPD pendentes', value: overview.data?.pendingPrivacyRequests },
            { label: 'Eventos de auditoria (7d)', value: overview.data?.auditEventsLast7Days },
          ].map(card => (
            <div key={card.label} className="glass rounded-2xl p-5">
              <div className="text-3xl font-bold text-brand-700">{card.value ?? '—'}</div>
              <div className="text-sm text-slate-600 mt-1">{card.label}</div>
            </div>
          ))}
        </div>
      )}

      {tab === 'users' && (
        <div className="space-y-5">
          <section className="glass rounded-3xl p-6">
            <h2 className="text-lg font-semibold mb-4">Novo usuário</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <input className="rounded-xl border border-slate-200 px-3 py-2" placeholder="Nome" value={newUser.name} onChange={e => setNewUser({ ...newUser, name: e.target.value })} />
              <input className="rounded-xl border border-slate-200 px-3 py-2" placeholder="E-mail" type="email" value={newUser.email} onChange={e => setNewUser({ ...newUser, email: e.target.value })} />
              <input className="rounded-xl border border-slate-200 px-3 py-2" placeholder="Senha (mín. 8)" type="password" value={newUser.password} onChange={e => setNewUser({ ...newUser, password: e.target.value })} />
              <select className="rounded-xl border border-slate-200 px-3 py-2" value={newUser.role} onChange={e => setNewUser({ ...newUser, role: e.target.value })}>
                <option value="PATIENT">Paciente</option><option value="PSYCHOLOGIST">Psicólogo(a)</option><option value="ADMIN">Administrador</option>
              </select>
            </div>
            <button onClick={createUser} disabled={!newUser.name || !newUser.email || newUser.password.length < 8}
              className="mt-4 rounded-xl bg-brand-600 text-white px-5 py-2.5 font-semibold disabled:opacity-60">Criar usuário</button>
          </section>

          <section className="glass rounded-3xl p-6">
            <div className="flex flex-wrap gap-3 mb-4">
              <input className="rounded-xl border border-slate-200 px-3 py-2 flex-1 min-w-[180px]" placeholder="Buscar nome ou e-mail" value={userSearch} onChange={e => setUserSearch(e.target.value)} />
              <select className="rounded-xl border border-slate-200 px-3 py-2" value={userRole} onChange={e => setUserRole(e.target.value)}>
                <option value="">Todos os perfis</option><option value="PATIENT">Paciente</option><option value="PSYCHOLOGIST">Psicólogo(a)</option><option value="ADMIN">Admin</option>
              </select>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="text-left text-slate-500 border-b border-slate-200">
                  <tr><th className="py-2">Usuário</th><th className="py-2">Perfil</th><th className="py-2">Status</th><th className="py-2">Ações</th></tr>
                </thead>
                <tbody>
                  {users.data?.content.map(u => (
                    <tr key={u.id} className="border-b border-slate-100 align-middle">
                      <td className="py-2"><div className="font-medium">{u.name}</div><div className="text-xs text-slate-500">{u.email}</div></td>
                      <td className="py-2">
                        <select className="rounded-lg border border-slate-200 px-2 py-1" value={u.role}
                          onChange={e => mutate(() => api.put(`/admin/users/${u.id}/role`, { role: e.target.value }), 'Perfil atualizado.')}>
                          <option value="PATIENT">Paciente</option><option value="PSYCHOLOGIST">Psicólogo(a)</option><option value="ADMIN">Admin</option>
                        </select>
                      </td>
                      <td className="py-2">
                        <button onClick={() => mutate(() => api.put(`/admin/users/${u.id}/enabled`, { enabled: !u.enabled }), 'Status atualizado.')}
                          className={`rounded-full px-2.5 py-1 text-xs font-semibold ${u.enabled ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-600'}`}>
                          {u.enabled ? 'Ativo' : 'Inativo'}
                        </button>
                      </td>
                      <td className="py-2">
                        <button className="text-brand-700 hover:underline text-xs" onClick={() => { setResetFor(resetFor === u.id ? null : u.id); setNewPassword('') }}>Redefinir senha</button>
                        {resetFor === u.id && (
                          <div className="flex gap-2 mt-2">
                            <input type="password" className="rounded-lg border border-slate-200 px-2 py-1 text-xs" placeholder="Nova senha" value={newPassword} onChange={e => setNewPassword(e.target.value)} />
                            <button disabled={newPassword.length < 8} onClick={() => mutate(() => api.post(`/admin/users/${u.id}/reset-password`, { newPassword }), 'Senha redefinida.').then(() => setResetFor(null))}
                              className="text-xs rounded-lg bg-slate-800 text-white px-2 py-1 disabled:opacity-60">Salvar</button>
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {users.data?.totalElements === 0 && <p className="text-sm text-slate-500 py-4">Nenhum usuário encontrado.</p>}
            </div>
          </section>
        </div>
      )}

      {tab === 'tests' && (
        <div className="space-y-5">
          <section className="glass rounded-3xl p-6">
            <h2 className="text-lg font-semibold mb-4">Novo teste</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <select className="rounded-xl border border-slate-200 px-3 py-2" value={newTest.type} onChange={e => setNewTest({ ...newTest, type: e.target.value })}>
                <option value="PSYCHOLOGICAL">Psicológico</option><option value="VOCATIONAL">Vocacional</option>
              </select>
              <input className="rounded-xl border border-slate-200 px-3 py-2" placeholder="Título" value={newTest.title} onChange={e => setNewTest({ ...newTest, title: e.target.value })} />
              <input className="rounded-xl border border-slate-200 px-3 py-2 sm:col-span-2" placeholder="Descrição" value={newTest.description} onChange={e => setNewTest({ ...newTest, description: e.target.value })} />
            </div>
            <button onClick={createTest} disabled={!newTest.title.trim()} className="mt-4 rounded-xl bg-brand-600 text-white px-5 py-2.5 font-semibold disabled:opacity-60">Criar teste</button>
          </section>

          <section className="space-y-4">
            {tests.data?.map(t => (
              <div key={t.id} className="glass rounded-3xl p-6">
                <div className="flex items-start justify-between gap-4 flex-wrap">
                  <div>
                    <span className="text-xs font-semibold px-2 py-1 rounded-full bg-slate-100 text-slate-700">{typeLabel(t.type)}</span>
                    <h3 className="text-lg font-semibold mt-2">{t.title}</h3>
                    <p className="text-sm text-slate-600">{t.description}</p>
                    <p className="text-xs text-slate-500 mt-1">{t.questionCount} questão(ões)</p>
                  </div>
                  <button onClick={() => mutate(() => api.put(`/admin/tests/${t.id}/active`, null, { params: { active: !t.active } }), 'Status do teste atualizado.')}
                    className={`rounded-full px-3 py-1 text-xs font-semibold ${t.active ? 'bg-green-100 text-green-700' : 'bg-slate-200 text-slate-600'}`}>
                    {t.active ? 'Ativo' : 'Inativo'}
                  </button>
                </div>
                <button className="mt-4 text-sm text-brand-700 hover:underline" onClick={() => setQuestionFor(questionFor === t.id ? null : t.id)}>
                  {questionFor === t.id ? 'Fechar questões' : 'Gerenciar questões'}
                </button>
                {questionFor === t.id && (
                  <div className="mt-4 border-t border-slate-200 pt-4 space-y-4">
                    {questions.data?.map(q => (
                      <div key={q.id} className="flex items-start justify-between gap-3 text-sm">
                        <div><span className="font-medium">{q.position}. {q.prompt}</span><div className="text-xs text-slate-500">{q.options.length} opção(ões)</div></div>
                        <button className="text-red-600 hover:underline text-xs" onClick={() => mutate(() => api.delete(`/admin/tests/${t.id}/questions/${q.id}`), 'Questão removida.').then(() => qc.invalidateQueries({ queryKey: ['admin-questions', t.id] }))}>Remover</button>
                      </div>
                    ))}
                    <div className="grid gap-3 rounded-xl bg-slate-50 p-4">
                      <input className="rounded-xl border border-slate-200 px-3 py-2" placeholder="Enunciado da questão" value={question.prompt} onChange={e => setQuestion({ ...question, prompt: e.target.value })} />
                      <div className="grid sm:grid-cols-2 gap-3">
                        <select className="rounded-xl border border-slate-200 px-3 py-2" value={question.kind} onChange={e => setQuestion({ ...question, kind: e.target.value })}>
                          <option value="SCALE">Escala</option><option value="SINGLE_CHOICE">Múltipla escolha</option><option value="TEXT">Texto aberto</option>
                        </select>
                        <input className="rounded-xl border border-slate-200 px-3 py-2" placeholder="Opções: Rótulo=valor (uma por linha)" value={question.options} onChange={e => setQuestion({ ...question, options: e.target.value })} />
                      </div>
                      <button disabled={!question.prompt.trim()} onClick={addQuestion} className="justify-self-start rounded-xl bg-slate-800 text-white px-4 py-2 text-sm font-semibold disabled:opacity-60">Adicionar questão</button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </section>
        </div>
      )}

      {tab === 'requests' && (
        <section className="glass rounded-3xl p-6">
          <h2 className="text-lg font-semibold mb-4">Solicitações de titulares</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-slate-500 border-b border-slate-200">
                <tr><th className="py-2">Tipo</th><th className="py-2">Status</th><th className="py-2">Solicitada em</th><th className="py-2">Detalhes</th><th className="py-2">Ação</th></tr>
              </thead>
              <tbody>
                {requests.data?.map(r => (
                  <tr key={r.id} className="border-b border-slate-100 align-middle">
                    <td className="py-2">{r.type === 'DATA_DELETION' ? 'Exclusão' : 'Cópia dos dados'}</td>
                    <td className="py-2">{statusLabel(r.status)}</td>
                    <td className="py-2">{fmt(r.requestedAt)}</td>
                    <td className="py-2 text-slate-600">{r.details || '—'}</td>
                    <td className="py-2">
                      <select className="rounded-lg border border-slate-200 px-2 py-1" value={r.status}
                        onChange={e => mutate(() => api.put(`/admin/privacy-requests/${r.id}/status`, { status: e.target.value }), 'Solicitação atualizada.')}>
                        <option value="PENDING">Pendente</option><option value="APPROVED">Aprovada</option><option value="REJECTED">Rejeitada</option>
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {requests.data?.length === 0 && <p className="text-sm text-slate-500 py-4">Nenhuma solicitação registrada.</p>}
          </div>
          <p className="text-xs text-slate-500 mt-4">A aprovação registra a decisão na auditoria. A execução efetiva de exportações/exclusões deve seguir o procedimento interno de governança de dados.</p>
        </section>
      )}

      {tab === 'audit' && (
        <section className="glass rounded-3xl p-6">
          <div className="flex flex-wrap gap-3 mb-4">
            <input className="rounded-xl border border-slate-200 px-3 py-2 flex-1 min-w-[180px]" placeholder="Filtrar por ator ou recurso" value={auditAction} onChange={e => { setAuditAction(e.target.value); setAuditPage(0) }} />
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-slate-500 border-b border-slate-200">
                <tr><th className="py-2">Data</th><th className="py-2">Ator</th><th className="py-2">Ação</th><th className="py-2">Recurso</th><th className="py-2">Detalhes</th></tr>
              </thead>
              <tbody>
                {audit.data?.content.map(a => (
                  <tr key={a.id} className="border-b border-slate-100 align-top">
                    <td className="py-2 whitespace-nowrap">{fmt(a.createdAt)}</td>
                    <td className="py-2">{a.actorEmail || 'Sistema'}</td>
                    <td className="py-2"><span className="font-mono text-xs">{a.action}</span></td>
                    <td className="py-2">{a.resourceType} {a.resourceId ? `· ${a.resourceId.slice(0, 8)}` : ''}</td>
                    <td className="py-2 text-slate-600">{a.details || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {audit.data?.content.length === 0 && <p className="text-sm text-slate-500 py-4">Nenhum evento encontrado.</p>}
          </div>
          <div className="flex items-center gap-3 mt-4 text-sm">
            <button disabled={auditPage === 0} onClick={() => setAuditPage(p => p - 1)} className="rounded-lg border border-slate-200 px-3 py-1 disabled:opacity-50">Anterior</button>
            <span>Página {(audit.data?.number ?? 0) + 1} de {Math.max(audit.data?.totalPages ?? 1, 1)}</span>
            <button disabled={auditPage + 1 >= (audit.data?.totalPages ?? 1)} onClick={() => setAuditPage(p => p + 1)} className="rounded-lg border border-slate-200 px-3 py-1 disabled:opacity-50">Próxima</button>
          </div>
        </section>
      )}
    </div>
  )
}
