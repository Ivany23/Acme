import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api, textoErro } from '../api.js'
import { useSessao } from '../sessao.jsx'

const TEXTOS = {
  funcionario: {
    titulo: 'Entrar',
    lado: 'Controlo de produção da oficina: encomendas, turnos, produção e custos num só lugar.',
    rotulo: 'Utilizador',
  },
  cliente: {
    titulo: 'Área de cliente',
    lado: 'Peça orçamentos, faça encomendas e acompanhe o estado de cada uma.',
    rotulo: 'Email',
  },
}

/** Ecrã de login. modo = "funcionario" (gestão) ou "cliente" (portal do cliente). */
export default function Login({ modo = 'funcionario' }) {
  const { entrar, sair } = useSessao()
  const navegar = useNavigate()
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [erro, setErro] = useState('')
  const [aEntrar, setAEntrar] = useState(false)
  const [servidor, setServidor] = useState('a verificar')
  const t = TEXTOS[modo]

  // "Acorda" o servidor do Render enquanto o utilizador escreve
  useEffect(() => {
    let activo = true
    api.get('/estado')
      .then(() => activo && setServidor('ligado'))
      .catch(() => activo && setServidor('indisponivel'))
    return () => { activo = false }
  }, [])

  async function submeter(e) {
    e.preventDefault()
    setErro('')
    setAEntrar(true)
    try {
      const sessao = await entrar(login, password)
      const cliente = sessao.utilizador.papel === 'CLIENTE'
      if (modo === 'cliente' && !cliente) {
        sair()
        setErro('Esta conta é de funcionário. Use a entrada da empresa.')
        return
      }
      navegar(cliente ? '/cliente' : '/', { replace: true })
    } catch (falha) {
      setErro(textoErro(falha))
    } finally {
      setAEntrar(false)
    }
  }

  return (
    <div className={`pagina-login ${modo === 'cliente' ? 'login-cliente' : ''}`}>
      <div className="login-painel">
        <div className="login-lado">
          <img src="/marca.svg" alt="" width="48" height="48" />
          <h1>Acme</h1>
          <p>{t.lado}</p>
        </div>

        <form className="login-form" onSubmit={submeter}>
          <h2>{t.titulo}</h2>
          <label className="campo">
            <span className="campo-rotulo">{t.rotulo}</span>
            <input value={login} onChange={(e) => setLogin(e.target.value)} autoComplete="username"
              type={modo === 'cliente' ? 'email' : 'text'} required autoFocus />
          </label>
          <label className="campo">
            <span className="campo-rotulo">Password</span>
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password" required />
          </label>

          {erro && <div className="mensagem erro">{erro}</div>}

          <button type="submit" className="botao principal largo" disabled={aEntrar}>
            {aEntrar ? 'A entrar…' : 'Entrar'}
          </button>

          {modo === 'cliente' ? (
            <p className="login-ligacoes">
              Ainda não tem conta? <Link to="/cliente/registar">Criar conta</Link>
              <span>·</span>
              <Link to="/entrar">Sou funcionário</Link>
            </p>
          ) : (
            <p className="login-ligacoes">
              É cliente? <Link to="/cliente/entrar">Entrar na área de cliente</Link>
            </p>
          )}

          <p className={`estado-servidor ${servidor}`}>
            {servidor === 'a verificar' && 'A ligar ao servidor (pode demorar até 1 minuto na primeira vez)…'}
            {servidor === 'ligado' && 'Servidor disponível'}
            {servidor === 'indisponivel' && 'Servidor indisponível de momento'}
          </p>
        </form>
      </div>
    </div>
  )
}
