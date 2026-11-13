import { useState } from 'react'
import { api } from '../api.js'
import { Cabecalho, Conteudo, Modal, useDados } from '../componentes/ui.jsx'
import { dataHora, dinheiro, minutos, quantidade } from '../util.js'

function Barras({ titulo, valores, formatar }) {
  const entradas = Object.entries(valores || {})
  const maximo = Math.max(1, ...entradas.map(([, v]) => v))
  return (
    <div className="barras">
      <h4>{titulo}</h4>
      {entradas.length === 0 ? <p className="descricao">Sem dados.</p> : entradas.map(([nome, valor]) => (
        <div className="barra-linha" key={nome}>
          <span className="barra-nome">{nome}</span>
          <span className="barra-trilho"><span className="barra" style={{ width: `${(valor / maximo) * 100}%` }} /></span>
          <span className="barra-valor">{formatar(valor)}</span>
        </div>
      ))}
    </div>
  )
}

export function DetalheRelatorio({ relatorio: r }) {
  const desvioPositivo = r.desvio >= 0
  return (
    <div className="relatorio">
      <div className="indicadores pequenos">
        <div className="indicador"><span>Custo estimado</span><strong>{dinheiro(r.custoEstimado)}</strong></div>
        <div className="indicador"><span>Custo real</span><strong>{dinheiro(r.custoReal)}</strong></div>
        <div className={`indicador ${desvioPositivo ? 'positivo' : 'negativo'}`}>
          <span>Diferença</span><strong>{dinheiro(r.desvio)}</strong>
        </div>
        <div className="indicador"><span>Retrabalhos</span><strong>{r.retrabalhos}</strong></div>
      </div>
      <div className="grelha-barras">
        <Barras titulo="Tempo por etapa" valores={r.minutosPorEtapa} formatar={minutos} />
        <Barras titulo="Tempo por artesão" valores={r.minutosPorArtesao} formatar={minutos} />
        <Barras titulo="Materiais consumidos" valores={r.materiaisConsumidos} formatar={quantidade} />
        <Barras titulo="Horas extra por artesão" valores={r.minutosExtraPorArtesao} formatar={minutos} />
      </div>
    </div>
  )
}

export default function Relatorios() {
  const base = useDados(async () => {
    const [relatorios, analise] = await Promise.all([api.get('/relatorios'), api.get('/relatorios/analise')])
    return { relatorios, analise }
  })
  const [aberto, setAberto] = useState(null)
  const a = base.dados?.analise

  return (
    <>
      <Cabecalho
        titulo="Relatórios"
        descricao="Gerados automaticamente quando uma encomenda é concluída. Em baixo, a análise do histórico."
      />
      <Conteudo {...base}>
        {a && (
          <div className="indicadores">
            <div className="indicador"><span>Encomendas analisadas</span><strong>{a.totalRelatorios}</strong></div>
            <div className="indicador"><span>Custo real médio</span><strong>{dinheiro(a.custoMedioPorEncomenda)}</strong></div>
            <div className="indicador"><span>Diferença média do orçamento</span><strong>{dinheiro(a.desvioMedioOrcamento)}</strong></div>
            <div className="indicador"><span>Horas extra acumuladas</span><strong>{minutos(a.totalMinutosExtra)}</strong></div>
            <div className="indicador"><span>Retrabalhos</span><strong>{a.totalRetrabalhos}</strong></div>
          </div>
        )}

        <section className="cartao">
          {base.dados?.relatorios.length === 0 ? (
            <div className="vazio">Ainda não há relatórios. Conclua uma encomenda para gerar o primeiro.</div>
          ) : (
            <div className="tabela-envolvente">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>Encomenda</th><th>Cliente</th><th>Peça</th><th className="numero">Estimado</th>
                    <th className="numero">Real</th><th className="numero">Diferença</th><th>Gerado em</th><th className="accoes" />
                  </tr>
                </thead>
                <tbody>
                  {base.dados?.relatorios.map((r) => (
                    <tr key={r.id}>
                      <td className="mudo">#{r.encomendaId}</td>
                      <td><strong>{r.clienteNome}</strong></td>
                      <td>{r.pecaNome} × {r.quantidade}</td>
                      <td className="numero">{dinheiro(r.custoEstimado)}</td>
                      <td className="numero">{dinheiro(r.custoReal)}</td>
                      <td className={`numero ${r.desvio >= 0 ? 'texto-verde' : 'texto-vermelho'}`}>{dinheiro(r.desvio)}</td>
                      <td>{dataHora(r.dataGeracao)}</td>
                      <td className="accoes">
                        <button type="button" className="botao pequeno" onClick={() => setAberto(r)}>Ver</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </Conteudo>

      <Modal titulo={aberto ? `Relatório da encomenda #${aberto.encomendaId}` : ''} aberto={!!aberto}
        fechar={() => setAberto(null)} largo>
        {aberto && <DetalheRelatorio relatorio={aberto} />}
      </Modal>
    </>
  )
}
