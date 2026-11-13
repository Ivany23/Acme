import { Navigate, Route, Routes } from 'react-router-dom'
import { useSessao } from './sessao.jsx'
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

function Protegida({ children }) {
  const { sessao } = useSessao()
  return sessao ? children : <Navigate to="/entrar" replace />
}

export default function App() {
  const { sessao } = useSessao()
  return (
    <Routes>
      <Route path="/entrar" element={sessao ? <Navigate to="/" replace /> : <Login />} />
      <Route element={<Protegida><Layout /></Protegida>}>
        <Route index element={<Painel />} />
        <Route path="encomendas" element={<Encomendas />} />
        <Route path="producao" element={<Producao />} />
        <Route path="ponto" element={<Ponto />} />
        <Route path="turnos" element={<Turnos />} />
        <Route path="relatorios" element={<Relatorios />} />
        <Route path="pecas" element={<Pecas />} />
        <Route path="materiais" element={<Materiais />} />
        <Route path="clientes" element={<Clientes />} />
        <Route path="funcionarios" element={<Funcionarios />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
