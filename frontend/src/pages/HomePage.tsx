import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function HomePage() {
  const { user } = useAuth()
  const isPsy = user?.role === 'PSYCHOLOGIST'
  return (
    <div className="space-y-6">
      <section className="rounded-3xl bg-gradient-to-r from-brand-600 to-teal-500 text-white p-6 sm:p-8 shadow-soft">
        <h1 className="text-2xl sm:text-3xl font-bold mb-2">Olá, {user?.name?.split(' ')[0]} 👋</h1>
        <p className="text-brand-50 max-w-2xl">
          {isPsy
            ? 'Acompanhe seus pacientes vinculados, visualize registros do diário e ofereça feedback para apoiar o cuidado terapêutico.'
            : 'Este é seu espaço seguro para registrar sentimentos, acompanhar sua evolução e realizar testes quando indicado pelo seu psicólogo.'}
        </p>
      </section>
      <div className="grid md:grid-cols-3 gap-5">
        {isPsy ? (
          <>
            <Card title="Meus pacientes" desc="Veja pacientes vinculados, acesse diários e áudios, e acompanhe a evolução." to="/" cta="Abrir painel" color="bg-white" />
            <Card title="Testes disponíveis" desc="Consulte os instrumentos psicológicos e vocacionais disponíveis na plataforma." to="/testes" cta="Ver testes" color="bg-white" />
            <Card title="Minha conta" desc="Atualize seu perfil e altere sua senha com segurança." to="/conta" cta="Gerenciar conta" color="bg-brand-50" />
          </>
        ) : (
          <>
            <Card title="Diário de comportamento" desc="Anote, dite ou grave áudios sobre seu dia, emoções e acontecimentos importantes." to="/diario" cta="Abrir diário" color="bg-white" />
            <Card title="Testes e vocação" desc="Realize testes psicológicos e vocacionais, salve progresso e acompanhe resultados." to="/testes" cta="Ver testes" color="bg-white" />
            <Card title="Conta e segurança" desc="Atualize seu perfil e altere sua senha quando necessário." to="/conta" cta="Minha conta" color="bg-brand-50" />
          </>
        )}
      </div>
      <section className="glass rounded-3xl p-6">
        <h2 className="font-semibold text-lg mb-2">{isPsy ? 'Boas práticas' : 'Dica de bem-estar'}</h2>
        <p className="text-slate-600">
          {isPsy
            ? 'Combine com seus pacientes o uso do diário e dos testes como instrumentos de apoio. Feedbacks claros e respeitosos fortalecem o vínculo terapêutico.'
            : 'Reserve alguns minutos do seu dia para escrever no diário. Registrar pensamentos e comportamentos ajuda a identificar padrões e facilita o acompanhamento terapêutico.'}
        </p>
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
