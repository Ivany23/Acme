import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api, textoErro } from '../api.js'
import { useSessao } from '../sessao.jsx'

export default function Login() {
  const { entrar } = useSessao()
  const navegar = useNavigate()
  const [login, setLogin] = useState('')
  const [password, setPassword] = useState('')
  const [erro, setErro] = useState('')
  const [aEntrar, setAEntrar] = useState(false)
  const [servidor, setServidor] = useState('a verificar')

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
      await entrar(login, password)
      navegar('/', { replace: true })
    } catch (falha) {
      setErro(textoErro(falha))
    } finally {
      setAEntrar(false)
    }
  }

  return (
    <div className="pagina-login">
      <div className="login-painel">
        <div className="login-lado">
          <img src="/marca.svg" alt="" width="48" height="48" />
          <h1>Acme</h1>
          <p>Controlo de produção da oficina: encomendas, turnos, produção e custos num só lugar.</p>
        </div>

        <form className="login-form" onSubmit={submeter}>
          <h2>Entrar</h2>
          <label className="campo">
            <span className="campo-rotulo">Utilizador</span>
            <input value={login} onChange={(e) => setLogin(e.target.value)} autoComplete="username" required autoFocus />
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
