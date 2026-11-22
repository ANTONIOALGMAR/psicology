import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { PrivacyOverview } from '../types'

const CONSENT_KINDS = [
  { kind: 'TERMS', label: 'Termos de uso do sistema', version: '2026-11' },
  { kind: 'PRIVACY', label: 'Política de privacidade e tratamento de dados', version: '2026-11' },
  { kind: 'DATA_SHARING', label: 'Compartilhamento com profissional vinculado', version: '2026-11' },
]

export default function PrivacyPage() {
  const qc = useQueryClient()
  const { data, isLoading } = useQuery({
    queryKey: ['privacy-me'],
    queryFn: async () => (await api.get<PrivacyOverview>('/privacy/me')).data,
  })
  const [decisions, setDecisions] = useState<Record<string, boolean>>({})
  const [requestType, setRequestType] = useState('DATA_EXPORT')
  const [details, setDetails] = useState('')
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  useEffect(() => {
    if (!data) return
    const next: Record<string, boolean> = {}
    for (const item of data.consents) if (!(item.kind in next)) next[item.kind] = item.accepted
    setDecisions(next)
  }, [data])

  const saveConsents = useMutation({
    mutationFn: async () => {
      for (const item of CONSENT_KINDS) {
        await api.post('/privacy/consents', { kind: item.kind, accepted: !!decisions[item.kind] })
      }
    },
    onSuccess: () => { setMsg('Preferências de privacidade salvas.'); setErr(''); qc.invalidateQueries({ queryKey: ['privacy-me'] }) },
    onError: () => { setErr('Não foi possível salvar suas preferências.'); setMsg('') },
  })

  const createRequest = useMutation({
    mutationFn: async () => { await api.post('/privacy/requests', { type: requestType, details: details || undefined }) },
    onSuccess: () => { setMsg('Solicitação registrada. A equipe administrativa fará o acompanhamento.'); setErr(''); setDetails(''); qc.invalidateQueries({ queryKey: ['privacy-me'] }) },
    onError: (e: any) => { setErr(e?.response?.data?.message || 'Não foi possível registrar a solicitação.'); setMsg('') },
  })

  const exportData = async () => {
    setMsg(''); setErr('')
    try {
      const res = await api.get('/privacy/export')
      const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `psicare-dados-${new Date().toISOString().slice(0, 10)}.json`
      link.click()
      URL.revokeObjectURL(url)
      setMsg('Seus dados foram exportados em JSON.')
    } catch { setErr('Não foi possível exportar seus dados.') }
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Privacidade e meus dados</h1>
        <p className="text-slate-600">Você controla seus consentimentos e pode solicitar uma cópia ou a exclusão dos seus dados.</p>
      </div>
      {msg && <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-xl px-3 py-2">{msg}</div>}
      {err && <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{err}</div>}

      <section className="glass rounded-3xl p-6">
        <h2 className="text-lg font-semibold mb-1">Consentimentos</h2>
        <p className="text-sm text-slate-600 mb-4">Versão vigente: {CONSENT_KINDS[0].version}. Cada alteração é registrada na trilha de auditoria.</p>
        {isLoading ? <p className="text-sm text-slate-500">Carregando...</p> : (
          <div className="space-y-3">
            {CONSENT_KINDS.map(item => (
              <label key={item.kind} className="flex items-start gap-3 rounded-xl border border-slate-200 p-3 cursor-pointer hover:bg-slate-50">
                <input type="checkbox" className="mt-1" checked={!!decisions[item.kind]}
                  onChange={e => setDecisions(prev => ({ ...prev, [item.kind]: e.target.checked }))} />
                <span>
                  <span className="block text-sm font-medium">{item.label}</span>
                  <span className="block text-xs text-slate-500">
                    {decisions[item.kind] ? 'Consentimento registrado' : 'Sem consentimento ativo'}
                  </span>
                </span>
              </label>
            ))}
            <button onClick={() => saveConsents.mutate()} disabled={saveConsents.isPending}
              className="rounded-xl bg-brand-600 hover:bg-brand-700 text-white px-5 py-2.5 font-semibold disabled:opacity-60">
              {saveConsents.isPending ? 'Salvando...' : 'Salvar preferências'}
            </button>
          </div>
        )}
      </section>

      <section className="glass rounded-3xl p-6">
        <h2 className="text-lg font-semibold mb-1">Portabilidade dos dados</h2>
        <p className="text-sm text-slate-600 mb-4">Baixe diário, anexos (metadados), comentários, testes e consentimentos em um arquivo JSON.</p>
        <button onClick={exportData} className="rounded-xl border border-brand-200 bg-brand-50 text-brand-800 px-5 py-2.5 font-semibold hover:bg-brand-100">Exportar meus dados</button>
      </section>

      <section className="glass rounded-3xl p-6">
        <h2 className="text-lg font-semibold mb-4">Solicitações de privacidade</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Tipo de solicitação</label>
            <select className="w-full rounded-xl border border-slate-200 px-4 py-2.5" value={requestType} onChange={e => setRequestType(e.target.value)}>
              <option value="DATA_EXPORT">Cópia dos meus dados</option>
              <option value="DATA_DELETION">Exclusão dos meus dados</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Detalhes (opcional)</label>
            <input className="w-full rounded-xl border border-slate-200 px-4 py-2.5" value={details} onChange={e => setDetails(e.target.value)} />
          </div>
        </div>
        <button onClick={() => createRequest.mutate()} disabled={createRequest.isPending}
          className="mt-4 rounded-xl bg-slate-800 hover:bg-slate-900 text-white px-5 py-2.5 font-semibold disabled:opacity-60">
          {createRequest.isPending ? 'Enviando...' : 'Enviar solicitação'}
        </button>

        {!!data?.requests.length && (
          <div className="mt-6 overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="text-left text-slate-500 border-b border-slate-200">
                <tr><th className="py-2">Tipo</th><th className="py-2">Status</th><th className="py-2">Data</th></tr>
              </thead>
              <tbody>
                {data.requests.map(r => (
                  <tr key={r.id} className="border-b border-slate-100">
                    <td className="py-2">{r.type === 'DATA_DELETION' ? 'Exclusão' : 'Cópia dos dados'}</td>
                    <td className="py-2">{r.status === 'PENDING' ? 'Pendente' : r.status === 'APPROVED' ? 'Aprovada' : 'Rejeitada'}</td>
                    <td className="py-2">{new Date(r.requestedAt).toLocaleDateString('pt-BR')}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <p className="text-xs text-slate-500">A exclusão de dados é processada pela equipe administrativa conforme a política de retenção e obrigações legais aplicáveis.</p>
    </div>
  )
}
