import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import type { ChatMessageView, ChatReply, ChatStatus } from '../types'

const SESSION_KEY = 'psicare-chat-session'
const SUGGESTIONS = ['Como agendo uma consulta?', 'Estou me sentindo ansioso', 'Dúvida sobre medicação']

export default function ChatWidget() {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [messages, setMessages] = useState<ChatMessageView[]>([])
  const [sessionId, setSessionId] = useState<string | undefined>(() => sessionStorage.getItem(SESSION_KEY) || undefined)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)

  const status = useQuery({ queryKey: ['chat-status'], queryFn: async () => (await api.get<ChatStatus>('/chat/status')).data, staleTime: 60_000 })

  useEffect(() => {
    if (!open || !sessionId || messages.length) return
    api.get<ChatMessageView[]>('/chat/history', { params: { sessionId } })
      .then(r => setMessages(r.data))
      .catch(() => setMessages([]))
  }, [open, sessionId, messages.length])

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages, open])

  const send = async (text: string) => {
    const content = text.trim()
    if (!content || loading) return
    setError('')
    setLoading(true)
    try {
      const res = await api.post<ChatReply>('/chat/messages', { content, sessionId })
      setSessionId(res.data.sessionId)
      sessionStorage.setItem(SESSION_KEY, res.data.sessionId)
      setMessages(prev => [...prev, res.data.userMessage, res.data.botMessage])
      setInput('')
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Não foi possível enviar a mensagem. Tente novamente.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="fixed safe-bottom z-30 flex flex-col items-end gap-2">
      {open && (
        <section className="w-full max-w-[380px] max-h-[min(78vh,640px)] rounded-3xl border border-slate-200 bg-white shadow-xl overflow-hidden flex flex-col">
          <div className="bg-brand-700 text-white px-4 py-3 flex items-start justify-between gap-3">
            <div>
              <h2 className="font-semibold text-sm">Orientador automático</h2>
              <p className="text-xs text-brand-100 mt-0.5">Ajuda fora do horário de atendimento</p>
            </div>
            <button onClick={() => setOpen(false)} aria-label="Fechar conversa" className="rounded-lg px-2 py-1 text-white/80 hover:bg-white/10">✕</button>
          </div>

          <div className="px-4 py-3 bg-amber-50 border-b border-amber-100 text-xs text-amber-900">
            {status.data?.message || 'Carregando status do atendimento...'}
            {status.data?.consentRequired && (
              <span className="block mt-1">
                Para conversar, aceite a política em <Link to="/privacidade" className="underline font-semibold">Privacidade</Link>.
              </span>
            )}
          </div>

          <div className="h-[min(48vh,300px)] sm:h-72 overflow-y-auto px-4 py-3 space-y-3 bg-slate-50/60">
            {messages.length === 0 && (
              <p className="text-sm text-slate-500">
                Escreva sua dúvida. Este orientador dá respostas automáticas e não substitui a conversa com seu psicólogo.
              </p>
            )}
            {messages.map(m => (
              <div key={m.id} className={`flex ${m.sender === 'USER' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[85%] rounded-2xl px-3 py-2 text-sm whitespace-pre-wrap ${m.sender === 'USER' ? 'bg-brand-600 text-white' : m.crisis ? 'bg-red-50 text-red-900 border border-red-200' : 'bg-white border border-slate-200 text-slate-800'}`}>
                  {m.content}
                </div>
              </div>
            ))}
            <div ref={bottomRef} />
          </div>

          {messages.length === 0 && (
            <div className="px-4 pt-2 flex flex-wrap gap-2">
              {SUGGESTIONS.map(s => (
                <button key={s} onClick={() => setInput(s)} className="text-xs rounded-full border border-slate-200 bg-white px-2.5 py-1 text-slate-600 hover:bg-slate-100">{s}</button>
              ))}
            </div>
          )}

          {error && <p className="px-4 pt-2 text-xs text-red-600">{error}</p>}

          <form className="p-3 flex gap-2 border-t border-slate-100" onSubmit={e => { e.preventDefault(); void send(input) }}>
            <label htmlFor="chat-input" className="sr-only">Sua mensagem</label>
            <input id="chat-input" value={input} onChange={e => setInput(e.target.value)}
              disabled={status.data?.consentRequired || loading}
              placeholder="Escreva sua mensagem..." maxLength={2000}
              className="flex-1 rounded-xl border border-slate-200 px-3 py-2.5 text-sm disabled:opacity-60 min-h-[44px]" />
            <button type="submit" disabled={!input.trim() || status.data?.consentRequired || loading}
              className="rounded-xl bg-brand-600 text-white px-4 py-2.5 text-sm font-semibold disabled:opacity-50 min-h-[44px]">
              {loading ? '...' : 'Enviar'}
            </button>
          </form>

          <p className="px-4 pb-3 text-[11px] text-slate-500">
            Resposta automática, não substitui atendimento profissional. Em crise: <strong>188 (CVV)</strong>, <strong>192 (SAMU)</strong> ou <strong>190</strong>.
          </p>
        </section>
      )}

      <button onClick={() => setOpen(v => !v)}
        className="rounded-full bg-brand-600 text-white px-4 py-3 text-sm font-semibold shadow-lg hover:bg-brand-700">
        {open ? 'Fechar ajuda' : '💬 Preciso de ajuda'}
      </button>
    </div>
  )
}
