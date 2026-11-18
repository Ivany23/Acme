import { useState } from 'react'
import { api } from '../api.js'
import { numeroOuNulo } from '../util.js'
import { Cabecalho, Campo, Conteudo, Modal, useAccao, useDados } from './ui.jsx'

/**
 * Página de listagem + criar/editar/apagar para recursos simples.
 * campos: [{ nome, rotulo, tipo: 'text'|'number'|'email'|'time'|'select', opcoes, obrigatorio, mostrar(form) }]
 * colunas: [{ rotulo, valor(linha), classe }]
 */
export default function CrudSimples({
  titulo, descricao, recurso, nomeSingular, campos, colunas, vazio, paraFormulario, paraPedido, extraLinha,
  podeAlterar = true,
}) {
  const lista = useDados(() => api.get(`/${recurso}`), [recurso])
  const { executar, ocupado } = useAccao()
  const [edicao, setEdicao] = useState(null) // { id?, form }

  const temAccoes = podeAlterar || Boolean(extraLinha)

  const formVazio = () => Object.fromEntries(campos.map((c) => [c.nome, c.inicial ?? '']))

  function abrirNovo() {
    setEdicao({ id: null, form: formVazio() })
  }

  function abrirEdicao(linha) {
    const base = paraFormulario ? paraFormulario(linha) : linha
    const form = Object.fromEntries(campos.map((c) => [c.nome, base[c.nome] ?? '']))
    setEdicao({ id: linha.id, form })
  }

  function alterar(nome, valor) {
    setEdicao((e) => ({ ...e, form: { ...e.form, [nome]: valor } }))
  }

  async function guardar(evento) {
    evento.preventDefault()
    const corpo = {}
    for (const c of campos) {
      if (c.mostrar && !c.mostrar(edicao.form)) continue
      const v = edicao.form[c.nome]
      corpo[c.nome] = c.tipo === 'number' ? numeroOuNulo(v) : (v === '' ? null : v)
    }
    const final = paraPedido ? paraPedido(corpo) : corpo
    const r = await executar(
      () => (edicao.id ? api.put(`/${recurso}/${edicao.id}`, final) : api.post(`/${recurso}`, final)),
      edicao.id ? `${nomeSingular} actualizado` : `${nomeSingular} criado`,
    )
    if (r.ok) {
      setEdicao(null)
      lista.recarregar()
    }
  }

  async function apagar(linha) {
    if (!window.confirm(`Apagar "${linha.nome || linha.denominacao}"?`)) return
    const r = await executar(() => api.apagar(`/${recurso}/${linha.id}`), `${nomeSingular} apagado`)
    if (r.ok) lista.recarregar()
  }

  return (
    <>
      <Cabecalho titulo={titulo} descricao={descricao}>
        {podeAlterar && (
          <button type="button" className="botao principal" onClick={abrirNovo}>Novo {nomeSingular.toLowerCase()}</button>
        )}
      </Cabecalho>

      <section className="cartao">
        <Conteudo {...lista} vazio={lista.dados?.length === 0} textoVazio={vazio}>
          <div className="tabela-envolvente">
            <table className="tabela">
              <thead>
                <tr>
                  {colunas.map((c) => <th key={c.rotulo} className={c.classe}>{c.rotulo}</th>)}
                  {temAccoes && <th className="accoes">Acções</th>}
                </tr>
              </thead>
              <tbody>
                {lista.dados?.map((linha) => (
                  <tr key={linha.id}>
                    {colunas.map((c) => <td key={c.rotulo} className={c.classe}>{c.valor(linha)}</td>)}
                    {temAccoes && (
                      <td className="accoes">
                        {extraLinha && extraLinha(linha)}
                        {podeAlterar && (
                          <>
                            <button type="button" className="botao pequeno" onClick={() => abrirEdicao(linha)}>Editar</button>
                            <button type="button" className="botao pequeno perigo" onClick={() => apagar(linha)} disabled={ocupado}>
                              Apagar
                            </button>
                          </>
                        )}
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Conteudo>
      </section>

      <Modal
        titulo={edicao?.id ? `Editar ${nomeSingular.toLowerCase()}` : `Novo ${nomeSingular.toLowerCase()}`}
        aberto={!!edicao}
        fechar={() => setEdicao(null)}
      >
        {edicao && (
          <form onSubmit={guardar}>
            <div className="grelha-form">
              {campos.filter((c) => !c.mostrar || c.mostrar(edicao.form)).map((c) => (
                <Campo key={c.nome} rotulo={c.rotulo} ajuda={c.ajuda} largura={c.largura}>
                  {c.tipo === 'select' ? (
                    <select
                      value={edicao.form[c.nome]}
                      onChange={(e) => alterar(c.nome, e.target.value)}
                      required={c.obrigatorio}
                      disabled={c.soNaCriacao && !!edicao.id}
                    >
                      {c.opcoes.map((o) => <option key={o.valor} value={o.valor}>{o.texto}</option>)}
                    </select>
                  ) : (
                    <input
                      type={c.tipo || 'text'}
                      step={c.tipo === 'number' ? 'any' : undefined}
                      min={c.minimo}
                      value={edicao.form[c.nome]}
                      onChange={(e) => alterar(c.nome, e.target.value)}
                      required={c.obrigatorio}
                    />
                  )}
                </Campo>
              ))}
            </div>
            <div className="modal-rodape">
              <button type="button" className="botao" onClick={() => setEdicao(null)}>Cancelar</button>
              <button type="submit" className="botao principal" disabled={ocupado}>
                {ocupado ? 'A guardar…' : 'Guardar'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </>
  )
}
