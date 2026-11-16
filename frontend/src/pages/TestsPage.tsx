export default function TestsPage() {
  const tests = [
    { id: '1', type: 'Vocacional', title: 'Teste Vocacional Básico', desc: 'Explore interesses e aptidões para apoiar escolhas profissionais.', color: 'from-amber-400 to-orange-500' },
    { id: '2', type: 'Psicológico', title: 'Autoavaliação de Bem-estar', desc: 'Questionário breve para acompanhar seu bem-estar emocional ao longo do tempo.', color: 'from-brand-500 to-teal-500' },
  ]
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold">Testes psicológicos e vocacionais</h1>
        <p className="text-slate-600">Canal de aplicação de testes. Novos instrumentos podem ser disponibilizados pelo seu psicólogo.</p>
      </div>
      <div className="grid md:grid-cols-2 gap-5">
        {tests.map(t => (
          <div key={t.id} className="glass rounded-3xl overflow-hidden">
            <div className={`h-2 bg-gradient-to-r ${t.color}`} />
            <div className="p-6">
              <span className="inline-flex text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700 mb-3">{t.type}</span>
              <h3 className="text-lg font-semibold mb-2">{t.title}</h3>
              <p className="text-slate-600 mb-4">{t.desc}</p>
              <button className="rounded-xl bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 font-semibold text-sm">Iniciar teste</button>
            </div>
          </div>
        ))}
      </div>
      <div className="rounded-3xl border border-amber-200 bg-amber-50 p-5 text-amber-900 text-sm">
        <strong>Importante:</strong> os resultados de testes psicológicos são instrumentos de apoio e devem ser interpretados por um profissional qualificado no contexto do acompanhamento.
      </div>
    </div>
  )
}
