import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from '../../src/App.jsx'
import { SessaoProvider } from '../../src/sessao.jsx'
import { AvisosProvider } from '../../src/componentes/ui.jsx'
import { servidorFalso, sessaoActiva } from '../servidorFalso.js'

const material = { id: 1, nome: 'Madeira', unidadeMedida: 'kg', custoUnitario: 50 }
const peca = {
  id: 1, nome: 'Cadeira', dimensoes: '45x45x90 cm', precoUnitario: 1000, custoMateriaisPorUnidade: 250,
  materiais: [{ materialId: 1, materialNome: 'Madeira', unidadeMedida: 'kg', quantidadePorUnidade: 5, custo: 250 }],
  etapas: [{ id: 1, descricao: 'Corte', ordem: 1 }],
}
const encomenda = {
  id: 1, clienteId: 1, clienteNome: 'Ana Machava', pecaId: 1, pecaNome: 'Cadeira', quantidade: 2,
  estado: 'EM_PRODUCAO', paga: true, custosIndirectos: 100, custoEstimado: 3120, dataCriacao: '2026-11-13T09:00:00',
}
const relatorio = {
  id: 1, encomendaId: 1, clienteNome: 'Ana Machava', pecaNome: 'Cadeira', quantidade: 2,
  minutosPorEtapa: { Corte: 120 }, minutosPorArtesao: { 'João Mondlane': 120 }, materiaisConsumidos: { Madeira: 10 },
  minutosExtraPorArtesao: { 'João Mondlane': 60 }, retrabalhos: 0, custoEstimado: 3120, custoReal: 600, desvio: 2520,
  dataGeracao: '2026-11-13T12:00:00',
}

/** API falsa completa, como o backend real a devolveria. */
function apiCompleta() {
  return servidorFalso({
    'GET /dashboard': () => ({
      totalClientes: 1, totalPecas: 1, totalMateriais: 1, totalFuncionarios: 1, totalArtesaos: 1, totalTurnos: 1,
      encomendasPorEstado: { PENDENTE: 0, EM_PRODUCAO: 1, CONCLUIDA: 0, CANCELADA: 0 },
      analise: { totalRelatorios: 1, custoMedioPorEncomenda: 600, totalMinutosExtra: 60, desvioMedioOrcamento: 2520, totalRetrabalhos: 0 },
    }),
    'GET /encomendas': () => [encomenda],
    'GET /producao': () => [{
      id: 1, artesaoId: 1, artesaoNome: 'João Mondlane', encomendaId: 1, etapaId: 1, etapaDescricao: 'Corte',
      turnoId: 1, turnoDenominacao: 'Matutino', data: '2026-11-13', horaInicio: '08:00:00', horaFim: '10:00:00',
      tempoGastoMinutos: 120, materiaisUsados: [], custoMateriais: 0, custosIndirectos: 0, retrabalho: false,
    }],
    'GET /ponto': () => [{
      id: 1, artesaoId: 1, artesaoNome: 'João Mondlane', turnoId: 1, turnoDenominacao: 'Matutino', data: '2026-11-13',
      horaEntrada: '07:00:00', horaSaida: '16:00:00', minutosTrabalhados: 540, minutosExtra: 60,
    }],
    'GET /turnos': () => [{ id: 1, denominacao: 'Matutino', horaEntrada: '07:00:00', horaSaida: '15:00:00', duracaoMinutos: 480 }],
    'GET /funcionarios/artesaos': () => [{ id: 1, tipo: 'ARTESAO', nome: 'João Mondlane', cargo: 'Artesão', especialidade: 'Carpintaria' }],
    'GET /funcionarios': () => [{ id: 1, tipo: 'ARTESAO', nome: 'João Mondlane', cargo: 'Artesão', especialidade: 'Carpintaria' }],
    'GET /alocacoes': () => [{ id: 1, artesaoId: 1, artesaoNome: 'João Mondlane', turnoId: 1, turnoDenominacao: 'Matutino', data: '2026-11-13' }],
    'GET /relatorios': () => [relatorio],
    'GET /relatorios/analise': () => ({ totalRelatorios: 1, custoMedioPorEncomenda: 600, totalMinutosExtra: 60, desvioMedioOrcamento: 2520, totalRetrabalhos: 0 }),
    'GET /pecas': () => [peca],
    'GET /materiais': () => [material],
    'GET /clientes': () => [{ id: 1, nome: 'Ana Machava', email: 'ana@mail.com', telefone: '845556677' }],
    'GET /utilizadores': () => [{
      id: 1, login: 'admin', nome: 'Administrador', papel: 'ADMINISTRADOR', papelDescricao: 'Administrador',
      funcionarioId: null, funcionarioNome: null, clienteId: null, clienteNome: null,
    }],
  })
}

function renderApp(rota) {
  return render(
    <MemoryRouter initialEntries={[rota]}>
      <SessaoProvider><AvisosProvider><App /></AvisosProvider></SessaoProvider>
    </MemoryRouter>,
  )
}

