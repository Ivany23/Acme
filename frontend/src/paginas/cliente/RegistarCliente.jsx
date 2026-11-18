import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { textoErro } from '../../api.js'
import { useSessao } from '../../sessao.jsx'

/** Caso de uso "Registar Conta de Cliente" (TG1). */
export default function RegistarCliente() {
  const { registarCliente } = useSessao()
  const navegar = useNavigate()
  const [form, setForm] = useState({ nome: '', email: '', telefone: '', password: '', confirmar: '' })
  const [erro, setErro] = useState('')
  const [aGuardar, setAGuardar] = useState(false)
  const alterar = (c, v) => setForm((f) => ({ ...f, [c]: v }))

  async function submeter(e) {
    e.preventDefault()
    if (form.password !== form.confirmar) {
      setErro('As passwords não coincidem')
      return
    }
    setErro('')
    setAGuardar(true)
    try {
      await registarCliente({
        nome: form.nome, email: form.email, telefone: form.telefone || null, password: form.password,
      })
      navegar('/cliente', { replace: true })
    } catch (falha) {
      setErro(textoErro(falha))
    } finally {
      setAGuardar(false)
    }
  }

  return (
    <div className="pagina-login login-cliente">
      <div className="login-painel">
        <div className="login-lado">
          <img src="/marca.svg" alt="" width="48" height="48" />
          <h1>Acme</h1>
          <p>Crie a sua conta para pedir orçamentos e acompanhar as suas encomendas.</p>
        </div>

        <form className="login-form" onSubmit={submeter}>
          <h2>Criar conta</h2>
          <label className="campo">
            <span className="campo-rotulo">Nome</span>
            <input value={form.nome} onChange={(e) => alterar('nome', e.target.value)} autoComplete="name" required autoFocus />
          </label>
          <label className="campo">
            <span className="campo-rotulo">Email</span>
            <input type="email" value={form.email} onChange={(e) => alterar('email', e.target.value)} autoComplete="email" required />
          </label>
          <label className="campo">
            <span className="campo-rotulo">Telefone (opcional)</span>
            <input value={form.telefone} onChange={(e) => alterar('telefone', e.target.value)} autoComplete="tel" />
          </label>
          <label className="campo">
            <span className="campo-rotulo">Password</span>
            <input type="password" minLength={6} value={form.password}
              onChange={(e) => alterar('password', e.target.value)} autoComplete="new-password" required />
          </label>
          <label className="campo">
            <span className="campo-rotulo">Confirmar password</span>
            <input type="password" minLength={6} value={form.confirmar}
              onChange={(e) => alterar('confirmar', e.target.value)} autoComplete="new-password" required />
          </label>

          {erro && <div className="mensagem erro">{erro}</div>}

          <button type="submit" className="botao principal largo" disabled={aGuardar}>
            {aGuardar ? 'A criar…' : 'Criar conta'}
          </button>
          <p className="login-ligacoes">Já tem conta? <Link to="/cliente/entrar">Entrar</Link></p>
        </form>
      </div>
    </div>
  )
}
