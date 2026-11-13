import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { Cabecalho, Conteudo, Estado, useDados } from '../componentes/ui.jsx'
import { useSessao } from '../sessao.jsx'
import { ESTADOS, data, dinheiro, minutos } from '../util.js'

export default function Painel() {
  const { sessao } = useSessao()
  const base = useDados(async () => {
    const [resumo, encomendas] = await Promise.all([api.get('/dashboard'), api.get('/encomendas')])
    return { resumo, encomendas }
  })
  const r = base.dados?.resumo
  const totalEncomendas = r ? Object.values(r.encomendasPorEstado).reduce((a, b) => a + b, 0) : 0
  const activas = base.dados?.encomendas.filter((e) => e.estado === 'PENDENTE' || e.estado === 'EM_PRODUCAO') ?? []

  return (
    <>
      <Cabecalho titulo={`Olá, ${sessao?.utilizador?.nome ?? ''}`} descricao="Resumo da oficina." />
      <Conteudo {...base}>
        {r && (
          <>
            <div className="indicadores">
              <Link to="/encomendas" className="indicador ligado">
                <span>Encomendas</span><strong>{totalEncomendas}</strong>
              </Link>
              <Link to="/clientes" className="indicador ligado"><span>Clientes</span><strong>{r.totalClientes}</strong></Link>
              <Link to="/pecas" className="indicador ligado"><span>Peças no catálogo</span><strong>{r.totalPecas}</strong></Link>
              <Link to="/funcionarios" className="indicador ligado"><span>Artesãos</span><strong>{r.totalArtesaos}</strong></Link>
              <Link to="/relatorios" className="indicador ligado">
                <span>Custo real médio</span><strong>{dinheiro(r.analise.custoMedioPorEncomenda)}</strong>
              </Link>
            </div>

            <div className="duas-colunas">
              <section className="cartao">
                <h2>Encomendas por estado</h2>
                <ul className="lista-estados">
                  {Object.entries(r.encomendasPorEstado).map(([estado, n]) => (
                    <li key={estado}>
                      <Estado valor={estado} />
                      <span className="barra-trilho">
                        <span className={`barra barra-${estado.toLowerCase()}`}
                          style={{ width: `${totalEncomendas ? (n / totalEncomendas) * 100 : 0}%` }} />
                      </span>
                      <strong>{n}</strong>
                    </li>
                  ))}
                </ul>
                <p className="descricao">
                  Horas extra registadas em encomendas concluídas: {minutos(r.analise.totalMinutosExtra)}
                </p>
              </section>

              <section className="cartao">
                <div className="cartao-topo">
                  <h2>Em curso</h2>
                  <Link to="/encomendas" className="botao pequeno">Ver todas</Link>
                </div>
                {activas.length === 0 ? <div className="vazio">Nenhuma encomenda pendente ou em produção.</div> : (
                  <table className="tabela compacta">
                    <tbody>
                      {activas.slice(0, 6).map((e) => (
                        <tr key={e.id}>
                          <td className="mudo">#{e.id}</td>
                          <td><strong>{e.pecaNome}</strong> × {e.quantidade}<div className="pequeno-texto">{e.clienteNome}</div></td>
                          <td><Estado valor={e.estado} /></td>
                          <td className="pequeno-texto">{data(e.dataCriacao)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </section>
            </div>
            <p className="nota">
              Fluxo: registe a encomenda → confirme o pagamento ({ESTADOS.EM_PRODUCAO.toLowerCase()}) → registe a produção e o ponto →
              conclua a encomenda para gerar o relatório.
            </p>
          </>
        )}
      </Conteudo>
    </>
  )
}
