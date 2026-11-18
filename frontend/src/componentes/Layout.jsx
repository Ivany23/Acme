import { useState } from 'react'
import { NavLink, Outlet } from 'react-router-dom'
import { usePode, useSessao } from '../sessao.jsx'
import { PAPEIS } from '../permissoes.js'
import AlterarPassword from './AlterarPassword.jsx'

const MENU = [
  { grupo: 'Geral', itens: [{ para: '/', texto: 'Painel', accao: 'painel' }] },
  {
    grupo: 'Produção',
    itens: [
      { para: '/encomendas', texto: 'Encomendas', accao: 'encomendas.ver' },
      { para: '/producao', texto: 'Registo de produção', accao: 'producao.ver' },
      { para: '/ponto', texto: 'Registo de ponto', accao: 'ponto.ver' },
      { para: '/turnos', texto: 'Turnos e alocação', accao: 'turnos.ver' },
      { para: '/relatorios', texto: 'Relatórios e análise', accao: 'relatorios.ver' },
    ],
  },
  {
    grupo: 'Cadastros',
    itens: [
      { para: '/pecas', texto: 'Peças', accao: 'pecas.ver' },
      { para: '/materiais', texto: 'Materiais', accao: 'materiais.ver' },
      { para: '/clientes', texto: 'Clientes', accao: 'clientes.ver' },
      { para: '/funcionarios', texto: 'Funcionários', accao: 'funcionarios.ver' },
    ],
  },
  { grupo: 'Administração', itens: [{ para: '/utilizadores', texto: 'Utilizadores', accao: 'utilizadores' }] },
]

export default function Layout() {
  const { sessao, sair } = useSessao()
  const pode = usePode()
  const [menuAberto, setMenuAberto] = useState(false)
  const [password, setPassword] = useState(false)
  const utilizador = sessao?.utilizador

  return (
    <div className={`aplicacao ${menuAberto ? 'menu-aberto' : ''}`}>
      <aside className="lateral">
        <div className="marca">
          <img src="/marca.svg" alt="" width="34" height="34" />
          <div>
            <strong>Acme</strong>
            <span>Controlo de produção</span>
          </div>
        </div>
        <nav onClick={() => setMenuAberto(false)}>
          {MENU.map((g) => {
            const itens = g.itens.filter((i) => pode(i.accao))
            if (itens.length === 0) return null
            return (
              <div key={g.grupo} className="menu-grupo">
                <span className="menu-titulo">{g.grupo}</span>
                {itens.map((i) => (
                  <NavLink key={i.para} to={i.para} end={i.para === '/'} className="menu-item">
                    {i.texto}
                  </NavLink>
                ))}
              </div>
            )
          })}
        </nav>
        <div className="lateral-rodape">
          <div className="utilizador">
            <strong>{utilizador?.nome}</strong>
            <span>{PAPEIS[utilizador?.papel] ?? ''}</span>
          </div>
          <div className="lateral-botoes">
            <button type="button" className="botao-sair" onClick={() => setPassword(true)}>Password</button>
            <button type="button" className="botao-sair" onClick={sair}>Sair</button>
          </div>
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
      {password && <AlterarPassword fechar={() => setPassword(false)} />}
    </div>
  )
}
