import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { Availability, AvailabilityRequest } from '../types'

const ZONES = ['America/Sao_Paulo', 'America/Recife', 'America/Manaus', 'UTC']

export default function AvailabilityPage() {
  const qc = useQueryClient()
  const [form, setForm] = useState<AvailabilityRequest>({
    timezone: 'America/Sao_Paulo', startTime: '09:00', endTime: '18:00', weekendEnabled: false,
  })
  const [notice, setNotice] = useState('')

  const availability = useQuery({
    queryKey: ['availability'],
    queryFn: async () => (await api.get<Availability>('/psychologist/availability')).data,
  })

  useEffect(() => {
    if (!availability.data) return
    setForm({
      timezone: availability.data.timezone,
      startTime: availability.data.startTime.slice(0, 5),
      endTime: availability.data.endTime.slice(0, 5),
      weekendEnabled: availability.data.weekendEnabled,
    })
  }, [availability.data])

  const save = useMutation({
    mutationFn: async () => (await api.put<Availability>('/psychologist/availability', form)).data,
    onSuccess: () => {
      setNotice('Horário salvo. O orientador automático vai seguir esta configuração.')
      qc.invalidateQueries({ queryKey: ['availability'] })
      qc.invalidateQueries({ queryKey: ['chat-status'] })
    },
    onError: (e: any) => setNotice(e?.response?.data?.message || 'Não foi possível salvar o horário.'),
  })

  return (
    <div className="max-w-3xl mx-auto space-y-5">
      <div>
        <h1 className="text-2xl font-bold">Minha disponibilidade</h1>
        <p className="text-slate-600">
          Defina o horário em que você costuma atender. Fora dele, o orientador automático responde aos pacientes com dicas gerais e encaminha casos de crise para canais de emergência.
        </p>
      </div>

      <section className="glass rounded-3xl p-5 sm:p-6 space-y-5">
        <p className="text-sm text-slate-600 rounded-xl bg-brand-50 border border-brand-100 px-4 py-3">
          O PsiCare usa esse horário como referência. Ele não bloqueia sua agenda nem substitui seus compromissos — serve apenas para saber quando deve aparecer a mensagem automática.
        </p>

        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="av-zone" className="block text-sm font-medium mb-1">Fuso horário</label>
            <select id="av-zone" className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white"
              value={form.timezone} onChange={e => setForm({ ...form, timezone: e.target.value })}>
              {ZONES.map(z => <option key={z} value={z}>{z}</option>)}
              {!ZONES.includes(form.timezone) && <option value={form.timezone}>{form.timezone}</option>}
            </select>
          </div>
          <div className="flex items-end">
            <label className="flex items-center gap-3 text-sm text-slate-700 cursor-pointer rounded-xl border border-slate-200 px-3 py-2.5 w-full">
              <input type="checkbox" className="h-5 w-5 accent-brand-600" checked={form.weekendEnabled}
                onChange={e => setForm({ ...form, weekendEnabled: e.target.checked })} />
              Atender também nos fins de semana
            </label>
          </div>
          <div>
            <label htmlFor="av-start" className="block text-sm font-medium mb-1">Início</label>
            <input id="av-start" type="time" className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm"
              value={form.startTime} onChange={e => setForm({ ...form, startTime: e.target.value })} />
          </div>
          <div>
            <label htmlFor="av-end" className="block text-sm font-medium mb-1">Fim</label>
            <input id="av-end" type="time" className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm"
              value={form.endTime} onChange={e => setForm({ ...form, endTime: e.target.value })} />
          </div>
        </div>

        {notice && <div className={`text-sm rounded-xl px-3 py-2 ${notice.startsWith('Horário salvo') ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'}`}>{notice}</div>}

        <div className="flex flex-wrap gap-3">
          <button onClick={() => save.mutate()} disabled={save.isPending}
            className="rounded-xl bg-brand-600 text-white px-5 py-3 text-sm font-semibold disabled:opacity-60 min-h-[44px]">
            {save.isPending ? 'Salvando...' : 'Salvar horário'}
          </button>
          <button onClick={() => qc.invalidateQueries({ queryKey: ['availability'] })}
            className="rounded-xl border border-slate-200 px-5 py-3 text-sm font-semibold min-h-[44px]">
            Recarregar
          </button>
        </div>
      </section>

      <section className="glass rounded-3xl p-5 sm:p-6">
        <h2 className="font-semibold mb-2">Como funciona na prática</h2>
        <ul className="list-disc pl-5 space-y-1 text-sm text-slate-600">
          <li>Dentro do horário: o paciente é avisado de que você está disponível e pode usar a Agenda.</li>
          <li>Fora do horário: o orientador automático responde com orientações gerais e registra a conversa para auditoria.</li>
          <li>Em crise: o sistema orienta ligar 188 (CVV), 192 (SAMU) ou 190, e gera um alerta para você sem mostrar o conteúdo da mensagem no alerta.</li>
        </ul>
      </section>
    </div>
  )
}
