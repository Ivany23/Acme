import { useEffect, useState } from 'react'
import { api, textoErro } from '../../api.js'
import { useSessao } from '../../sessao.jsx'
import { Campo, Conteudo, Estado, Mensagem, useAccao, useDados } from '../../componentes/ui.jsx'
import AlterarPassword from '../../componentes/AlterarPassword.jsx'
import { data, dinheiro } from '../../util.js'

const EXPLICACAO = {
  PENDENTE: 'A aguardar confirmação do pagamento pela Acme.',
  EM_PRODUCAO: 'Pagamento confirmado. A peça está a ser fabricada.',
  CONCLUIDA: 'Produção concluída. Pode levantar a encomenda.',
  CANCELADA: 'Encomenda cancelada.',
}

/** Caso de uso "Solicitar Orçamento de Encomenda" (RF008). */
function PedirOrcamento({ encomendado }) {
  const pecas = useDados(() => api.get('/portal/pecas'))
  const { executar, ocupado } = useAccao()
  const [pecaId, setPecaId] = useState('')
  const [quantidade, setQuantidade] = useState(1)
  const [orcamento, setOrcamento] = useState(null)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!pecaId && pecas.dados?.length) setPecaId(pecas.dados[0].id)
  }, [pecas.dados, pecaId])

  useEffect(() => {
    const qtd = Number(quantidade)
    if (!pecaId || !(qtd > 0) || !Number.isInteger(qtd)) {
      setOrcamento(null)
      return undefined
    }
    const controlo = new AbortController()
    const t = setTimeout(() => {
      api.get(`/portal/orcamento?pecaId=${pecaId}&quantidade=${qtd}`, { sinal: controlo.signal })
        .then((o) => { setOrcamento(o); setErro('') })
        .catch((e) => { if (!controlo.signal.aborted) setErro(textoErro(e)) })
    }, 300)
    return () => { clearTimeout(t); controlo.abort() }
  }, [pecaId, quantidade])

  const peca = pecas.dados?.find((p) => String(p.id) === String(pecaId))

  async function encomendar(e) {
    e.preventDefault()
    if (!window.confirm(`Confirmar a encomenda de ${quantidade} × ${peca?.nome} por ${dinheiro(orcamento?.custoEstimado)}?`)) return
    const r = await executar(() => api.post('/portal/encomendas', { pecaId: Number(pecaId), quantidade: Number(quantidade) }),
      'Encomenda registada. A Acme vai confirmar o pagamento.')
    if (r.ok) encomendado()
  }

  return (
    <section className="cartao">
      <h2>Pedir orçamento</h2>
      <Conteudo {...pecas} vazio={pecas.dados?.length === 0} textoVazio="De momento não há peças no catálogo.">
        <form onSubmit={encomendar}>
          <div className="grelha-form">
            <Campo rotulo="Peça">
              <select value={pecaId} onChange={(e) => setPecaId(e.target.value)} required>
                {pecas.dados?.map((p) => (
                  <option key={p.id} value={p.id}>{p.nome}{p.dimensoes ? ` (${p.dimensoes})` : ''}</option>
                ))}
              </select>
            </Campo>
            <Campo rotulo="Quantidade">
              <input type="number" min="1" step="1" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} required />
            </Campo>
          </div>

          {erro && <Mensagem tipo="erro">{erro}</Mensagem>}
          {orcamento && (
            <div className="orcamento">
              <table className="tabela-resumo">
                <tbody>
                  <tr><td>Preço ({orcamento.quantidade} × {dinheiro(peca?.precoUnitario)})</td><td>{dinheiro(orcamento.custoPecas)}</td></tr>
                  <tr><td>Materiais</td><td>{dinheiro(orcamento.custoMateriais)}</td></tr>
                  <tr><td>Taxa da organização ({orcamento.percentagemOrganizacao}%)</td>
                    <td>{dinheiro(orcamento.custoEstimado - orcamento.custoBase)}</td></tr>
                  <tr className="total"><td>Total estimado</td><td>{dinheiro(orcamento.custoEstimado)}</td></tr>
                </tbody>
              </table>
            </div>
          )}
          <div className="modal-rodape">
            <button type="submit" className="botao principal" disabled={ocupado || !orcamento}>
              {ocupado ? 'A enviar…' : 'Fazer encomenda'}
            </button>
          </div>
        </form>
      </Conteudo>
    </section>
  )
}

