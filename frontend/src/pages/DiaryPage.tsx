import { useEffect, useMemo, useRef, useState } from 'react'
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
  const [recording, setRecording] = useState(false)
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null)
  const [audioUrl, setAudioUrl] = useState<string | null>(null)
  const [filterMood, setFilterMood] = useState('Todos')
  const [search, setSearch] = useState('')
  const recognitionRef = useRef<any>(null)
  const mediaRecorderRef = useRef<MediaRecorder | null>(null)
  const chunksRef = useRef<BlobPart[]>([])
  const token = localStorage.getItem('token')

  const create = useMutation({
    mutationFn: async () => {
      const text = content.trim() || (audioBlob ? 'Registro em áudio' : '')
      const entry = (await api.post('/journal', { title, content: text, mood })).data as JournalEntry
      if (audioBlob) {
        const form = new FormData()
        form.append('file', audioBlob, `audio-${Date.now()}.webm`)
        await api.post(`/journal/${entry.id}/attachments`, form, { headers: { 'Content-Type': 'multipart/form-data' } })
      }
      return entry
    },
    onSuccess: () => {
      setTitle(''); setContent(''); setMood('Neutro'); clearAudio(); qc.invalidateQueries({queryKey:['journal']})
    }
  })

  const remove = useMutation({
    mutationFn: async (id: string) => api.delete(`/journal/${id}`),
    onSuccess: () => qc.invalidateQueries({queryKey:['journal']})
  })

  const removeAttachment = useMutation({
    mutationFn: async (id: string) => api.delete(`/journal/attachments/${id}`),
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
    return () => { if (audioUrl) URL.revokeObjectURL(audioUrl) }
  }, [])

  const toggleDictation = () => {
    const rec = recognitionRef.current
    if (!rec) { alert('Ditado por voz não suportado neste navegador. Você pode gravar um áudio ou digitar sua anotação.'); return }
    if (listening) { rec.stop(); setListening(false) } else { setListening(true); rec.start() }
  }

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const mr = new MediaRecorder(stream)
      chunksRef.current = []
      mr.ondataavailable = (e) => { if (e.data.size > 0) chunksRef.current.push(e.data) }
      mr.onstop = () => {
        stream.getTracks().forEach(t => t.stop())
        const blob = new Blob(chunksRef.current, { type: mr.mimeType || 'audio/webm' })
        if (audioUrl) URL.revokeObjectURL(audioUrl)
        setAudioBlob(blob)
        setAudioUrl(URL.createObjectURL(blob))
        setRecording(false)
      }
      mediaRecorderRef.current = mr
      mr.start()
      setRecording(true)
    } catch {
      alert('Não foi possível acessar o microfone. Verifique as permissões do navegador.')
    }
  }

  const stopRecording = () => {
    mediaRecorderRef.current?.stop()
  }

  const clearAudio = () => {
    if (audioUrl) URL.revokeObjectURL(audioUrl)
    setAudioBlob(null); setAudioUrl(null)
  }

  const filtered = useMemo(() => {
    return entries.filter(e => {
      const moodOk = filterMood === 'Todos' || (e.mood || '') === filterMood
      const q = search.toLowerCase().trim()
      const textOk = !q || (e.title||'').toLowerCase().includes(q) || (e.content||'').toLowerCase().includes(q)
      return moodOk && textOk
    })
  }, [entries, filterMood, search])

  const grouped = useMemo(() => {
    const map = new Map<string, JournalEntry[]>()
    for (const e of filtered) {
      const d = new Date(e.entryDate)
      const key = d.toLocaleDateString('pt-BR', { year: 'numeric', month: 'long' })
      if (!map.has(key)) map.set(key, [])
      map.get(key)!.push(e)
    }
    return Array.from(map.entries())
  }, [filtered])

  const withToken = (url: string) => token ? `${url}${url.includes('?') ? '&' : '?'}token=${encodeURIComponent(token)}` : url
  const canSave = content.trim().length > 0 || !!audioBlob

  return (
    <div className="grid lg:grid-cols-5 gap-6">
      <section className="lg:col-span-2 glass rounded-3xl p-6 h-fit">
        <h2 className="text-xl font-bold mb-1">Nova anotação</h2>
        <p className="text-slate-500 text-sm mb-4">Escreva, dite ou grave um áudio para registrar como você está.</p>
        <input className="w-full rounded-xl border border-slate-200 px-4 py-3 mb-3" placeholder="Título (opcional)" value={title} onChange={e=>setTitle(e.target.value)} />
        <div className="flex flex-wrap gap-2 mb-3">
          <select className="rounded-xl border border-slate-200 px-3 py-2 text-sm" value={mood} onChange={e=>setMood(e.target.value)}>
            {['Muito bem','Bem','Neutro','Ansioso(a)','Triste','Irritado(a)'].map(m => <option key={m}>{m}</option>)}
          </select>
          <button type="button" onClick={toggleDictation} className={`rounded-xl px-3 py-2 text-sm font-semibold border ${listening ? 'bg-red-50 border-red-200 text-red-700' : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'}`}>
            {listening ? '⏹ Parar ditado' : '📝 Ditar texto'}
          </button>
          {!recording ? (
            <button type="button" onClick={startRecording} className="rounded-xl px-3 py-2 text-sm font-semibold border bg-white border-slate-200 text-slate-700 hover:bg-slate-50">🎙 Gravar áudio</button>
          ) : (
            <button type="button" onClick={stopRecording} className="rounded-xl px-3 py-2 text-sm font-semibold border bg-red-50 border-red-200 text-red-700">⏹ Parar gravação</button>
          )}
        </div>
        <textarea className="w-full rounded-xl border border-slate-200 px-4 py-3 min-h-[160px] focus:outline-none focus:ring-2 focus:ring-brand-300" placeholder="Descreva seus pensamentos, comportamentos ou como foi seu dia..." value={content} onChange={e=>setContent(e.target.value)} />
        {audioUrl && (
          <div className="mt-3 rounded-xl border border-brand-100 bg-brand-50 p-3">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-brand-800">Áudio gravado (será anexado ao salvar)</span>
              <button type="button" onClick={clearAudio} className="text-xs text-red-600 hover:underline">Remover</button>
            </div>
            <audio controls src={audioUrl} className="w-full" />
          </div>
        )}
        <button onClick={() => canSave && create.mutate()} disabled={!canSave || create.isPending} className="w-full mt-4 rounded-xl bg-brand-600 hover:bg-brand-700 text-white font-semibold py-3 disabled:opacity-60">{create.isPending ? 'Salvando...' : 'Salvar no diário'}</button>
        <p className="text-xs text-slate-500 mt-3">Você pode salvar texto, áudio ou ambos. O ditado e a gravação dependem do suporte do navegador.</p>
      </section>

      <section className="lg:col-span-3 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-3">
          <h2 className="text-xl font-bold">Meu histórico</h2>
          <div className="flex flex-wrap gap-2">
            <input className="rounded-xl border border-slate-200 px-3 py-2 text-sm" placeholder="Buscar..." value={search} onChange={e=>setSearch(e.target.value)} />
            <select className="rounded-xl border border-slate-200 px-3 py-2 text-sm" value={filterMood} onChange={e=>setFilterMood(e.target.value)}>
              {['Todos','Muito bem','Bem','Neutro','Ansioso(a)','Triste','Irritado(a)'].map(m => <option key={m}>{m}</option>)}
            </select>
          </div>
        </div>
        {isLoading && <div className="text-slate-500">Carregando...</div>}
        {!isLoading && filtered.length === 0 && <div className="glass rounded-3xl p-8 text-center text-slate-500">Nenhum registro encontrado.</div>}
        {grouped.map(([month, items]) => (
          <div key={month} className="space-y-3">
            <h3 className="text-sm font-semibold text-slate-500 uppercase tracking-wide">{month}</h3>
            {items.map(e => (
              <article key={e.id} className="glass rounded-3xl p-5">
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <h3 className="font-semibold text-slate-900">{e.title || 'Registro do diário'}</h3>
                    <div className="text-xs text-slate-500 mt-1">{new Date(e.entryDate).toLocaleString('pt-BR')} · Humor: {e.mood || '—'}</div>
                  </div>
                  <button onClick={() => remove.mutate(e.id)} className="text-xs text-red-600 hover:underline">Excluir</button>
                </div>
                {e.content && <p className="text-slate-700 mt-3 whitespace-pre-wrap">{e.content}</p>}
                {e.attachments && e.attachments.length > 0 && (
                  <div className="mt-4 space-y-2">
                    {e.attachments.map(a => (
                      <div key={a.id} className="rounded-xl border border-slate-200 p-3 bg-slate-50/60">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium text-slate-700">{a.type === 'AUDIO' ? '🎧 Áudio' : 'Anexo'} {a.originalName ? `- ${a.originalName}` : ''}</span>
                          <button onClick={() => removeAttachment.mutate(a.id)} className="text-xs text-red-600 hover:underline">Remover</button>
                        </div>
                        {a.type === 'AUDIO' || (a.mimeType||'').startsWith('audio/') ? (
                          <audio controls src={withToken(a.url)} className="w-full" preload="none" />
                        ) : (
                          <a href={withToken(a.url)} className="text-brand-700 text-sm underline" target="_blank" rel="noreferrer">Abrir anexo</a>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </article>
            ))}
          </div>
        ))}
      </section>
    </div>
  )
}
