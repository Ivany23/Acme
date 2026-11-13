import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from '../../src/App.jsx'
import { SessaoProvider } from '../../src/sessao.jsx'
import { AvisosProvider } from '../../src/componentes/ui.jsx'
import { servidorFalso } from '../servidorFalso.js'

function renderApp(rota = '/entrar') {
  return render(
    <MemoryRouter initialEntries={[rota]}>
      <SessaoProvider>
        <AvisosProvider>
          <App />
        </AvisosProvider>
      </SessaoProvider>
    </MemoryRouter>,
  )
}

const resumo = {
  totalClientes: 2, totalPecas: 1, totalMateriais: 3, totalFuncionarios: 4, totalArtesaos: 3, totalTurnos: 2,
  encomendasPorEstado: { PENDENTE: 1, EM_PRODUCAO: 0, CONCLUIDA: 0, CANCELADA: 0 },
  analise: { totalRelatorios: 0, custoMedioPorEncomenda: 0, totalMinutosExtra: 0, desvioMedioOrcamento: 0, totalRetrabalhos: 0 },
}

// Testes de INTEGRAÇÃO: ecrã de login + sessão + rotas + cliente da API
describe('login', () => {
  it('sem sessão, qualquer página leva ao login', async () => {
    servidorFalso({ 'GET /estado': () => ({ estado: 'OK' }) })
    renderApp('/encomendas')
    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
  })

  it('entra com credenciais válidas e mostra o painel', async () => {
    const { chamadas } = servidorFalso({
      'GET /estado': () => ({ estado: 'OK' }),
      'POST /auth/login': () => ({ token: 'abc', utilizador: { id: 1, login: 'admin', nome: 'Administrador' } }),
      'GET /dashboard': () => resumo,
      'GET /encomendas': () => [],
    })
    renderApp()

    await userEvent.type(screen.getByLabelText('Utilizador'), 'admin')
    await userEvent.type(screen.getByLabelText('Password'), 'acme2026')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('heading', { name: 'Olá, Administrador' })).toBeInTheDocument()
    expect(chamadas.find((c) => c.caminho === '/auth/login').corpo).toEqual({ login: 'admin', password: 'acme2026' })
    expect(JSON.parse(localStorage.getItem('acme.sessao')).token).toBe('abc')
  })

  it('mostra o erro quando a password está errada', async () => {
    servidorFalso({
      'GET /estado': () => ({ estado: 'OK' }),
      'POST /auth/login': () => [401, { mensagem: 'Utilizador ou password incorrectos' }],
    })
    renderApp()

    await userEvent.type(screen.getByLabelText('Utilizador'), 'admin')
    await userEvent.type(screen.getByLabelText('Password'), 'errada')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Utilizador ou password incorrectos')).toBeInTheDocument()
    expect(localStorage.getItem('acme.sessao')).toBeNull()
  })
})
