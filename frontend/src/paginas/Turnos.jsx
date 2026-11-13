import { useState } from 'react'
import CrudSimples from '../componentes/CrudSimples.jsx'
import { Conteudo, useAccao, useAvisos, useDados } from '../componentes/ui.jsx'
import { api } from '../api.js'
import { data as formatarData, hoje, hora, minutos } from '../util.js'

function Alocacao() {
  const [dia, setDia] = useState(hoje())
  const { executar, ocupado } = useAccao()
  const { avisar } = useAvisos()
  const base = useDados(async () => {
    const [artesaos, turnos, alocacoes] = await Promise.all([
      api.get('/funcionarios/artesaos'),
      api.get('/turnos'),
      api.get(`/alocacoes?data=${dia}`),
    ])
    return { artesaos, turnos, alocacoes }
  }, [dia])

  const alocacaoDe = (artesaoId) => base.dados?.alocacoes.find((a) => a.artesaoId === artesaoId)

  async function automatica() {
    const r = await executar(() => api.post('/alocacoes/automatica', { data: dia }))
    if (r.ok) {
      base.recarregar()
      const n = r.resultado.length
      avisar(n === 0 ? 'Todos os artesãos já tinham turno neste dia' : `${n} artesão(s) alocado(s)`)
    }
  }

  async function mudar(artesaoId, turnoId) {
    const actual = alocacaoDe(artesaoId)
    const r = turnoId
      ? await executar(() => api.post('/alocacoes', { artesaoId, turnoId: Number(turnoId), data: dia }), 'Turno alterado')
      : await executar(() => api.apagar(`/alocacoes/${actual.id}`), 'Alocação removida')
    if (r.ok) base.recarregar()
  }

  return (
    <section className="cartao">
      <div className="cartao-topo">
        <div>
          <h2>Alocação de artesãos</h2>
          <p className="descricao">Cada artesão trabalha no máximo num turno por dia.</p>
        </div>
        <div className="linha-accoes">
          <input type="date" value={dia} onChange={(e) => setDia(e.target.value)} aria-label="Dia" />
          <button type="button" className="botao principal" onClick={automatica} disabled={ocupado}>
            Alocar automaticamente
          </button>
        </div>
      </div>

      <Conteudo {...base} vazio={base.dados?.artesaos.length === 0} textoVazio="Registe artesãos em Funcionários.">
        <div className="tabela-envolvente">
          <table className="tabela">
            <thead>
              <tr><th>Artesão</th><th>Especialidade</th><th>Turno em {formatarData(dia)}</th></tr>
            </thead>
            <tbody>
              {base.dados?.artesaos.map((a) => {
                const actual = alocacaoDe(a.id)
                return (
                  <tr key={a.id}>
                    <td><strong>{a.nome}</strong></td>
                    <td>{a.especialidade || '—'}</td>
                    <td>
                      <select
                        value={actual?.turnoId ?? ''}
                        onChange={(e) => mudar(a.id, e.target.value)}
                        disabled={ocupado}
                        aria-label={`Turno de ${a.nome}`}
                        className={actual ? '' : 'sem-valor'}
                      >
                        <option value="">Sem turno</option>
                        {base.dados.turnos.map((t) => (
                          <option key={t.id} value={t.id}>
                            {t.denominacao} ({hora(t.horaEntrada)}–{hora(t.horaSaida)})
                          </option>
                        ))}
                      </select>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </Conteudo>
    </section>
  )
}

export default function Turnos() {
  return (
    <>
      <CrudSimples
        titulo="Turnos e alocação"
        descricao="Horários de trabalho e distribuição diária dos artesãos."
        recurso="turnos"
        nomeSingular="Turno"
        vazio="Nenhum turno registado."
        campos={[
          { nome: 'denominacao', rotulo: 'Denominação', obrigatorio: true, largura: 2, ajuda: 'ex.: Matutino' },
          { nome: 'horaEntrada', rotulo: 'Hora de entrada', tipo: 'time', obrigatorio: true },
          { nome: 'horaSaida', rotulo: 'Hora de saída', tipo: 'time', obrigatorio: true },
        ]}
        paraFormulario={(t) => ({ ...t, horaEntrada: hora(t.horaEntrada), horaSaida: hora(t.horaSaida) })}
        colunas={[
          { rotulo: 'Turno', valor: (t) => <strong>{t.denominacao}</strong> },
          { rotulo: 'Entrada', valor: (t) => hora(t.horaEntrada) },
          { rotulo: 'Saída', valor: (t) => hora(t.horaSaida) },
          { rotulo: 'Duração', valor: (t) => minutos(t.duracaoMinutos) },
        ]}
      />
      <Alocacao />
    </>
  )
}
