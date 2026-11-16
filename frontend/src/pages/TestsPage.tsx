import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Attempt, TestDetail, TestSummary } from '../types'

export default function TestsPage() {
  const qc = useQueryClient()
  const { data: tests = [], isLoading } = useQuery({ queryKey: ['tests'], queryFn: async () => (await api.get<TestSummary[]>('/tests')).data })
  const { data: attempts = [] } = useQuery({ queryKey: ['attempts-me'], queryFn: async () => (await api.get<Attempt[]>('/attempts/me')).data })
  const [active, setActive] = useState<{ detail: TestDetail, attempt: Attempt } | null>(null)
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  const start = async (testId: string) => {
    const [detailRes, attemptRes] = await Promise.all([
      api.get<TestDetail>(`/tests/${testId}`),
      api.post<Attempt>(`/tests/${testId}/attempts`)
    ])
    const map: Record<string,string> = {}
    for (const a of attemptRes.data.answers || []) if (a.optionId) map[a.questionId] = a.optionId
    setAnswers(map)
    setActive({ detail: detailRes.data, attempt: attemptRes.data })
  }

  const setAnswer = (qid: string, optionId: string) => setAnswers(prev => ({ ...prev, [qid]: optionId }))

  const saveProgress = async (submit: boolean) => {
    if (!active) return
    setSubmitting(true)
    try {
      const payload = Object.entries(answers).map(([questionId, optionId]) => ({ questionId, optionId }))
      const url = submit ? `/attempts/${active.attempt.id}/submit` : `/attempts/${active.attempt.id}/answers`
      const res = await api.post<Attempt>(url, payload)
      setActive({ ...active, attempt: res.data })
      qc.invalidateQueries({ queryKey: ['attempts-me'] })
      if (submit) { /* stay to show result */ }
    } finally { setSubmitting(false) }
  }

  const inProgressByTest = useMemo(() => {
    const m = new Map<string, Attempt>()
    for (const a of attempts) if (a.status === 'IN_PROGRESS') m.set(a.testId, a)
    return m
  }, [attempts])

  if (active) {
    const done = active.attempt.status === 'COMPLETED'
    return (
      <div className="space-y-5">
        <button onClick={() => { setActive(null); setAnswers({}) }} className="text-sm text-brand-700 hover:underline">← Voltar para testes</button>
        <div className="glass rounded-3xl p-6">
          <div className="flex items-start justify-between gap-4">
            <div>
              <span className="inline-flex text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 mb-2">{active.detail.type === 'VOCATIONAL' ? 'Vocacional' : 'Psicológico'}</span>
              <h1 className="text-2xl font-bold">{active.detail.title}</h1>
              <p className="text-slate-600 mt-1">{active.detail.description}</p>
            </div>
            {done && <span className="rounded-full bg-green-100 text-green-700 px-3 py-1 text-sm font-semibold">Concluído</span>}
          </div>
        </div>

        {done ? (
          <div className="glass rounded-3xl p-6">
            <h2 className="text-xl font-bold mb-2">Resultado</h2>
            <div className="text-3xl font-bold text-brand-700 mb-2">Pontuação: {active.attempt.score}</div>
            <p className="text-slate-700 whitespace-pre-wrap">{active.attempt.resultSummary}</p>
            <p className="text-xs text-slate-500 mt-4">Resultados são instrumentos de apoio e devem ser interpretados por um profissional qualificado.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {active.detail.questions.map((q, idx) => (
              <div key={q.id} className="glass rounded-3xl p-5">
                <div className="font-medium mb-3">{idx + 1}. {q.prompt}</div>
                <div className="grid sm:grid-cols-2 gap-2">
                  {q.options.map(o => (
                    <label key={o.id} className={`flex items-center gap-2 rounded-xl border px-3 py-2 cursor-pointer ${answers[q.id]===o.id ? 'border-brand-400 bg-brand-50' : 'border-slate-200 hover:bg-slate-50'}`}>
                      <input type="radio" name={q.id} checked={answers[q.id]===o.id} onChange={() => setAnswer(q.id, o.id)} />
                      <span className="text-sm">{o.label}</span>
                    </label>
                  ))}
                </div>
              </div>
            ))}
            <div className="flex gap-3">
              <button onClick={() => saveProgress(false)} disabled={submitting} className="rounded-xl border border-slate-300 bg-white px-5 py-3 font-semibold disabled:opacity-60">Salvar progresso</button>
              <button onClick={() => saveProgress(true)} disabled={submitting} className="rounded-xl bg-brand-600 hover:bg-brand-700 text-white px-5 py-3 font-semibold disabled:opacity-60">{submitting ? 'Enviando...' : 'Finalizar teste'}</button>
            </div>
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Testes psicológicos e vocacionais</h1>
        <p className="text-slate-600">Responda aos instrumentos disponíveis, salve seu progresso e acompanhe seus resultados.</p>
      </div>

      <div className="grid md:grid-cols-2 gap-5">
        {isLoading && <div className="text-slate-500">Carregando testes...</div>}
        {tests.map(t => {
          const prog = inProgressByTest.get(t.id)
          return (
            <div key={t.id} className="glass rounded-3xl overflow-hidden">
              <div className={`h-2 bg-gradient-to-r ${t.type === 'VOCATIONAL' ? 'from-amber-400 to-orange-500' : 'from-brand-500 to-teal-500'}`} />
              <div className="p-6">
                <span className="inline-flex text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 mb-3">{t.type === 'VOCATIONAL' ? 'Vocacional' : 'Psicológico'}</span>
                <h3 className="text-lg font-semibold mb-2">{t.title}</h3>
                <p className="text-slate-600 mb-2">{t.description}</p>
                <p className="text-xs text-slate-500 mb-4">{t.questionCount} questões</p>
                <button onClick={() => start(t.id)} className="rounded-xl bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 font-semibold text-sm">{prog ? 'Continuar teste' : 'Iniciar teste'}</button>
              </div>
            </div>
          )
        })}
      </div>

      <div className="glass rounded-3xl p-6">
        <h2 className="text-lg font-bold mb-3">Histórico de tentativas</h2>
        {attempts.length === 0 && <div className="text-slate-500 text-sm">Nenhuma tentativa ainda.</div>}
        <div className="space-y-2">
          {attempts.map(a => (
            <div key={a.id} className="flex items-center justify-between rounded-xl border border-slate-200 px-4 py-3">
              <div>
                <div className="font-medium">{a.testTitle}</div>
                <div className="text-xs text-slate-500">{new Date(a.startedAt).toLocaleString('pt-BR')} · {a.status === 'COMPLETED' ? `Concluído (pontuação ${a.score})` : 'Em andamento'}</div>
              </div>
              <button onClick={() => start(a.testId)} className="text-sm font-semibold text-brand-700 hover:underline">{a.status === 'COMPLETED' ? 'Ver/Refazer' : 'Continuar'}</button>
            </div>
          ))}
        </div>
      </div>

      <div className="rounded-3xl border border-amber-200 bg-amber-50 p-5 text-amber-900 text-sm">
        <strong>Importante:</strong> os resultados de testes psicológicos são instrumentos de apoio e devem ser interpretados por um profissional qualificado no contexto do acompanhamento.
      </div>
    </div>
  )
}
