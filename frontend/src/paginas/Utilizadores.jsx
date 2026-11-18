import { useState } from 'react'
import { api } from '../api.js'
import { Cabecalho, Campo, Conteudo, Mensagem, Modal, useAccao, useDados } from '../componentes/ui.jsx'
import { PAPEIS } from '../permissoes.js'
import { useSessao } from '../sessao.jsx'

const vazio = { login: '', nome: '', papel: 'ARTESAO', funcionarioId: '', clienteId: '', password: '' }

function FormUtilizador({ inicial, edicao, funcionarios, clientes, guardar, cancelar, ocupado }) {
  const [form, setForm] = useState(inicial)
  const alterar = (c, v) => setForm((f) => ({ ...f, [c]: v }))
  const artesaos = funcionarios.filter((f) => f.tipo === 'ARTESAO')
  const pessoas = form.papel === 'ARTESAO' ? artesaos : funcionarios

  function escolherPessoa(lista, campo, id) {
    const pessoa = lista.find((x) => String(x.id) === String(id))
    setForm((f) => ({ ...f, [campo]: id, nome: f.nome || pessoa?.nome || '' }))
  }

  function submeter(e) {
    e.preventDefault()
    guardar({
      login: form.login,
      nome: form.nome,
      papel: form.papel,
      funcionarioId: form.papel !== 'CLIENTE' && form.funcionarioId ? Number(form.funcionarioId) : null,
      clienteId: form.papel === 'CLIENTE' && form.clienteId ? Number(form.clienteId) : null,
      password: form.password || null,
    })
  }

  return (
    <form onSubmit={submeter}>
      <div className="grelha-form">
        <Campo rotulo="Perfil">
          <select value={form.papel} onChange={(e) => alterar('papel', e.target.value)} required>
            {Object.entries(PAPEIS).map(([valor, texto]) => <option key={valor} value={valor}>{texto}</option>)}
          </select>
        </Campo>
        {form.papel === 'CLIENTE' ? (
          <Campo rotulo="Cliente">
            <select value={form.clienteId} onChange={(e) => escolherPessoa(clientes, 'clienteId', e.target.value)} required>
              <option value="">Escolha…</option>
              {clientes.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </Campo>
        ) : (
          <Campo rotulo={form.papel === 'ARTESAO' ? 'Artesão' : 'Funcionário (opcional)'}>
            <select value={form.funcionarioId}
              onChange={(e) => escolherPessoa(pessoas, 'funcionarioId', e.target.value)}
              required={form.papel === 'ARTESAO'}>
              <option value="">{form.papel === 'ARTESAO' ? 'Escolha…' : 'Nenhum'}</option>
              {pessoas.map((f) => <option key={f.id} value={f.id}>{f.nome} ({f.cargo})</option>)}
            </select>
          </Campo>
        )}
        <Campo rotulo="Login" ajuda={edicao ? 'O login não pode ser alterado' : 'ex.: rita.langa'}>
          <input value={form.login} onChange={(e) => alterar('login', e.target.value)} disabled={edicao} required />
        </Campo>
        <Campo rotulo="Nome a mostrar">
          <input value={form.nome} onChange={(e) => alterar('nome', e.target.value)} required />
        </Campo>
        <Campo rotulo={edicao ? 'Nova password (opcional)' : 'Password'} ajuda="Pelo menos 6 caracteres">
          <input type="password" minLength={6} value={form.password} onChange={(e) => alterar('password', e.target.value)}
            autoComplete="new-password" required={!edicao} />
        </Campo>
      </div>
      {form.papel === 'ARTESAO' && artesaos.length === 0 && (
        <Mensagem tipo="aviso">Registe primeiro o artesão em Funcionários.</Mensagem>
      )}
      <div className="modal-rodape">
        <button type="button" className="botao" onClick={cancelar}>Cancelar</button>
        <button type="submit" className="botao principal" disabled={ocupado}>{ocupado ? 'A guardar…' : 'Guardar'}</button>
      </div>
    </form>
  )
}

export default function Utilizadores() {
  const { sessao } = useSessao()
  const base = useDados(async () => {
    const [utilizadores, funcionarios, clientes] = await Promise.all([
      api.get('/utilizadores'), api.get('/funcionarios'), api.get('/clientes'),
    ])
    return { utilizadores, funcionarios, clientes }
  })
  const { executar, ocupado } = useAccao()
  const [edicao, setEdicao] = useState(null)

  async function guardar(corpo) {
    const r = await executar(
      () => (edicao.id ? api.put(`/utilizadores/${edicao.id}`, corpo) : api.post('/utilizadores', corpo)),
      edicao.id ? 'Utilizador actualizado' : 'Utilizador criado',
    )
    if (r.ok) {
      setEdicao(null)
      base.recarregar()
    }
  }

  async function apagar(u) {
    if (!window.confirm(`Apagar a conta "${u.login}"?`)) return
    const r = await executar(() => api.apagar(`/utilizadores/${u.id}`), 'Conta apagada')
    if (r.ok) base.recarregar()
  }

  function editar(u) {
    setEdicao({
      id: u.id,
      form: {
        login: u.login, nome: u.nome, papel: u.papel, password: '',
        funcionarioId: u.funcionarioId ?? '', clienteId: u.clienteId ?? '',
      },
    })
  }

  return (
    <>
      <Cabecalho titulo="Utilizadores" descricao="Contas de acesso. Cada perfil só vê e faz o que lhe compete.">
        <button type="button" className="botao principal" onClick={() => setEdicao({ id: null, form: vazio })}>
          Novo utilizador
        </button>
      </Cabecalho>

      <section className="cartao">
        <Conteudo {...base} vazio={base.dados?.utilizadores.length === 0}>
          <div className="tabela-envolvente">
            <table className="tabela">
              <thead>
                <tr><th>Nome</th><th>Login</th><th>Perfil</th><th>Associado a</th><th className="accoes">Acções</th></tr>
              </thead>
              <tbody>
                {base.dados?.utilizadores.map((u) => (
                  <tr key={u.id}>
                    <td><strong>{u.nome}</strong></td>
                    <td className="mudo">{u.login}</td>
                    <td><span className={`etiqueta papel-${u.papel.toLowerCase()}`}>{u.papelDescricao}</span></td>
                    <td>{u.funcionarioNome || u.clienteNome || '—'}</td>
                    <td className="accoes">
                      <button type="button" className="botao pequeno" onClick={() => editar(u)}>Editar</button>
                      {u.id !== sessao?.utilizador?.id && (
                        <button type="button" className="botao pequeno perigo" disabled={ocupado} onClick={() => apagar(u)}>Apagar</button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Conteudo>
      </section>

      <section className="cartao">
        <h2>O que cada perfil pode fazer</h2>
        <table className="tabela compacta">
          <tbody>
            <tr><td><strong>Director-Geral</strong></td><td>Tudo o que é da gestão, excepto gerir utilizadores.</td></tr>
            <tr><td><strong>Contador</strong></td><td>Materiais, peças e custos; confirma pagamentos; consulta relatórios e análise.</td></tr>
            <tr><td><strong>Supervisor de Turno</strong></td><td>Turnos e alocação; produção e ponto de todos os artesãos; conclui encomendas; relatórios.</td></tr>
            <tr><td><strong>Gerente de Vendas</strong></td><td>Clientes, orçamentos e encomendas; consulta o histórico (relatórios e análise).</td></tr>
            <tr><td><strong>Artesão</strong></td><td>Regista a sua própria produção e o seu ponto; consulta encomendas, turnos e catálogo.</td></tr>
            <tr><td><strong>Cliente</strong></td><td>Área de cliente: orçamentos, encomendas e acompanhamento do estado.</td></tr>
          </tbody>
        </table>
      </section>

      <Modal titulo={edicao?.id ? 'Editar utilizador' : 'Novo utilizador'} aberto={!!edicao} fechar={() => setEdicao(null)} largo>
        {edicao && (
          <FormUtilizador inicial={edicao.form} edicao={!!edicao.id} funcionarios={base.dados?.funcionarios || []}
            clientes={base.dados?.clientes || []} guardar={guardar} cancelar={() => setEdicao(null)} ocupado={ocupado} />
        )}
      </Modal>
    </>
  )
}
