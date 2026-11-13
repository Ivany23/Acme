import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { useSessao } from '../sessao.jsx'

const MENU = [
  { grupo: 'Geral', itens: [{ para: '/', texto: 'Painel' }] },
  {
    grupo: 'Produção',
    itens: [
      { para: '/encomendas', texto: 'Encomendas' },
      { para: '/producao', texto: 'Registo de produção' },
      { para: '/ponto', texto: 'Registo de ponto' },
      { para: '/turnos', texto: 'Turnos e alocação' },
      { para: '/relatorios', texto: 'Relatórios' },
    ],
  },
  {
    grupo: 'Cadastros',
    itens: [
      { para: '/pecas', texto: 'Peças' },
      { para: '/materiais', texto: 'Materiais' },
      { para: '/clientes', texto: 'Clientes' },
      { para: '/funcionarios', texto: 'Funcionários' },
    ],
  },
]

export default function Layout() {
  const { sessao, sair } = useSessao()
  const [menuAberto, setMenuAberto] = useState(false)

  return (
    <div className={`aplicacao ${menuAberto ? 'menu-aberto' : ''}`}>
      <aside className="lateral">
        <div className="marca">
          <img src="/marca.svg" alt="" width="34" height="34" />
          <div>
            <strong>Acme</strong>
            <span>Oficina de mobiliário</span>
          </div>
        </div>
        <nav onClick={() => setMenuAberto(false)}>
          {MENU.map((g) => (
            <div key={g.grupo} className="menu-grupo">
              <span className="menu-titulo">{g.grupo}</span>
              {g.itens.map((i) => (
                <NavLink key={i.para} to={i.para} end={i.para === '/'} className="menu-item">
                  {i.texto}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
        <div className="lateral-rodape">
          <span className="utilizador">{sessao?.utilizador?.nome}</span>
          <button type="button" className="botao-sair" onClick={sair}>Sair</button>
        </div>
      </aside>

      <div className="principal">
        <div className="barra-movel">
          <button type="button" className="botao" onClick={() => setMenuAberto((v) => !v)}>Menu</button>
          <strong>Acme</strong>
        </div>
        <main className="conteudo">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
