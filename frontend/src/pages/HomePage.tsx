import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function HomePage() {
  const { user } = useAuth()
  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-gradient-to-r from-brand-600 to-teal-500 text-white p-8 shadow-soft">
        <h1 className="text-3xl font-bold mb-2">Olá, {user?.name?.split(' ')[0]} 👋</h1>
        <p className="text-brand-50 max-w-2xl">Este é seu espaço seguro para registrar sentimentos, acompanhar sua evolução e realizar testes quando indicado pelo seu psicólogo.</p>
      </section>
      <div className="grid md:grid-cols-3 gap-5">
        <Card title="Diário de comportamento" desc="Anote ou fale sobre seu dia, emoções e acontecimentos importantes." to="/diario" cta="Abrir diário" color="bg-white" />
        <Card title="Testes e vocação" desc="Acesse testes psicológicos e vocacionais disponibilizados para você." to="/testes" cta="Ver testes" color="bg-white" />
        <Card title="Acompanhamento" desc="Seu psicólogo pode acompanhar seus registros vinculados para apoiar seu cuidado." to="/diario" cta="Ver registros" color="bg-brand-50" />
      </div>
      <section className="glass rounded-3xl p-6">
        <h2 className="font-semibold text-lg mb-2">Dica de bem-estar</h2>
        <p className="text-slate-600">Reserve alguns minutos do seu dia para escrever no diário. Registrar pensamentos e comportamentos ajuda a identificar padrões e facilita o acompanhamento terapêutico.</p>
      </section>
    </div>
  )
}

function Card({title, desc, to, cta, color}:{title:string;desc:string;to:string;cta:string;color:string}) {
  return (
    <div className={`${color} rounded-3xl p-6 border border-slate-100 shadow-sm hover:shadow-soft transition`}>
      <h3 className="font-semibold text-lg text-slate-900 mb-2">{title}</h3>
      <p className="text-slate-600 mb-4">{desc}</p>
      <Link to={to} className="inline-flex rounded-xl bg-brand-600 text-white px-4 py-2 text-sm font-semibold hover:bg-brand-700">{cta}</Link>
    </div>
  )
}
