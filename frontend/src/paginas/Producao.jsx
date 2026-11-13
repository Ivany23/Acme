import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Cabecalho, Campo, Conteudo, Mensagem, Modal, useAccao, useDados } from '../componentes/ui.jsx'
import { data, dinheiro, hoje, hora, minutos, numeroOuNulo, quantidade } from '../util.js'

function NovoRegisto({ fechar, criado }) {
  const base = useDados(async () => {
    const [encomendas, artesaos, turnos, materiais] = await Promise.all([
      api.get('/encomendas?estado=EM_PRODUCAO'),
      api.get('/funcionarios/artesaos'),
      api.get('/turnos'),
      api.get('/materiais'),
    ])
    return { encomendas, artesaos, turnos, materiais }
  })
  const { executar, ocupado } = useAccao()
  const [form, setForm] = useState({
    encomendaId: '', etapaId: '', artesaoId: '', turnoId: '', data: hoje(),
    horaInicio: '', horaFim: '', custosIndirectos: 0, retrabalho: false, materiais: [],
  })
  const [etapas, setEtapas] = useState([])
  const alterar = (campo, valor) => setForm((f) => ({ ...f, [campo]: valor }))

  // valores iniciais quando os dados chegam
  useEffect(() => {
    if (!base.dados) return
    setForm((f) => ({
      ...f,
      encomendaId: f.encomendaId || base.dados.encomendas[0]?.id || '',
      artesaoId: f.artesaoId || base.dados.artesaos[0]?.id || '',
      turnoId: f.turnoId || base.dados.turnos[0]?.id || '',
    }))
  }, [base.dados])

  // etapas e materiais previstos da peça da encomenda escolhida
  useEffect(() => {
    const enc = base.dados?.encomendas.find((e) => String(e.id) === String(form.encomendaId))
    if (!enc) { setEtapas([]); return }
    let activo = true
    api.get(`/pecas/${enc.pecaId}`).then((peca) => {
      if (!activo) return
      setEtapas(peca.etapas)
      setForm((f) => ({
        ...f,
        etapaId: peca.etapas[0]?.id ?? '',
        materiais: peca.materiais.map((m) => ({ materialId: m.materialId, quantidade: '' })),
      }))
    }).catch(() => activo && setEtapas([]))
    return () => { activo = false }
  }, [form.encomendaId, base.dados])

  // sugere o turno alocado ao artesão nesse dia
  useEffect(() => {
    if (!form.artesaoId || !form.data) return
    let activo = true
    api.get(`/alocacoes?data=${form.data}`).then((lista) => {
      const a = lista.find((x) => String(x.artesaoId) === String(form.artesaoId))
      if (activo && a) setForm((f) => ({ ...f, turnoId: a.turnoId }))
    }).catch(() => {})
    return () => { activo = false }
  }, [form.artesaoId, form.data])

  const alterarMaterial = (i, campo, valor) =>
    setForm((f) => ({ ...f, materiais: f.materiais.map((m, j) => (j === i ? { ...m, [campo]: valor } : m)) }))

  async function submeter(e) {
    e.preventDefault()
    const materiaisUsados = form.materiais
      .filter((m) => numeroOuNulo(m.quantidade) > 0)
      .map((m) => ({ materialId: Number(m.materialId), quantidade: numeroOuNulo(m.quantidade) }))
    const r = await executar(() => api.post('/producao', {
      encomendaId: Number(form.encomendaId),
      etapaId: Number(form.etapaId),
      artesaoId: Number(form.artesaoId),
      turnoId: Number(form.turnoId),
      data: form.data,
      horaInicio: form.horaInicio,
      horaFim: form.horaFim,
      custosIndirectos: numeroOuNulo(form.custosIndirectos) ?? 0,
      retrabalho: form.retrabalho,
      materiaisUsados,
    }), 'Produção registada')
    if (r.ok) criado()
  }

  const d = base.dados
  const semDados = d && (d.encomendas.length === 0 || d.artesaos.length === 0 || d.turnos.length === 0)

  return (
    <Modal titulo="Registar produção" aberto fechar={fechar} largo>
      <Conteudo {...base}>
        {semDados ? (
          <Mensagem tipo="aviso">
            Para registar produção é preciso ter uma encomenda paga (em produção), artesãos e turnos.
          </Mensagem>
        ) : (
          <form onSubmit={submeter}>
            <div className="grelha-form">
              <Campo rotulo="Encomenda" largura={2}>
                <select value={form.encomendaId} onChange={(e) => alterar('encomendaId', e.target.value)} required>
                  {d?.encomendas.map((x) => (
                    <option key={x.id} value={x.id}>#{x.id} · {x.pecaNome} × {x.quantidade} · {x.clienteNome}</option>
                  ))}
                </select>
              </Campo>
              <Campo rotulo="Etapa" largura={2}>
                <select value={form.etapaId} onChange={(e) => alterar('etapaId', e.target.value)} required>
                  {etapas.map((x) => <option key={x.id} value={x.id}>{x.ordem}. {x.descricao}</option>)}
                </select>
              </Campo>
              <Campo rotulo="Artesão" largura={2}>
                <select value={form.artesaoId} onChange={(e) => alterar('artesaoId', e.target.value)} required>
                  {d?.artesaos.map((x) => <option key={x.id} value={x.id}>{x.nome}</option>)}
                </select>
              </Campo>
              <Campo rotulo="Turno" largura={2}>
                <select value={form.turnoId} onChange={(e) => alterar('turnoId', e.target.value)} required>
                  {d?.turnos.map((x) => <option key={x.id} value={x.id}>{x.denominacao}</option>)}
                </select>
              </Campo>
              <Campo rotulo="Data">
                <input type="date" value={form.data} onChange={(e) => alterar('data', e.target.value)} required />
              </Campo>
              <Campo rotulo="Início">
                <input type="time" value={form.horaInicio} onChange={(e) => alterar('horaInicio', e.target.value)} required />
              </Campo>
              <Campo rotulo="Fim">
                <input type="time" value={form.horaFim} onChange={(e) => alterar('horaFim', e.target.value)} required />
              </Campo>
              <Campo rotulo="Custos indirectos (MT)">
                <input type="number" min="0" step="any" value={form.custosIndirectos}
                  onChange={(e) => alterar('custosIndirectos', e.target.value)} />
              </Campo>
            </div>

            <label className="caixa-marcar">
              <input type="checkbox" checked={form.retrabalho} onChange={(e) => alterar('retrabalho', e.target.checked)} />
              Foi retrabalho (correcção de uma peça com defeito)
            </label>

            <div className="sub-bloco">
              <div className="sub-bloco-topo">
                <h3>Materiais usados</h3>
                <button type="button" className="botao pequeno"
                  onClick={() => alterar('materiais', [...form.materiais, { materialId: d.materiais[0]?.id ?? '', quantidade: '' }])}>
                  Adicionar material
                </button>
              </div>
              {form.materiais.length === 0 && <p className="descricao">Nenhum material nesta etapa.</p>}
              {form.materiais.map((m, i) => (
                <div className="linha-dinamica" key={i}>
                  <select value={m.materialId} onChange={(e) => alterarMaterial(i, 'materialId', e.target.value)} aria-label="Material">
                    {d?.materiais.map((x) => <option key={x.id} value={x.id}>{x.nome} ({x.unidadeMedida})</option>)}
                  </select>
                  <input type="number" min="0" step="any" placeholder="Quantidade (vazio = não usado)"
                    aria-label="Quantidade usada" value={m.quantidade}
                    onChange={(e) => alterarMaterial(i, 'quantidade', e.target.value)} />
                  <button type="button" className="botao pequeno perigo"
                    onClick={() => alterar('materiais', form.materiais.filter((_, j) => j !== i))}>Remover</button>
                </div>
              ))}
            </div>

            <div className="modal-rodape">
              <button type="button" className="botao" onClick={fechar}>Cancelar</button>
              <button type="submit" className="botao principal" disabled={ocupado || !form.etapaId}>
                {ocupado ? 'A registar…' : 'Registar'}
              </button>
            </div>
          </form>
        )}
      </Conteudo>
    </Modal>
  )
}

