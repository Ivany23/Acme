import { Navigate, Route, Routes } from 'react-router-dom'
import { usePode, useSessao } from './sessao.jsx'
import { eCliente } from './permissoes.js'
import Layout from './componentes/Layout.jsx'
import Login from './paginas/Login.jsx'
import Painel from './paginas/Painel.jsx'
import Encomendas from './paginas/Encomendas.jsx'
import Producao from './paginas/Producao.jsx'
import Ponto from './paginas/Ponto.jsx'
import Turnos from './paginas/Turnos.jsx'
import Relatorios from './paginas/Relatorios.jsx'
import Pecas from './paginas/Pecas.jsx'
import Materiais from './paginas/Materiais.jsx'
import Clientes from './paginas/Clientes.jsx'
import Funcionarios from './paginas/Funcionarios.jsx'
import Utilizadores from './paginas/Utilizadores.jsx'
import RegistarCliente from './paginas/cliente/RegistarCliente.jsx'
import PortalCliente from './paginas/cliente/PortalCliente.jsx'

/** Área da empresa: exige sessão de funcionário. */
function AreaFuncionarios({ children }) {
  const { sessao } = useSessao()
  if (!sessao) return <Navigate to="/entrar" replace />
  if (eCliente(sessao)) return <Navigate to="/cliente" replace />
  return children
}

/** Área do cliente: exige sessão de cliente. */
function AreaCliente({ children }) {
  const { sessao } = useSessao()
  if (!eCliente(sessao)) return <Navigate to="/cliente/entrar" replace />
  return children
}

/** Página a que o perfil não tem acesso: mostra uma mensagem (sem redireccionar, para nunca criar ciclos). */
function Permitido({ accao, children }) {
  const pode = usePode()
  if (pode(accao)) return children
  return (
    <div className="cartao">
      <h2>Sem acesso</h2>
      <p className="descricao">O seu perfil não tem permissão para abrir esta página.</p>
    </div>
  )
}

function destino(sessao) {
  return eCliente(sessao) ? '/cliente' : '/'
}

export default function App() {
  const { sessao } = useSessao()
  const p = (accao, elemento) => <Permitido accao={accao}>{elemento}</Permitido>

  return (
    <Routes>
      <Route path="/entrar" element={sessao ? <Navigate to={destino(sessao)} replace /> : <Login modo="funcionario" />} />
      <Route path="/cliente/entrar" element={eCliente(sessao) ? <Navigate to="/cliente" replace /> : <Login modo="cliente" />} />
      <Route path="/cliente/registar" element={eCliente(sessao) ? <Navigate to="/cliente" replace /> : <RegistarCliente />} />
      <Route path="/cliente" element={<AreaCliente><PortalCliente /></AreaCliente>} />

      <Route element={<AreaFuncionarios><Layout /></AreaFuncionarios>}>
        <Route index element={p('painel', <Painel />)} />
        <Route path="encomendas" element={p('encomendas.ver', <Encomendas />)} />
        <Route path="producao" element={p('producao.ver', <Producao />)} />
        <Route path="ponto" element={p('ponto.ver', <Ponto />)} />
        <Route path="turnos" element={p('turnos.ver', <Turnos />)} />
        <Route path="relatorios" element={p('relatorios.ver', <Relatorios />)} />
        <Route path="pecas" element={p('pecas.ver', <Pecas />)} />
        <Route path="materiais" element={p('materiais.ver', <Materiais />)} />
        <Route path="clientes" element={p('clientes.ver', <Clientes />)} />
        <Route path="funcionarios" element={p('funcionarios.ver', <Funcionarios />)} />
        <Route path="utilizadores" element={p('utilizadores', <Utilizadores />)} />
      </Route>
      <Route path="*" element={<Navigate to={sessao ? destino(sessao) : '/entrar'} replace />} />
    </Routes>
  )
}