describe('menus por perfil', () => {
  it('o artesão só vê o que lhe compete e regista apenas em seu nome', async () => {
    sessaoActiva({ id: 7, login: 'artesao', nome: 'João Mondlane', papel: 'ARTESAO', funcionarioId: 1 })
    apiCompleta()
    renderApp('/')
    await screen.findByRole('heading', { name: 'Olá, João Mondlane' })

    const menu = screen.getByRole('navigation')
    for (const visivel of ['Encomendas', 'Registo de produção', 'Registo de ponto', 'Peças']) {
      expect(within(menu).getByRole('link', { name: visivel })).toBeInTheDocument()
    }
    for (const escondido of ['Relatórios e análise', 'Clientes', 'Funcionários', 'Utilizadores']) {
      expect(within(menu).queryByRole('link', { name: escondido })).not.toBeInTheDocument()
    }

    // na página de materiais não há botões para alterar custos
    await userEvent.click(within(menu).getByRole('link', { name: 'Materiais' }))
    await screen.findByText('Madeira')
    expect(screen.queryByRole('button', { name: 'Novo material' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Apagar' })).not.toBeInTheDocument()
  })

  it('o contador não vê a gestão de utilizadores nem por endereço directo', async () => {
    sessaoActiva({ id: 3, login: 'contador', nome: 'Rita', papel: 'CONTADOR' })
    apiCompleta()
    renderApp('/utilizadores')
    expect(await screen.findByRole('heading', { name: 'Sem acesso' })).toBeInTheDocument()
  })

  it('um cliente que abre a área da empresa é levado para a área de cliente', async () => {
    sessaoActiva({ id: 9, login: 'ana@mail.com', nome: 'Ana', papel: 'CLIENTE', clienteId: 1 })
    servidorFalso({
      'GET /portal/encomendas': () => [],
      'GET /portal/notificacoes': () => [],
      'GET /portal/pecas': () => [],
    })
    renderApp('/materiais')
    expect(await screen.findByRole('heading', { name: 'Olá, Ana' })).toBeInTheDocument()
    expect(screen.getByText('Área de cliente')).toBeInTheDocument()
  })
})

// Teste de SISTEMA do frontend: a aplicação inteira, navegando pelo menu como um utilizador
describe('navegação pela aplicação', () => {
  it('abre todas as páginas do menu sem erros', async () => {
    sessaoActiva()
    apiCompleta()
    render(
      <MemoryRouter initialEntries={['/']}>
        <SessaoProvider><AvisosProvider><App /></AvisosProvider></SessaoProvider>
      </MemoryRouter>,
    )
    expect(await screen.findByRole('heading', { name: 'Olá, Administrador' })).toBeInTheDocument()

    const menu = screen.getByRole('navigation')
    const paginas = [
      ['Encomendas', 'Encomendas', 'Ana Machava'],
      ['Registo de produção', 'Registo de produção', 'Corte'],
      ['Registo de ponto', 'Registo de ponto', '1h'],
      ['Turnos e alocação', 'Turnos e alocação', 'Alocação de artesãos'],
      ['Relatórios e análise', 'Relatórios e análise', '#1'],
      ['Utilizadores', 'Utilizadores', 'admin'],
      ['Peças', 'Peças', '45x45x90 cm'],
      ['Materiais', 'Materiais', 'Madeira'],
      ['Clientes', 'Clientes', 'ana@mail.com'],
      ['Funcionários', 'Funcionários', 'Carpintaria'],
    ]
    for (const [link, titulo, conteudo] of paginas) {
      await userEvent.click(within(menu).getByRole('link', { name: link }))
      expect(await screen.findByRole('heading', { level: 1, name: titulo })).toBeInTheDocument()
      expect((await screen.findAllByText(conteudo)).length).toBeGreaterThan(0)
    }
  })

  it('o botão Sair termina a sessão e volta ao login', async () => {
    sessaoActiva()
    apiCompleta()
    render(
      <MemoryRouter initialEntries={['/materiais']}>
        <SessaoProvider><AvisosProvider><App /></AvisosProvider></SessaoProvider>
      </MemoryRouter>,
    )
    await screen.findByText('Madeira')
    await userEvent.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByRole('heading', { name: 'Entrar' })).toBeInTheDocument()
    expect(localStorage.getItem('acme.sessao')).toBeNull()
  })

  it('mostra o relatório com o custo real e as horas extra', async () => {
    sessaoActiva()
    apiCompleta()
    render(
      <MemoryRouter initialEntries={['/relatorios']}>
        <SessaoProvider><AvisosProvider><App /></AvisosProvider></SessaoProvider>
      </MemoryRouter>,
    )
    await userEvent.click(await screen.findByRole('button', { name: 'Ver' }))
    const modal = screen.getByRole('dialog')
    expect(within(modal).getByText('Tempo por etapa')).toBeInTheDocument()
    expect(within(modal).getByText('600,00 MT')).toBeInTheDocument()
    expect(within(modal).getAllByText('1h').length).toBeGreaterThan(0)
  })
})
