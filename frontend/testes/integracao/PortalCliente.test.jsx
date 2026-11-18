import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from '../../src/App.jsx'
import { SessaoProvider } from '../../src/sessao.jsx'
import { AvisosProvider } from '../../src/componentes/ui.jsx'
import { servidorFalso, sessaoActiva } from '../servidorFalso.js'

function renderApp(rota) {
  return render(
    <MemoryRouter initialEntries={[rota]}>
      <SessaoProvider><AvisosProvider><App /></AvisosProvider></SessaoProvider>
    </MemoryRouter>,
  )
}

const cliente = { id: 9, login: 'ana@mail.com', nome: 'Ana', papel: 'CLIENTE', papelDescricao: 'Cliente', clienteId: 4 }
const peca = { id: 1, nome: 'Cadeira', dimensoes: '45x45x90 cm', precoUnitario: 1000 }
const orcamento = {
  pecaId: 1, pecaNome: 'Cadeira', quantidade: 2, custoPecas: 2000, custoMateriais: 500, custosIndirectos: 0,
  percentagemOrganizacao: 20, custoBase: 2500, custoEstimado: 3000,
}

// Testes de INTEGRAÇÃO: área de cliente (casos de uso do actor Cliente no TG1) com a API simulada
describe('área de cliente', () => {
  it('regista a conta e entra na área de cliente', async () => {
    const { chamadas } = servidorFalso({
      'POST /portal/registar': () => [201, { token: 't', utilizador: cliente }],
      'GET /portal/encomendas': () => [],
      'GET /portal/notificacoes': () => [],
      'GET /portal/pecas': () => [peca],
      'GET /portal/orcamento': () => orcamento,
    })
    renderApp('/cliente/registar')

    await userEvent.type(screen.getByLabelText('Nome'), 'Ana')
    await userEvent.type(screen.getByLabelText('Email'), 'ana@mail.com')
    await userEvent.type(screen.getByLabelText('Password'), 'segredo1')
    await userEvent.type(screen.getByLabelText('Confirmar password'), 'segredo1')
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
    expect(chamadas.find((c) => c.caminho === '/portal/registar').corpo)
      .toEqual({ nome: 'Ana', email: 'ana@mail.com', telefone: null, password: 'segredo1' })
  })

  it('não envia o registo se as passwords não coincidirem', async () => {
    const { chamadas } = servidorFalso({})
    renderApp('/cliente/registar')

    await userEvent.type(screen.getByLabelText('Nome'), 'Ana')
    await userEvent.type(screen.getByLabelText('Email'), 'ana@mail.com')
    await userEvent.type(screen.getByLabelText('Password'), 'segredo1')
    await userEvent.type(screen.getByLabelText('Confirmar password'), 'outra123')
    await userEvent.click(screen.getByRole('button', { name: 'Criar conta' }))

    expect(screen.getByText('As passwords não coincidem')).toBeInTheDocument()
    expect(chamadas).toHaveLength(0)
  })

  it('mostra o orçamento, faz a encomenda e lista o estado e as notificações', async () => {
    sessaoActiva(cliente)
    const encomendas = []
    const { chamadas } = servidorFalso({
      'GET /portal/pecas': () => [peca],
      'GET /portal/orcamento': () => orcamento,
      'GET /portal/encomendas': () => encomendas,
      'GET /portal/notificacoes': () => ['Encomenda #5 está agora EM_PRODUCAO'],
      'POST /portal/encomendas': (corpo) => {
        const nova = { id: 6, clienteId: 4, pecaId: 1, pecaNome: 'Cadeira', quantidade: corpo.quantidade, estado: 'PENDENTE',
          paga: false, custosIndirectos: 0, custoEstimado: 3000, dataCriacao: '2026-11-18T10:00:00' }
        encomendas.push(nova)
        return [201, nova]
      },
    })
    window.confirm = () => true
    renderApp('/cliente')

    expect(await screen.findByText('Encomenda #5 está agora EM_PRODUCAO')).toBeInTheDocument()
    const quantidade = await screen.findByLabelText('Quantidade')
    await userEvent.clear(quantidade)
    await userEvent.type(quantidade, '2')
    expect(await screen.findByText('3000,00 MT')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Fazer encomenda' }))

    expect(await screen.findByText('A aguardar confirmação do pagamento pela Acme.')).toBeInTheDocument()
    expect(chamadas.find((c) => c.metodo === 'POST').corpo).toEqual({ pecaId: 1, quantidade: 2 })
  })

  it('uma conta de funcionário não entra pela área de cliente', async () => {
    servidorFalso({
      'GET /estado': () => ({ estado: 'OK' }),
      'POST /auth/login': () => ({ token: 't', utilizador: { id: 1, login: 'admin', nome: 'Administrador', papel: 'ADMINISTRADOR' } }),
    })
    renderApp('/cliente/entrar')

    await userEvent.type(screen.getByLabelText('Email'), 'admin@acme.co')
    await userEvent.type(screen.getByLabelText('Password'), 'acme2026')
    await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByText('Esta conta é de funcionário. Use a entrada da empresa.')).toBeInTheDocument()
    expect(localStorage.getItem('acme.sessao')).toBeNull()
  })
})