/** Caso de uso "Acompanhar Estado da Encomenda" (padrão Observer). */
export default function PortalCliente() {
  const { sessao, sair } = useSessao()
  const [password, setPassword] = useState(false)
  const { executar, ocupado } = useAccao()
  const base = useDados(async () => {
    const [encomendas, notificacoes] = await Promise.all([api.get('/portal/encomendas'), api.get('/portal/notificacoes')])
    return { encomendas, notificacoes }
  })

  async function cancelar(e) {
    if (!window.confirm(`Cancelar a encomenda #${e.id}?`)) return
    const r = await executar(() => api.post(`/portal/encomendas/${e.id}/cancelar`), 'Encomenda cancelada')
    if (r.ok) base.recarregar()
  }

  return (
    <div className="portal">
      <header className="portal-topo">
        <div className="marca-portal">
          <img src="/marca.svg" alt="" width="32" height="32" />
          <strong>Acme</strong>
          <span>Área de cliente</span>
        </div>
        <div className="portal-utilizador">
          <span>{sessao?.utilizador?.nome}</span>
          <button type="button" className="botao pequeno" onClick={() => setPassword(true)}>Password</button>
          <button type="button" className="botao pequeno" onClick={sair}>Sair</button>
        </div>
      </header>

      <main className="conteudo">
        <div className="cabecalho">
          <div>
            <h1>Olá, {sessao?.utilizador?.nome}</h1>
            <p className="descricao">Peça um orçamento, encomende e acompanhe aqui o estado das suas encomendas.</p>
          </div>
        </div>

        <div className="duas-colunas">
          <PedirOrcamento encomendado={base.recarregar} />

          <section className="cartao">
            <h2>Notificações</h2>
            <Conteudo {...base} vazio={base.dados?.notificacoes.length === 0}
              textoVazio="Ainda não recebeu notificações. Será avisado sempre que uma encomenda mudar de estado.">
              <ol className="lista-notificacoes">
                {base.dados?.notificacoes.slice().reverse().map((n, i) => <li key={i}>{n}</li>)}
              </ol>
            </Conteudo>
          </section>
        </div>

        <section className="cartao">
          <h2>As minhas encomendas</h2>
          <Conteudo {...base} vazio={base.dados?.encomendas.length === 0} textoVazio="Ainda não fez nenhuma encomenda.">
            <div className="tabela-envolvente">
              <table className="tabela">
                <thead>
                  <tr><th>#</th><th>Peça</th><th className="numero">Qtd.</th><th className="numero">Valor</th>
                    <th>Data</th><th>Estado</th><th className="accoes" /></tr>
                </thead>
                <tbody>
                  {base.dados?.encomendas.map((e) => (
                    <tr key={e.id}>
                      <td className="mudo">{e.id}</td>
                      <td><strong>{e.pecaNome}</strong></td>
                      <td className="numero">{e.quantidade}</td>
                      <td className="numero">{dinheiro(e.custoEstimado)}</td>
                      <td>{data(e.dataCriacao)}</td>
                      <td><Estado valor={e.estado} /><div className="pequeno-texto">{EXPLICACAO[e.estado]}</div></td>
                      <td className="accoes">
                        {e.estado === 'PENDENTE' && (
                          <button type="button" className="botao pequeno perigo" disabled={ocupado} onClick={() => cancelar(e)}>
                            Cancelar
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </Conteudo>
        </section>
      </main>
      {password && <AlterarPassword fechar={() => setPassword(false)} />}
    </div>
  )
}
