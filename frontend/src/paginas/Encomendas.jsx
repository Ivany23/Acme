import { useEffect, useState } from 'react'
import { api, textoErro } from '../api.js'
import { Cabecalho, Campo, Conteudo, Estado, Mensagem, Modal, useAccao, useDados } from '../componentes/ui.jsx'
import { ESTADOS, data, dataHora, dinheiro, hora, minutos, numeroOuNulo, quantidade } from '../util.js'
import { DetalheRelatorio } from './Relatorios.jsx'
import { usePode } from '../sessao.jsx'

function NovaEncomenda({ fechar, criada }) {
  const base = useDados(async () => {
    const [clientes, pecas] = await Promise.all([api.get('/clientes'), api.get('/pecas')])
    return { clientes, pecas }
  })
  const { executar, ocupado } = useAccao()
  const [form, setForm] = useState({ clienteId: '', pecaId: '', quantidade: 1, custosIndirectos: 0 })
  const [orcamento, setOrcamento] = useState(null)
  const [erroOrcamento, setErroOrcamento] = useState('')

  useEffect(() => {
    if (base.dados && !form.clienteId && !form.pecaId) {
      setForm((f) => ({ ...f, clienteId: base.dados.clientes[0]?.id ?? '', pecaId: base.dados.pecas[0]?.id ?? '' }))
    }
  }, [base.dados, form.clienteId, form.pecaId])

  // Pré-visualização do orçamento (RF008)
  useEffect(() => {
    const qtd = Number(form.quantidade)
    const ind = numeroOuNulo(form.custosIndirectos) ?? 0
    if (!form.pecaId || !(qtd > 0) || ind < 0) {
      setOrcamento(null)
      return undefined
    }
    const controlo = new AbortController()
    const t = setTimeout(() => {
      api.get(`/encomendas/orcamento?pecaId=${form.pecaId}&quantidade=${qtd}&custosIndirectos=${ind}`, { sinal: controlo.signal })
        .then((o) => { setOrcamento(o); setErroOrcamento('') })
        .catch((e) => { if (!controlo.signal.aborted) setErroOrcamento(textoErro(e)) })
    }, 300)
    return () => { clearTimeout(t); controlo.abort() }
  }, [form.pecaId, form.quantidade, form.custosIndirectos])

  const alterar = (campo, valor) => setForm((f) => ({ ...f, [campo]: valor }))

  async function submeter(e) {
    e.preventDefault()
    const r = await executar(() => api.post('/encomendas', {
      clienteId: Number(form.clienteId),
      pecaId: Number(form.pecaId),
      quantidade: Number(form.quantidade),
      custosIndirectos: numeroOuNulo(form.custosIndirectos) ?? 0,
    }), 'Encomenda registada')
    if (r.ok) criada()
  }

  return (
    <Modal titulo="Nova encomenda" aberto fechar={fechar} largo>
      <Conteudo {...base}>
        {base.dados && (base.dados.clientes.length === 0 || base.dados.pecas.length === 0) ? (
          <Mensagem tipo="aviso">É preciso ter pelo menos um cliente e uma peça registados.</Mensagem>
        ) : (
          <form onSubmit={submeter}>
            <div className="grelha-form">
              <Campo rotulo="Cliente" largura={2}>
                <select value={form.clienteId} onChange={(e) => alterar('clienteId', e.target.value)} required>
                  {base.dados?.clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
                </select>
              </Campo>
              <Campo rotulo="Peça" largura={2}>
                <select value={form.pecaId} onChange={(e) => alterar('pecaId', e.target.value)} required>
                  {base.dados?.pecas.map((p) => <option key={p.id} value={p.id}>{p.nome}</option>)}
                </select>
              </Campo>
              <Campo rotulo="Quantidade">
                <input type="number" min="1" step="1" value={form.quantidade}
                  onChange={(e) => alterar('quantidade', e.target.value)} required />
              </Campo>
              <Campo rotulo="Custos indirectos (MT)" ajuda="transporte, energia, etc.">
                <input type="number" min="0" step="any" value={form.custosIndirectos}
                  onChange={(e) => alterar('custosIndirectos', e.target.value)} />
              </Campo>
            </div>

            <div className="orcamento">
              <h3>Orçamento estimado</h3>
              {erroOrcamento && <Mensagem tipo="erro">{erroOrcamento}</Mensagem>}
              {orcamento ? (
                <table className="tabela-resumo">
                  <tbody>
                    <tr><td>Peças ({orcamento.quantidade} × preço unitário)</td><td>{dinheiro(orcamento.custoPecas)}</td></tr>
                    <tr><td>Materiais</td><td>{dinheiro(orcamento.custoMateriais)}</td></tr>
                    <tr><td>Custos indirectos</td><td>{dinheiro(orcamento.custosIndirectos)}</td></tr>
                    <tr><td>Subtotal</td><td>{dinheiro(orcamento.custoBase)}</td></tr>
                    <tr><td>Margem da organização ({orcamento.percentagemOrganizacao}%)</td>
                      <td>{dinheiro(orcamento.custoEstimado - orcamento.custoBase)}</td></tr>
                    <tr className="total"><td>Total</td><td>{dinheiro(orcamento.custoEstimado)}</td></tr>
                  </tbody>
                </table>
              ) : !erroOrcamento && <p className="descricao">Preencha a peça e a quantidade.</p>}
            </div>

            <div className="modal-rodape">
              <button type="button" className="botao" onClick={fechar}>Cancelar</button>
              <button type="submit" className="botao principal" disabled={ocupado}>
                {ocupado ? 'A registar…' : 'Registar encomenda'}
              </button>
            </div>
          </form>
        )}
      </Conteudo>
    </Modal>
  )
}

function DetalheEncomenda({ encomenda, fechar }) {
  const podeVerRelatorio = usePode()('relatorios.ver')
  const info = useDados(async () => {
    const registos = await api.get(`/producao?encomendaId=${encomenda.id}`)
    let relatorio = null
    if (encomenda.estado === 'CONCLUIDA' && podeVerRelatorio) {
      relatorio = await api.get(`/relatorios/encomenda/${encomenda.id}`).catch(() => null)
    }
    return { registos, relatorio }
  }, [encomenda.id, encomenda.estado, podeVerRelatorio])

  return (
    <Modal titulo={`Encomenda #${encomenda.id}`} aberto fechar={fechar} largo>
      <dl className="resumo-encomenda">
        <div><dt>Cliente</dt><dd>{encomenda.clienteNome}</dd></div>
        <div><dt>Peça</dt><dd>{encomenda.pecaNome} × {encomenda.quantidade}</dd></div>
        <div><dt>Estado</dt><dd><Estado valor={encomenda.estado} /></dd></div>
        <div><dt>Orçamento</dt><dd>{dinheiro(encomenda.custoEstimado)}</dd></div>
        <div><dt>Criada em</dt><dd>{dataHora(encomenda.dataCriacao)}</dd></div>
        <div><dt>Pagamento</dt><dd>{encomenda.paga ? 'Pago' : 'Por pagar'}</dd></div>
      </dl>

      <Conteudo {...info}>
        <h3 className="sub-titulo">Produção registada</h3>
        {info.dados?.registos.length === 0 ? <p className="descricao">Sem registos de produção.</p> : (
          <div className="tabela-envolvente">
            <table className="tabela compacta">
              <thead><tr><th>Data</th><th>Etapa</th><th>Artesão</th><th>Horário</th><th>Tempo</th><th className="numero">Custo</th></tr></thead>
              <tbody>
                {info.dados?.registos.map((r) => (
                  <tr key={r.id}>
                    <td>{data(r.data)}</td>
                    <td>{r.etapaDescricao}{r.retrabalho && <span className="etiqueta laranja">retrabalho</span>}</td>
                    <td>{r.artesaoNome}</td>
                    <td>{hora(r.horaInicio)}–{hora(r.horaFim)}</td>
                    <td>{minutos(r.tempoGastoMinutos)}</td>
                    <td className="numero">{dinheiro(r.custoMateriais + r.custosIndirectos)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {info.dados?.relatorio && (
          <>
            <h3 className="sub-titulo">Relatório de produção</h3>
            <DetalheRelatorio relatorio={info.dados.relatorio} />
          </>
        )}
      </Conteudo>
    </Modal>
  )
}

const FILTROS = ['', 'PENDENTE', 'EM_PRODUCAO', 'CONCLUIDA', 'CANCELADA']

export default function Encomendas() {
  const [filtro, setFiltro] = useState('')
  const lista = useDados(() => api.get(`/encomendas${filtro ? `?estado=${filtro}` : ''}`), [filtro])
  const { executar, ocupado } = useAccao()
  const [nova, setNova] = useState(false)
  const [detalhe, setDetalhe] = useState(null)
  const pode = usePode()

  async function accao(fn, mensagem) {
    const r = await executar(fn, mensagem)
    if (r.ok) lista.recarregar()
  }

  const pagar = (e) => accao(() => api.post(`/encomendas/${e.id}/pagamento`), `Pagamento da encomenda #${e.id} confirmado`)
  const mudarEstado = (e, estado, pergunta) => {
    if (pergunta && !window.confirm(pergunta)) return
    accao(() => api.patch(`/encomendas/${e.id}/estado`, { estado }), `Encomenda #${e.id}: ${ESTADOS[estado].toLowerCase()}`)
  }
  const apagar = (e) => {
    if (!window.confirm(`Apagar a encomenda #${e.id}?`)) return
    accao(() => api.apagar(`/encomendas/${e.id}`), 'Encomenda apagada')
  }

  return (
    <>
      <Cabecalho titulo="Encomendas" descricao="Do orçamento ao relatório final: pague para iniciar a produção e conclua quando terminar.">
        {pode('encomendas.criar') && (
          <button type="button" className="botao principal" onClick={() => setNova(true)}>Nova encomenda</button>
        )}
      </Cabecalho>

      <div className="separadores" role="tablist">
        {FILTROS.map((f) => (
          <button key={f || 'todas'} type="button" role="tab" aria-selected={filtro === f}
            className={filtro === f ? 'activo' : ''} onClick={() => setFiltro(f)}>
            {f ? ESTADOS[f] : 'Todas'}
          </button>
        ))}
      </div>

      <section className="cartao">
        <Conteudo {...lista} vazio={lista.dados?.length === 0} textoVazio="Nenhuma encomenda neste estado.">
          <div className="tabela-envolvente">
            <table className="tabela">
              <thead>
                <tr>
                  <th>#</th><th>Cliente</th><th>Peça</th><th className="numero">Qtd.</th><th>Estado</th>
                  <th className="numero">Orçamento</th><th>Data</th><th className="accoes">Acções</th>
                </tr>
              </thead>
              <tbody>
                {lista.dados?.map((e) => (
                  <tr key={e.id}>
                    <td className="mudo">{e.id}</td>
                    <td><strong>{e.clienteNome}</strong></td>
                    <td>{e.pecaNome}</td>
                    <td className="numero">{quantidade(e.quantidade)}</td>
                    <td><Estado valor={e.estado} /></td>
                    <td className="numero">{dinheiro(e.custoEstimado)}</td>
                    <td>{data(e.dataCriacao)}</td>
                    <td className="accoes">
                      <button type="button" className="botao pequeno" onClick={() => setDetalhe(e)}>Detalhes</button>
                      {e.estado === 'PENDENTE' && pode('encomendas.pagar') && (
                        <button type="button" className="botao pequeno sucesso" disabled={ocupado} onClick={() => pagar(e)}>Pagar</button>
                      )}
                      {e.estado === 'EM_PRODUCAO' && pode('encomendas.estado') && (
                        <button type="button" className="botao pequeno sucesso" disabled={ocupado}
                          onClick={() => mudarEstado(e, 'CONCLUIDA', `Concluir a encomenda #${e.id}? Será gerado o relatório de produção.`)}>
                          Concluir
                        </button>
                      )}
                      {(e.estado === 'PENDENTE' || e.estado === 'EM_PRODUCAO') && pode('encomendas.estado') && (
                        <button type="button" className="botao pequeno perigo" disabled={ocupado}
                          onClick={() => mudarEstado(e, 'CANCELADA', `Cancelar a encomenda #${e.id}?`)}>
                          Cancelar
                        </button>
                      )}
                      {e.estado === 'PENDENTE' && pode('encomendas.apagar') && (
                        <button type="button" className="botao pequeno ligacao" disabled={ocupado} onClick={() => apagar(e)}>Apagar</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Conteudo>
      </section>

      {nova && <NovaEncomenda fechar={() => setNova(false)} criada={() => { setNova(false); lista.recarregar() }} />}
      {detalhe && <DetalheEncomenda encomenda={detalhe} fechar={() => setDetalhe(null)} />}
    </>
  )
}
