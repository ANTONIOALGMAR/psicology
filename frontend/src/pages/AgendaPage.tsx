import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import type { Appointment, PatientSummary, PsychologistDirectoryEntry } from '../types'

const statusLabel: Record<string, string> = {
  SCHEDULED: 'Agendado', CONFIRMED: 'Confirmado', COMPLETED: 'Concluído', CANCELLED: 'Cancelado',
}
const toLocalInput = (value: Date) => {
  const offset = value.getTimezoneOffset() * 60000
  return new Date(value.getTime() - offset).toISOString().slice(0, 16)
}

export default function AgendaPage() {
  const { user } = useAuth()
  const qc = useQueryClient()
  const isPsychologist = user?.role === 'PSYCHOLOGIST'
  const isPatient = user?.role === 'PATIENT'

  const [patientId, setPatientId] = useState('')
  const [psychologistId, setPsychologistId] = useState('')
  const [startsAt, setStartsAt] = useState('')
  const [duration, setDuration] = useState(50)
  const [mode, setMode] = useState('ONLINE')
  const [title, setTitle] = useState('Atendimento')
  const [notes, setNotes] = useState('')
  const [filterPatient, setFilterPatient] = useState('')
  const [msg, setMsg] = useState('')
  const [err, setErr] = useState('')

  const appointments = useQuery({
    queryKey: ['appointments'],
    queryFn: async () => (await api.get<Appointment[]>('/appointments')).data,
  })
  const patients = useQuery({
    queryKey: ['psy-patients'],
    enabled: isPsychologist,
    queryFn: async () => (await api.get<PatientSummary[]>('/psychologist/patients')).data,
  })
  const myPsychologist = useQuery({
    queryKey: ['my-psychologist'],
    enabled: isPatient,
    queryFn: async () => (await api.get<PsychologistDirectoryEntry>('/appointments/my-psychologist')).data,
  })

  const refresh = () => qc.invalidateQueries({ queryKey: ['appointments'] })

  const create = useMutation({
    mutationFn: async () => {
      const start = new Date(startsAt)
      const payload: any = {
        startsAt: start.toISOString(),
        endsAt: new Date(start.getTime() + duration * 60000).toISOString(),
        mode, title, notes: notes || undefined,
      }
      if (isPsychologist) payload.patientId = patientId
      if (!isPsychologist) payload.psychologistId = psychologistId || myPsychologist.data?.id
      await api.post('/appointments', payload)
    },
    onSuccess: () => {
      setMsg('Agendamento criado.'); setErr(''); setStartsAt(''); setNotes('')
      refresh()
    },
    onError: (e: any) => { setErr(e?.response?.data?.message || 'Não foi possível agendar.'); setMsg('') },
  })

  const cancel = useMutation({
    mutationFn: async (id: string) => { await api.post(`/appointments/${id}/cancel`) },
    onSuccess: () => { setMsg('Agendamento cancelado.'); refresh() },
    onError: (e: any) => { setErr(e?.response?.data?.message || 'Não foi possível cancelar.') },
  })

  const complete = useMutation({
    mutationFn: async (a: Appointment) => {
      await api.put(`/appointments/${a.id}`, {
        patientId: a.patientId, startsAt: a.startsAt, endsAt: a.endsAt,
        mode: a.mode, status: 'COMPLETED', title: a.title, notes: a.notes,
      })
    },
    onSuccess: () => { setMsg('Atendimento concluído.'); refresh() },
    onError: (e: any) => { setErr(e?.response?.data?.message || 'Não foi possível concluir.') },
  })

  const list = (appointments.data || []).filter(a => !filterPatient || a.patientId === filterPatient)

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Agenda de atendimentos</h1>
        <p className="text-slate-600">
          {isPsychologist
            ? 'Você visualiza apenas os agendamentos dos pacientes vinculados a você.'
            : isPatient
              ? 'Seus agendamentos com o profissional vinculado.'
              : 'Visão consolidada dos agendamentos.'}
        </p>
      </div>
      {msg && <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-xl px-3 py-2">{msg}</div>}
      {err && <div className="text-sm text-red-700 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{err}</div>}

      <section className="glass rounded-3xl p-6">
        <h2 className="text-lg font-semibold mb-4">Novo agendamento</h2>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {isPsychologist && (
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Paciente</label>
              <select className="w-full rounded-xl border border-slate-200 px-3 py-2.5" value={patientId} onChange={e => setPatientId(e.target.value)}>
                <option value="">Selecione...</option>
                {patients.data?.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
              </select>
            </div>
          )}
          {isPatient && (
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1">Profissional</label>
              <input className="w-full rounded-xl border border-slate-200 px-3 py-2.5 bg-slate-50" readOnly value={myPsychologist.data?.name || 'Sem profissional vinculado'} />
            </div>
          )}
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Data e hora</label>
            <input type="datetime-local" className="w-full rounded-xl border border-slate-200 px-3 py-2.5" value={startsAt} min={toLocalInput(new Date())} onChange={e => setStartsAt(e.target.value)} />
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Duração (minutos)</label>
            <select className="w-full rounded-xl border border-slate-200 px-3 py-2.5" value={duration} onChange={e => setDuration(Number(e.target.value))}>
              <option value={30}>30</option><option value={50}>50</option><option value={60}>60</option><option value={90}>90</option>
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700 mb-1">Modalidade</label>
            <select className="w-full rounded-xl border border-slate-200 px-3 py-2.5" value={mode} onChange={e => setMode(e.target.value)}>
              <option value="ONLINE">Online</option><option value="PRESENTIAL">Presencial</option>
            </select>
          </div>
          <div className="sm:col-span-2 lg:col-span-3">
            <label className="block text-sm font-medium text-slate-700 mb-1">Título / Observações</label>
            <div className="flex gap-3">
              <input className="flex-1 rounded-xl border border-slate-200 px-3 py-2.5" value={title} onChange={e => setTitle(e.target.value)} />
              <input className="flex-1 rounded-xl border border-slate-200 px-3 py-2.5" placeholder="Observações (opcional)" value={notes} onChange={e => setNotes(e.target.value)} />
            </div>
          </div>
        </div>
        <button onClick={() => create.mutate()} disabled={create.isPending || !startsAt || (isPsychologist && !patientId)}
          className="mt-4 rounded-xl bg-brand-600 text-white px-5 py-2.5 font-semibold disabled:opacity-60">
          {create.isPending ? 'Agendando...' : 'Agendar'}
        </button>
        {isPatient && !myPsychologist.data && <p className="text-xs text-slate-500 mt-2">Você precisa estar vinculado a um profissional para solicitar um atendimento.</p>}
      </section>

      <section className="glass rounded-3xl p-6">
        <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
          <h2 className="text-lg font-semibold">Próximos atendimentos</h2>
          {isPsychologist && (
            <select className="rounded-xl border border-slate-200 px-3 py-2 text-sm" value={filterPatient} onChange={e => setFilterPatient(e.target.value)}>
              <option value="">Todos os pacientes</option>
              {patients.data?.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
            </select>
          )}
        </div>
        {appointments.isLoading && <p className="text-sm text-slate-500">Carregando...</p>}
        {!appointments.isLoading && list.length === 0 && <p className="text-sm text-slate-500">Nenhum agendamento encontrado.</p>}
        <div className="space-y-3">
          {list.map(a => (
            <div key={a.id} className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200 p-4">
              <div>
                <div className="font-semibold">{a.title || 'Atendimento'}</div>
                <div className="text-sm text-slate-600">{new Date(a.startsAt).toLocaleString('pt-BR')} — {new Date(a.endsAt).toLocaleTimeString('pt-BR')}</div>
                <div className="text-xs text-slate-500">{isPatient ? a.psychologistName : a.patientName} · {a.mode === 'ONLINE' ? 'Online' : 'Presencial'}</div>
                {a.notes && <div className="text-sm text-slate-600 mt-1">{a.notes}</div>}
              </div>
              <div className="flex items-center gap-2">
                <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${a.status === 'CANCELLED' ? 'bg-slate-200 text-slate-600' : a.status === 'COMPLETED' ? 'bg-blue-100 text-blue-700' : 'bg-green-100 text-green-700'}`}>
                  {statusLabel[a.status] || a.status}
                </span>
                {!isPatient && (a.status === 'SCHEDULED' || a.status === 'CONFIRMED') && (
                  <button onClick={() => complete.mutate(a)} className="text-xs rounded-lg border border-slate-200 px-2 py-1 hover:bg-slate-50">Concluir</button>
                )}
                {a.status !== 'CANCELLED' && a.status !== 'COMPLETED' && (
                  <button onClick={() => cancel.mutate(a.id)} className="text-xs rounded-lg border border-red-200 text-red-600 px-2 py-1 hover:bg-red-50">Cancelar</button>
                )}
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
