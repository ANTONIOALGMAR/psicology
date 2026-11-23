import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Attempt, CrisisAlert, JournalEntry, PatientSummary, PsychologistDirectoryEntry, TransferHistoryEntry } from '../types'

export default function PsychologistPage() {
  const qc = useQueryClient()
  const { data: patients = [], isLoading } = useQuery({ queryKey: ['psy-patients'], queryFn: async () => (await api.get<PatientSummary[]>('/psychologist/patients')).data })
  const [selected, setSelected] = useState<PatientSummary | null>(null)
  const [email, setEmail] = useState('')
  const [commentText, setCommentText] = useState<Record<string, string>>({})
  const [showTransfer, setShowTransfer] = useState(false)
  const [targetId, setTargetId] = useState('')
  const [reason, setReason] = useState('')
  const [transferMsg, setTransferMsg] = useState('')
  const token = localStorage.getItem('token')

  const { data: journal = [], isLoading: loadingJournal } = useQuery({
    queryKey: ['psy-journal', selected?.id],
    queryFn: async () => (await api.get<JournalEntry[]>(`/psychologist/patients/${selected!.id}/journal`)).data,
    enabled: !!selected
  })
  const { data: attempts = [], isLoading: loadingAttempts } = useQuery({
    queryKey: ['psy-attempts', selected?.id],
    queryFn: async () => (await api.get<Attempt[]>(`/psychologist/patients/${selected!.id}/attempts`)).data,
    enabled: !!selected
  })

  const directory = useQuery({
    queryKey: ['psy-directory'],
    enabled: showTransfer,
    queryFn: async () => (await api.get<PsychologistDirectoryEntry[]>('/psychologist/directory')).data,
  })
  const transferHistory = useQuery({
    queryKey: ['psy-transfers', selected?.id],
    enabled: !!selected,
    queryFn: async () => (await api.get<TransferHistoryEntry[]>(`/psychologist/patients/${selected!.id}/transfers`)).data,
  })
  const alerts = useQuery({
    queryKey: ['chat-alerts'],
    queryFn: async () => (await api.get<CrisisAlert[]>('/psychologist/alerts')).data,
  })
  const transfer = useMutation({
    mutationFn: async () => {
      if (!selected) return
      await api.post(`/psychologist/patients/${selected.id}/transfer`, { toPsychologistId: targetId, reason: reason || undefined })
    },
    onSuccess: () => {
      setTransferMsg('Cliente transferido. Os agendamentos futuros foram reatribuídos.'); setShowTransfer(false); setTargetId(''); setReason('')
      setSelected(null)
      qc.invalidateQueries({ queryKey: ['psy-patients'] })
      qc.invalidateQueries({ queryKey: ['appointments'] })
    },
    onError: (e: any) => setTransferMsg(e?.response?.data?.message || 'Não foi possível transferir.'),
  })

  const link = useMutation({
    mutationFn: async () => (await api.post('/psychologist/patients/link', { email })).data,
    onSuccess: () => { setEmail(''); qc.invalidateQueries({ queryKey: ['psy-patients'] }) }
  })

  const addComment = useMutation({
    mutationFn: async ({ entryId, content }: { entryId: string, content: string }) => (await api.post(`/psychologist/journal/${entryId}/comments`, { content })).data,
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['psy-journal', selected?.id] }) }
  })

  const withToken = (url: string) => token ? `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}` : url

  const exportAttempts = async () => {
    if (!selected) return
    const res = await api.get(`/psychologist/patients/${selected.id}/attempts/export`, { responseType: 'blob' })
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `tentativas-${selected.name.replace(/[^a-z0-9_-]+/gi,'_')}.csv`
    a.click()
    URL.revokeObjectURL(url)
  }

  const attemptsByTest = useMemo(() => {
    const map = new Map<string, Attempt[]>()
    for (const a of attempts) {
      const arr = map.get(a.testTitle) || []
      arr.push(a)
      map.set(a.testTitle, arr)
    }
    return Array.from(map.entries())
  }, [attempts])

  return (
    <div className="grid lg:grid-cols-4 gap-6">
      <section className="lg:col-span-1 space-y-4">
        <div className="glass rounded-3xl p-5">
          <h2 className="font-bold text-lg mb-3">Meus pacientes</h2>
          {isLoading && <div className="text-sm text-slate-500">Carregando...</div>}
          <div className="space-y-2">
            {patients.map(p => (
              <button key={p.id} onClick={() => setSelected(p)} className={`w-full text-left rounded-xl px-3 py-2 border ${selected?.id===p.id ? 'bg-brand-50 border-brand-200' : 'border-slate-200 hover:bg-slate-50'}`}>
                <div className="font-medium text-sm">{p.name}</div>
                <div className="text-xs text-slate-500">{p.email}</div>
              </button>
            ))}
            {!isLoading && patients.length === 0 && <div className="text-sm text-slate-500">Nenhum paciente vinculado.</div>}
          </div>
        </div>
        <div className="glass rounded-3xl p-5">
          <h3 className="font-semibold mb-2">Vincular paciente</h3>
          <p className="text-xs text-slate-500 mb-2">Informe o e-mail de um paciente já cadastrado.</p>
          <label htmlFor="link-email" className="sr-only">E-mail do paciente</label>
          <input id="link-email" className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm mb-2" placeholder="email@paciente.com" value={email} onChange={e=>setEmail(e.target.value)} />
          <button onClick={() => email && link.mutate()} disabled={!email || link.isPending} className="w-full rounded-xl bg-brand-600 text-white py-2 text-sm font-semibold disabled:opacity-60">Vincular</button>
          {link.isError && <div className="text-xs text-red-600 mt-2">Não foi possível vincular (verifique o e-mail).</div>}
        </div>

        <div className="glass rounded-3xl p-5 border-red-100 bg-red-50/60">
          <div className="flex items-center justify-between gap-2">
            <h3 className="font-semibold text-red-900">Alertas de crise</h3>
            <span className="text-xs rounded-full bg-red-100 text-red-800 px-2 py-0.5">{alerts.data?.length ?? 0}</span>
          </div>
          {!!alerts.data?.length ? (
            <div className="mt-3 space-y-2">
              {alerts.data.map(a => (
                <div key={a.id} className="text-xs rounded-xl bg-white border border-red-100 px-3 py-2">
                  <div className="font-semibold text-red-900">{a.patientName}</div>
                  <div className="text-slate-600">{new Date(a.createdAt).toLocaleString('pt-BR')}</div>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-xs text-slate-600 mt-2">Nenhum alerta recente.</p>
          )}
          <p className="text-[11px] text-slate-500 mt-3">
            O alerta mostra apenas paciente e data. O conteúdo da conversa fica no histórico restrito da auditoria.
          </p>
        </div>
      </section>
      <section className="lg:col-span-3 space-y-4">
        {!selected && <div className="glass rounded-3xl p-8 text-center text-slate-500">Selecione um paciente para visualizar diário, evolução em testes e registrar feedback.</div>}
        {selected && (
          <>
            <div className="rounded-3xl bg-white border border-slate-100 p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-xl font-bold">Acompanhamento de {selected.name}</h2>
                  <p className="text-slate-500 text-sm">{selected.email}</p>
                </div>
                <button onClick={() => { setShowTransfer(!showTransfer); setTransferMsg('') }} className="rounded-xl border border-brand-200 bg-brand-50 text-brand-800 px-3 py-2 text-sm font-semibold">
                  {showTransfer ? 'Cancelar transferência' : 'Transferir cliente'}
                </button>
              </div>

              {showTransfer && (
                <div className="mt-4 rounded-2xl border border-brand-200 bg-brand-50/60 p-4 space-y-3">
                  <p className="text-sm text-slate-700">
                    Ao transferir, o vínculo passa para o profissional escolhido e os <strong>agendamentos futuros</strong> são reatribuídos.
                    O diário e os resultados de testes permanecem no histórico; a ação fica registrada na auditoria.
                  </p>
                  <label htmlFor="transfer-target" className="block text-sm font-medium text-slate-700">Novo psicólogo</label>
                  <select id="transfer-target" className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" value={targetId} onChange={e => setTargetId(e.target.value)}>
                    <option value="">Selecione...</option>
                    {directory.data?.map(d => <option key={d.id} value={d.id}>{d.name}</option>)}
                  </select>
                  <label htmlFor="transfer-reason" className="block text-sm font-medium text-slate-700">Motivo (opcional)</label>
                  <input id="transfer-reason" className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm" value={reason} onChange={e => setReason(e.target.value)} placeholder="Ex.: encaminhamento para especialidade" />
                  <button onClick={() => transfer.mutate()} disabled={!targetId || transfer.isPending}
                    className="rounded-xl bg-brand-600 text-white px-4 py-2 text-sm font-semibold disabled:opacity-60">Confirmar transferência</button>
                </div>
              )}
              {transferMsg && <div className="mt-3 text-sm rounded-xl bg-slate-50 border border-slate-200 px-3 py-2">{transferMsg}</div>}
              {!!transferHistory.data?.length && (
                <details className="mt-3 text-sm">
                  <summary className="cursor-pointer text-brand-700">Histórico de transferências</summary>
                  <div className="mt-2 space-y-1 text-slate-600">
                    {transferHistory.data.map(h => <div key={h.id}>{new Date(h.createdAt).toLocaleString('pt-BR')} — {h.details}</div>)}
                  </div>
                </details>
              )}
            </div>

            <div className="glass rounded-3xl p-5">
              <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
                <h3 className="text-lg font-bold">Evolução em testes</h3>
                <button onClick={exportAttempts} disabled={attempts.length === 0} className="rounded-xl border border-slate-300 bg-white px-3 py-2 text-sm font-semibold disabled:opacity-50">Exportar CSV</button>
              </div>
              {loadingAttempts && <div className="text-slate-500 text-sm">Carregando tentativas...</div>}
              {!loadingAttempts && attempts.length === 0 && <div className="text-slate-500 text-sm">Nenhuma tentativa de teste registrada.</div>}
              <div className="space-y-4">
                {attemptsByTest.map(([title, list]) => (
                  <div key={title} className="rounded-2xl border border-slate-200 p-4">
                    <div className="font-semibold mb-2">{title}</div>
                    <div className="space-y-3">
                      {list.map(a => (
                        <div key={a.id} className="text-sm rounded-xl bg-slate-50 px-3 py-3">
                          <div className="flex flex-wrap items-center justify-between gap-2">
                            <div>
                              <span className="font-medium">{new Date(a.startedAt).toLocaleString('pt-BR')}</span>
                              <span className="text-slate-500"> · {a.status === 'COMPLETED' ? 'Concluído' : 'Em andamento'}</span>
                            </div>
                            <div className="text-brand-700 font-semibold">
                              {a.status === 'COMPLETED' ? <>Pontuação: {a.score}{a.maxScore ? `/${a.maxScore}` : ''}</> : '—'}
                            </div>
                          </div>
                          {a.resultSummary && <p className="text-slate-600 mt-2">{a.resultSummary}</p>}
                          {a.psychologistFeedback && (
                            <details className="mt-3 rounded-xl border border-brand-100 bg-brand-50/60 p-3" open>
                              <summary className="cursor-pointer font-semibold text-brand-900">Feedback para o psicólogo (respaldo do teste)</summary>
                              <p className="mt-2 text-slate-700 whitespace-pre-wrap leading-relaxed">{a.psychologistFeedback}</p>
                            </details>
                          )}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
              <p className="text-xs text-slate-500 mt-3">Resultados são instrumentos de apoio e devem ser interpretados por profissional qualificado; o feedback abaixo oferece respaldo técnico para a leitura clínica.</p>
            </div>

            <div className="space-y-4">
              <h3 className="text-lg font-bold px-1">Diário</h3>
              {loadingJournal && <div className="text-slate-500">Carregando diário...</div>}
              {!loadingJournal && journal.length === 0 && <div className="glass rounded-3xl p-8 text-center text-slate-500">Sem registros ainda.</div>}
              {journal.map(e => (
                <article key={e.id} className="glass rounded-3xl p-5">
                  <h3 className="font-semibold text-slate-900">{e.title || 'Registro do diário'}</h3>
                  <div className="text-xs text-slate-500 mt-1">{new Date(e.entryDate).toLocaleString('pt-BR')} · Humor: {e.mood || '—'}</div>
                  {e.content && <p className="text-slate-700 mt-3 whitespace-pre-wrap">{e.content}</p>}
                  {e.attachments && e.attachments.length > 0 && (
                    <div className="mt-3 space-y-2">
                      {e.attachments.map(a => (
                        <div key={a.id} className="rounded-xl border border-slate-200 p-3 bg-slate-50/60">
                          <div className="text-sm font-medium text-slate-700 mb-2">{a.type === 'AUDIO' ? '🎧 Áudio' : 'Anexo'} {a.originalName ? `- ${a.originalName}` : ''}</div>
                          {a.type === 'AUDIO' || (a.mimeType||'').startsWith('audio/') ? (
                            <audio controls src={withToken(a.url)} className="w-full" preload="none" />
                          ) : (
                            <a href={withToken(a.url)} className="text-brand-700 text-sm underline" target="_blank" rel="noreferrer">Abrir anexo</a>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                  <div className="mt-4 border-t border-slate-100 pt-3">
                    <div className="text-sm font-semibold mb-2">Feedback do profissional</div>
                    {(e.comments || []).map(c => (
                      <div key={c.id} className="mb-2 rounded-xl bg-brand-50 px-3 py-2">
                        <div className="text-xs text-slate-500">{c.authorName} · {new Date(c.createdAt).toLocaleString('pt-BR')}</div>
                        <div className="text-sm text-slate-800 whitespace-pre-wrap">{c.content}</div>
                      </div>
                    ))}
                    <div className="flex gap-2 mt-2">
                      <label htmlFor={`comment-${e.id}`} className="sr-only">Feedback</label>
                      <input id={`comment-${e.id}`} className="flex-1 rounded-xl border border-slate-200 px-3 py-2 text-sm" placeholder="Escreva um feedback..." value={commentText[e.id] || ''} onChange={ev=>setCommentText({...commentText, [e.id]: ev.target.value})} />
                      <button onClick={() => { const t = (commentText[e.id]||'').trim(); if(t){ addComment.mutate({entryId:e.id, content:t}); setCommentText({...commentText,[e.id]:''}) } }} className="rounded-xl bg-brand-600 text-white px-3 py-2 text-sm font-semibold">Enviar</button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </>
        )}
      </section>
    </div>
  )
}
