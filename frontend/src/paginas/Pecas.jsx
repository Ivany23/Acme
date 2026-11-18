import { useState } from 'react'
import { api } from '../api.js'
import { Cabecalho, Campo, Conteudo, Mensagem, Modal, useAccao, useDados } from '../componentes/ui.jsx'
import { dinheiro, numeroOuNulo, quantidade } from '../util.js'
import { usePode } from '../sessao.jsx'

const formVazio = { nome: '', dimensoes: '', precoUnitario: '', materiais: [], etapas: [] }

function FormPeca({ inicial, materiais, guardar, cancelar, ocupado }) {
  const [form, setForm] = useState(inicial)
  const alterar = (campo, valor) => setForm((f) => ({ ...f, [campo]: valor }))
  const alterarLinha = (lista, i, campo, valor) =>
    setForm((f) => ({ ...f, [lista]: f[lista].map((l, j) => (j === i ? { ...l, [campo]: valor } : l)) }))
  const removerLinha = (lista, i) => setForm((f) => ({ ...f, [lista]: f[lista].filter((_, j) => j !== i) }))

  function submeter(e) {
    e.preventDefault()
    guardar({
      nome: form.nome,
      dimensoes: form.dimensoes || null,
      precoUnitario: numeroOuNulo(form.precoUnitario),
      materiais: form.materiais.map((m) => ({ materialId: Number(m.materialId), quantidade: numeroOuNulo(m.quantidade) })),
      etapas: form.etapas.map((e) => ({ id: e.id ?? null, descricao: e.descricao, ordem: Number(e.ordem) })),
    })
  }

  return (
    <form onSubmit={submeter}>
      <div className="grelha-form">
        <Campo rotulo="Nome" largura={2}>
          <input value={form.nome} onChange={(e) => alterar('nome', e.target.value)} required />
        </Campo>
        <Campo rotulo="Dimensões">
          <input value={form.dimensoes} onChange={(e) => alterar('dimensoes', e.target.value)} placeholder="ex.: 45x45x90 cm" />
        </Campo>
        <Campo rotulo="Preço unitário (MT)">
          <input type="number" step="any" min="0" value={form.precoUnitario}
            onChange={(e) => alterar('precoUnitario', e.target.value)} required />
        </Campo>
      </div>

      <div className="sub-bloco">
        <div className="sub-bloco-topo">
          <h3>Materiais por unidade</h3>
          <button type="button" className="botao pequeno" disabled={materiais.length === 0}
            onClick={() => alterar('materiais', [...form.materiais, { materialId: materiais[0]?.id ?? '', quantidade: '' }])}>
            Adicionar material
          </button>
        </div>
        {materiais.length === 0 && <Mensagem tipo="aviso">Registe primeiro os materiais.</Mensagem>}
        {form.materiais.map((m, i) => (
          <div className="linha-dinamica" key={i}>
            <select value={m.materialId} onChange={(e) => alterarLinha('materiais', i, 'materialId', e.target.value)}
              aria-label="Material" required>
              {materiais.map((x) => <option key={x.id} value={x.id}>{x.nome} ({x.unidadeMedida})</option>)}
            </select>
            <input type="number" step="any" min="0" placeholder="Quantidade" aria-label="Quantidade" required
              value={m.quantidade} onChange={(e) => alterarLinha('materiais', i, 'quantidade', e.target.value)} />
            <button type="button" className="botao pequeno perigo" onClick={() => removerLinha('materiais', i)}>Remover</button>
          </div>
        ))}
      </div>

      <div className="sub-bloco">
        <div className="sub-bloco-topo">
          <h3>Etapas de produção</h3>
          <button type="button" className="botao pequeno"
            onClick={() => alterar('etapas', [...form.etapas, { descricao: '', ordem: form.etapas.length + 1 }])}>
            Adicionar etapa
          </button>
        </div>
        {form.etapas.map((et, i) => (
          <div className="linha-dinamica" key={et.id ?? `nova-${i}`}>
            <input type="number" min="0" className="curto" aria-label="Ordem" required
              value={et.ordem} onChange={(e) => alterarLinha('etapas', i, 'ordem', e.target.value)} />
            <input placeholder="Descrição (ex.: Corte)" aria-label="Descrição da etapa" required
              value={et.descricao} onChange={(e) => alterarLinha('etapas', i, 'descricao', e.target.value)} />
            <button type="button" className="botao pequeno perigo" onClick={() => removerLinha('etapas', i)}>Remover</button>
          </div>
        ))}
      </div>

      <div className="modal-rodape">
        <button type="button" className="botao" onClick={cancelar}>Cancelar</button>
        <button type="submit" className="botao principal" disabled={ocupado}>{ocupado ? 'A guardar…' : 'Guardar'}</button>
      </div>
    </form>
  )
}

