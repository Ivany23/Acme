import { describe, expect, it } from 'vitest'
import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import Materiais from '../../src/paginas/Materiais.jsx'
import { AvisosProvider } from '../../src/componentes/ui.jsx'
import { servidorFalso, sessaoActiva } from '../servidorFalso.js'

function renderPagina() {
  return render(
    <MemoryRouter>
      <AvisosProvider><Materiais /></AvisosProvider>
    </MemoryRouter>,
  )
}

// Testes de INTEGRAÇÃO: página de materiais com a API simulada
describe('página de materiais', () => {
  it('lista os materiais vindos da API', async () => {
    sessaoActiva()
    servidorFalso({ 'GET /materiais': () => [{ id: 1, nome: 'Madeira', unidadeMedida: 'kg', custoUnitario: 50 }] })
    renderPagina()

    expect(await screen.findByText('Madeira')).toBeInTheDocument()
    expect(screen.getByText('50,00 MT')).toBeInTheDocument()
  })

  it('cria um material e actualiza a lista', async () => {
    sessaoActiva()
    const materiais = []
    const { chamadas } = servidorFalso({
      'GET /materiais': () => materiais,
      'POST /materiais': (corpo) => {
        const novo = { id: 2, ...corpo }
        materiais.push(novo)
        return [201, novo]
      },
    })
    renderPagina()
    expect(await screen.findByText('Nenhum material registado.')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Novo material' }))
    const modal = screen.getByRole('dialog')
    await userEvent.type(within(modal).getByLabelText('Nome'), 'Verniz')
    await userEvent.type(within(modal).getByLabelText(/Unidade de medida/), 'litro')
    await userEvent.type(within(modal).getByLabelText('Custo unitário (MT)'), '300')
    await userEvent.click(within(modal).getByRole('button', { name: 'Guardar' }))

    expect(await screen.findByText('Verniz')).toBeInTheDocument()
    expect(chamadas.find((c) => c.metodo === 'POST').corpo)
      .toEqual({ nome: 'Verniz', unidadeMedida: 'litro', custoUnitario: 300 })
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('mostra o erro da API quando não é possível apagar', async () => {
    sessaoActiva()
    servidorFalso({
      'GET /materiais': () => [{ id: 1, nome: 'Madeira', unidadeMedida: 'kg', custoUnitario: 50 }],
      'DELETE /materiais/1': () => [409, { mensagem: 'O material já foi usado em registos de produção' }],
    })
    window.confirm = () => true
    renderPagina()

    await userEvent.click(await screen.findByRole('button', { name: 'Apagar' }))

    expect(await screen.findByText('O material já foi usado em registos de produção')).toBeInTheDocument()
  })
})
