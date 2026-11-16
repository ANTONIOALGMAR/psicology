import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { JournalEntry } from '../types'

export default function DiaryPage() {
  const qc = useQueryClient()
  const { data: entries = [], isLoading } = useQuery({ queryKey: ['journal'], queryFn: async () => (await api.get<JournalEntry[]>('/journal')).data })
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [mood, setMood] = useState('Neutro')
  const [listening, setListening] = useState(false)
  const recognitionRef = useRef<any>(null)

  const create = useMutation({
    mutationFn: async () => (await api.post('/journal', { title, content, mood })).data,
    onSuccess: () => { setTitle(''); setContent(''); setMood('Neutro'); qc.invalidateQueries({queryKey:['journal']}) }
  })

  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/journal/${id}`),
    onSuccess: () => qc.invalidateQueries({queryKey:['journal']})
  })

  useEffect(() => {
    const SR = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition
    if (SR) {
      const rec = new SR(); rec.lang = 'pt-BR'; rec.continuous = true; rec.interimResults = true
      rec.onresult = (e: any) => {
        let final = ''
        for (let i = e.resultIndex; i < e.results.length; i++) {
          if (e.results[i].isFinal) final += e.results[i][0].transcript + ' '
        }
        if (final) setContent(c => (c + ' ' + final).trim())
      }
      rec.onend = () => setListening(false)
      recognitionRef.current = rec
    }
  }, [])

  const toggleVoice = () => {
    const rec = recognitionRef.current
    if (!rec) { alert('Ditado por voz não suportado neste navegador. Você pode digitar sua anotação.'); return }
    if (listening) { rec.stop(); setListening(false) } else { setListening(true); rec.start() }
  }

  return (
    <div className="grid lg:grid-cols-5 gap-6">
      <section className="lg:col-span-2 glass rounded-3xl p-6 h-fit">
        <h2 className="text-xl font-bold mb-1">Nova anotação</h2>
        <p className="text-slate-500 text-sm mb-4">Escreva ou use o ditado por voz para registrar como você está.</p>
        <input className="w-full rounded-xl border border-slate-200 px-4 py-3 mb-3" placeholder="Título (opcional)" value={title} onChange={e=>setTitle(e.target.value)} />
        <div className="flex gap-2 mb-3">
          <select className="rounded-xl border border-slate-200 px-3 py-2 text-sm" value={mood} onChange={e=>setMood(e.target.value)}>
            {['Muito bem','Bem','Neutro','Ansioso(a)','Triste','Irritado(a)'].map(m => <option key={m}>{m}</option>)}
          </select>
          <button type="button" onClick={toggleVoice} className={`rounded-xl px-4 py-2 text-sm font-semibold border ${listening ? 'bg-red-50 border-red-200 text-red-700' : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'}`}>
            {listening ? '⏹ Parar ditado' : '🎙 Falar / ditar'}
          </button>
        </div>
        <textarea className="w-full rounded-xl border border-slate-200 px-4 py-3 min-h-[180px] focus:outline-none focus:ring-2 focus:ring-brand-300" placeholder="Descreva seus pensamentos, comportamentos ou como foi seu dia..." value={content} onChange={e=>setContent(e.target.value)} />
        <button onClick={() => content.trim() && create.mutate()} disabled={!content.trim() || create.isPending} className="w-full mt-4 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold py-3 disabled:opacity-60">{create.isPending ? 'Salvando...' : 'Salvar no diário'}</button>
        <p className="text-xs text-slate-500 mt-3">O ditado usa o reconhecimento de voz do navegador (quando disponível). Seus registros ficam disponíveis para acompanhamento conforme vinculação com seu psicólogo.</p>
      </section>
      <section className="lg:col-span-3 space-y-4">
        <h2 className="text-xl font-bold">Meu histórico</h2>
        {isLoading && <div className="text-slate-500">Carregando...</div>}
        {!isLoading && entries.length === 0 && <div className="glass rounded-3xl p-8 text-center text-slate-500">Nenhum registro ainda. Comece escrevendo como você se sente hoje.</div>}
        {entries.map(e => (
          <article key={e.id} className="glass rounded-3xl p-5">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h3 className="font-semibold text-slate-900">{e.title || 'Registro do diário'}</h3>
                <div className="text-xs text-slate-500 mt-1">{new Date(e.entryDate).toLocaleString('pt-BR')} · Humor: {e.mood || '—'}</div>
              </div>
              <button onClick={() => remove.mutate(e.id)} className="text-xs text-red-600 hover:underline">Excluir</button>
            </div>
            <p className="text-slate-700 mt-3 whitespace-pre-wrap">{e.content}</p>
          </article>
        ))}
      </section>
    </div>
  )
}