export default function Pecas() {
  const base = useDados(async () => {
    const [pecas, materiais] = await Promise.all([api.get('/pecas'), api.get('/materiais')])
    return { pecas, materiais }
  })
  const { executar, ocupado } = useAccao()
  const [edicao, setEdicao] = useState(null)
  const podeAlterar = usePode()('pecas.alterar')

  function editar(p) {
    setEdicao({
      id: p.id,
      form: {
        nome: p.nome,
        dimensoes: p.dimensoes || '',
        precoUnitario: p.precoUnitario,
        materiais: p.materiais.map((m) => ({ materialId: m.materialId, quantidade: m.quantidadePorUnidade })),
        etapas: p.etapas.map((e) => ({ id: e.id, descricao: e.descricao, ordem: e.ordem })),
      },
    })
  }

  async function guardar(corpo) {
    const r = await executar(
      () => (edicao.id ? api.put(`/pecas/${edicao.id}`, corpo) : api.post('/pecas', corpo)),
      edicao.id ? 'Peça actualizada' : 'Peça criada',
    )
    if (r.ok) {
      setEdicao(null)
      base.recarregar()
    }
  }

  async function apagar(p) {
    if (!window.confirm(`Apagar a peça "${p.nome}"?`)) return
    const r = await executar(() => api.apagar(`/pecas/${p.id}`), 'Peça apagada')
    if (r.ok) base.recarregar()
  }

  return (
    <>
      <Cabecalho titulo="Peças" descricao="Catálogo de peças, com os materiais e as etapas necessárias ao fabrico.">
        {podeAlterar && (
          <button type="button" className="botao principal" onClick={() => setEdicao({ id: null, form: formVazio })}>
            Nova peça
          </button>
        )}
      </Cabecalho>

      <Conteudo {...base} vazio={base.dados?.pecas.length === 0} textoVazio="Nenhuma peça registada.">
        <div className="grelha-cartoes">
          {base.dados?.pecas.map((p) => (
            <article key={p.id} className="cartao peca">
              <div className="peca-topo">
                <div>
                  <h2>{p.nome}</h2>
                  <span className="descricao">{p.dimensoes || 'Sem dimensões'}</span>
                </div>
                <span className="preco">{dinheiro(p.precoUnitario)}</span>
              </div>
              <dl className="peca-detalhes">
                <dt>Materiais / unidade</dt>
                <dd>
                  {p.materiais.length === 0 ? '—' : p.materiais.map((m) => (
                    <div key={m.materialId}>{quantidade(m.quantidadePorUnidade)} {m.unidadeMedida} de {m.materialNome}</div>
                  ))}
                  <div className="destaque">Custo: {dinheiro(p.custoMateriaisPorUnidade)}</div>
                </dd>
                <dt>Etapas</dt>
                <dd>
                  {p.etapas.length === 0 ? '—' : (
                    <ol className="etapas">{p.etapas.map((e) => <li key={e.id}>{e.descricao}</li>)}</ol>
                  )}
                </dd>
              </dl>
              {podeAlterar && (
                <div className="peca-accoes">
                  <button type="button" className="botao pequeno" onClick={() => editar(p)}>Editar</button>
                  <button type="button" className="botao pequeno perigo" onClick={() => apagar(p)} disabled={ocupado}>Apagar</button>
                </div>
              )}
            </article>
          ))}
        </div>
      </Conteudo>

      <Modal titulo={edicao?.id ? 'Editar peça' : 'Nova peça'} aberto={!!edicao} fechar={() => setEdicao(null)} largo>
        {edicao && (
          <FormPeca inicial={edicao.form} materiais={base.dados?.materiais || []} guardar={guardar}
            cancelar={() => setEdicao(null)} ocupado={ocupado} />
        )}
      </Modal>
    </>
  )
}
