import { useState } from 'react'
import { api } from '../api.js'
import { Cabecalho, Campo, Conteudo, Modal, useDados } from '../componentes/ui.jsx'
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

const SEM_FILTRO = { inicio: '', fim: '', pecaId: '', artesao: '' }

function parametros(filtro) {
  const p = new URLSearchParams()
  Object.entries(filtro).forEach(([k, v]) => { if (v !== '' && v != null) p.set(k, v) })
  const q = p.toString()
  return q ? `?${q}` : ''
}

/** Caso de uso "Definir Critérios de Análise" (AnalisadorDados). */
function Criterios({ aplicado, aplicar }) {
  const opcoes = useDados(async () => {
    const [pecas, artesaos] = await Promise.all([api.get('/pecas'), api.get('/funcionarios/artesaos')])
    return { pecas, artesaos }
  })
  const [form, setForm] = useState(aplicado)
  const alterar = (c, v) => setForm((f) => ({ ...f, [c]: v }))

  function submeter(e) {
    e.preventDefault()
    aplicar(form)
  }

  return (
    <form className="cartao filtros" onSubmit={submeter}>
      <h2>Critérios de análise</h2>
      <div className="filtros-campos">
        <Campo rotulo="De">
          <input type="date" value={form.inicio} onChange={(e) => alterar('inicio', e.target.value)} />
        </Campo>
        <Campo rotulo="Até">
          <input type="date" value={form.fim} min={form.inicio || undefined} onChange={(e) => alterar('fim', e.target.value)} />
        </Campo>
        <Campo rotulo="Peça">
          <select value={form.pecaId} onChange={(e) => alterar('pecaId', e.target.value)}>
            <option value="">Todas</option>
            {opcoes.dados?.pecas.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
          </select>
        </Campo>
        <Campo rotulo="Artesão">
          <select value={form.artesao} onChange={(e) => alterar('artesao', e.target.value)}>
            <option value="">Todos</option>
            {opcoes.dados?.artesaos.map((a) => <option key={a.id} value={a.nome}>{a.nome}</option>)}
          </select>
        </Campo>
        <div className="filtros-botoes">
          <button type="submit" className="botao principal">Analisar</button>
          <button type="button" className="botao" onClick={() => { setForm(SEM_FILTRO); aplicar(SEM_FILTRO) }}>Limpar</button>
        </div>
      </div>
    </form>
  )
}

export default function Relatorios() {
  const [filtro, setFiltro] = useState(SEM_FILTRO)
  const q = parametros(filtro)
  const base = useDados(async () => {
    const [relatorios, analise] = await Promise.all([api.get(`/relatorios${q}`), api.get(`/relatorios/analise${q}`)])
    return { relatorios, analise }
  }, [q])
  const [aberto, setAberto] = useState(null)
  const a = base.dados?.analise

  return (
    <>
      <Cabecalho
        titulo="Relatórios e análise"
        descricao="Os relatórios são gerados automaticamente quando uma encomenda é concluída. Defina critérios para minerar o histórico."
      />
      <Criterios aplicado={filtro} aplicar={setFiltro} />
      <Conteudo {...base}>
        {a && (
          <>
            <div className="indicadores">
              <div className="indicador"><span>Encomendas analisadas</span><strong>{a.totalRelatorios}</strong></div>
              <div className="indicador"><span>Custo real médio</span><strong>{dinheiro(a.custoMedioPorEncomenda)}</strong></div>
              <div className="indicador"><span>Diferença média do orçamento</span><strong>{dinheiro(a.desvioMedioOrcamento)}</strong></div>
              <div className="indicador"><span>Horas extra acumuladas</span><strong>{minutos(a.totalMinutosExtra)}</strong></div>
              <div className="indicador"><span>Retrabalhos</span><strong>{a.totalRetrabalhos}</strong></div>
            </div>
            {a.totalRelatorios > 0 && (
              <section className="cartao">
                <div className="grelha-barras tres">
                  <Barras titulo="Custo real médio por peça" valores={a.custoMedioPorPeca} formatar={dinheiro} />
                  <Barras titulo="Tempo de produção por artesão" valores={a.minutosPorArtesao} formatar={minutos} />
                  <Barras titulo="Horas extra a pagar por artesão" valores={a.minutosExtraPorArtesao} formatar={minutos} />
                </div>
              </section>
            )}
          </>
        )}

        <section className="cartao">
          {base.dados?.relatorios.length === 0 ? (
            <div className="vazio">
              {q ? 'Nenhum relatório corresponde aos critérios.' : 'Ainda não há relatórios. Conclua uma encomenda para gerar o primeiro.'}
            </div>
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
