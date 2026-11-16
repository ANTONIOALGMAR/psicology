import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Attempt, JournalEntry, PatientSummary } from '../types'

export default function PsychologistPage() {
  const qc = useQueryClient()
  const { data: patients = [], isLoading } = useQuery({ queryKey: ['psy-patients'], queryFn: async () => (await api.get<PatientSummary[]>('/psychologist/patients')).data })
  const [selected, setSelected] = useState<PatientSummary | null>(null)
  const [email, setEmail] = useState('')
  const [commentText, setCommentText] = useState<Record<string, string>>({})
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
      </section>
      <section className="lg:col-span-3 space-y-4">
        {!selected && <div className="glass rounded-3xl p-8 text-center text-slate-500">Selecione um paciente para visualizar diário, evolução em testes e registrar feedback.</div>}
        {selected && (
          <>
            <div className="rounded-3xl bg-white border border-slate-100 p-5">
              <h2 className="text-xl font-bold">Acompanhamento de {selected.name}</h2>
              <p className="text-slate-500 text-sm">{selected.email}</p>
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
                    <div className="space-y-2">
                      {list.map(a => (
                        <div key={a.id} className="flex flex-wrap items-center justify-between gap-2 text-sm rounded-xl bg-slate-50 px-3 py-2">
                          <div>
                            <span className="font-medium">{new Date(a.startedAt).toLocaleString('pt-BR')}</span>
                            <span className="text-slate-500"> · {a.status === 'COMPLETED' ? 'Concluído' : 'Em andamento'}</span>
                          </div>
                          <div className="text-slate-700">
                            {a.status === 'COMPLETED' ? <><span className="font-semibold">Pontuação: {a.score}</span>{a.resultSummary ? <span className="text-slate-500"> — {a.resultSummary}</span> : null}</> : '—'}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
              <p className="text-xs text-slate-500 mt-3">Resultados são instrumentos de apoio e devem ser interpretados por profissional qualificado.</p>
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
