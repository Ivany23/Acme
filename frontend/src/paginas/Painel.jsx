import { Link } from 'react-router-dom'
import { api } from '../api.js'
import { Cabecalho, Conteudo, Estado, useDados } from '../componentes/ui.jsx'
import { usePode, useSessao } from '../sessao.jsx'
import { PAPEIS } from '../permissoes.js'
import { ESTADOS, data, dinheiro, minutos } from '../util.js'

/** Indicador do painel; só é ligação se o perfil puder abrir a página. */
function Indicador({ para, accao, rotulo, valor }) {
  const pode = usePode()
  const conteudo = <><span>{rotulo}</span><strong>{valor}</strong></>
  return pode(accao)
    ? <Link to={para} className="indicador ligado">{conteudo}</Link>
    : <div className="indicador">{conteudo}</div>
}

export default function Painel() {
  const { sessao } = useSessao()
  const pode = usePode()
  const base = useDados(async () => {
    const [resumo, encomendas] = await Promise.all([api.get('/dashboard'), api.get('/encomendas')])
    return { resumo, encomendas }
  })
  const r = base.dados?.resumo
  const totalEncomendas = r ? Object.values(r.encomendasPorEstado).reduce((a, b) => a + b, 0) : 0
  const activas = base.dados?.encomendas.filter((e) => e.estado === 'PENDENTE' || e.estado === 'EM_PRODUCAO') ?? []

  return (
    <>
      <Cabecalho titulo={`Olá, ${sessao?.utilizador?.nome ?? ''}`}
        descricao={`Resumo da oficina · perfil: ${PAPEIS[sessao?.utilizador?.papel] ?? ''}`} />
      <Conteudo {...base}>
        {r && (
          <>
            <div className="indicadores">
              <Indicador para="/encomendas" accao="encomendas.ver" rotulo="Encomendas" valor={totalEncomendas} />
              <Indicador para="/clientes" accao="clientes.ver" rotulo="Clientes" valor={r.totalClientes} />
              <Indicador para="/pecas" accao="pecas.ver" rotulo="Peças no catálogo" valor={r.totalPecas} />
              <Indicador para="/funcionarios" accao="funcionarios.ver" rotulo="Artesãos" valor={r.totalArtesaos} />
              {pode('relatorios.ver') && (
                <Indicador para="/relatorios" accao="relatorios.ver" rotulo="Custo real médio"
                  valor={dinheiro(r.analise.custoMedioPorEncomenda)} />
              )}
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
                {pode('relatorios.ver') && (
                  <p className="descricao">
                    Horas extra registadas em encomendas concluídas: {minutos(r.analise.totalMinutosExtra)}
                  </p>
                )}
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