export default function Producao() {
  const lista = useDados(() => api.get('/producao'))
  const { executar, ocupado } = useAccao()
  const [novo, setNovo] = useState(false)

  async function apagar(r) {
    if (!window.confirm('Apagar este registo de produção?')) return
    const res = await executar(() => api.apagar(`/producao/${r.id}`), 'Registo apagado')
    if (res.ok) lista.recarregar()
  }

  return (
    <>
      <Cabecalho titulo="Registo de produção" descricao="O que cada artesão fez, em que etapa, quanto tempo levou e que material gastou.">
        <button type="button" className="botao principal" onClick={() => setNovo(true)}>Registar produção</button>
      </Cabecalho>

      <section className="cartao">
        <Conteudo {...lista} vazio={lista.dados?.length === 0} textoVazio="Ainda não há produção registada.">
          <div className="tabela-envolvente">
            <table className="tabela">
              <thead>
                <tr>
                  <th>Data</th><th>Encomenda</th><th>Etapa</th><th>Artesão</th><th>Turno</th><th>Horário</th>
                  <th>Tempo</th><th>Materiais</th><th className="numero">Custo</th><th className="accoes" />
                </tr>
              </thead>
              <tbody>
                {lista.dados?.map((r) => (
                  <tr key={r.id}>
                    <td>{data(r.data)}</td>
                    <td className="mudo">#{r.encomendaId}</td>
                    <td>{r.etapaDescricao}{r.retrabalho && <span className="etiqueta laranja">retrabalho</span>}</td>
                    <td><strong>{r.artesaoNome}</strong></td>
                    <td>{r.turnoDenominacao}</td>
                    <td>{hora(r.horaInicio)}–{hora(r.horaFim)}</td>
                    <td>{minutos(r.tempoGastoMinutos)}</td>
                    <td className="pequeno-texto">
                      {r.materiaisUsados.length === 0 ? '—' : r.materiaisUsados
                        .map((m) => `${quantidade(m.quantidade)} ${m.unidadeMedida} ${m.materialNome}`).join(', ')}
                    </td>
                    <td className="numero">{dinheiro(r.custoMateriais + r.custosIndirectos)}</td>
                    <td className="accoes">
                      <button type="button" className="botao pequeno perigo" disabled={ocupado} onClick={() => apagar(r)}>Apagar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Conteudo>
      </section>

      {novo && <NovoRegisto fechar={() => setNovo(false)} criado={() => { setNovo(false); lista.recarregar() }} />}
    </>
  )
}
