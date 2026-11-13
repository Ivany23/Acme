import { describe, expect, it, vi } from 'vitest'
import { api, ErroApi, lerSessao, textoErro } from '../../src/api.js'
import { servidorFalso, sessaoActiva } from '../servidorFalso.js'

// Testes de SERVIÇOS: cliente HTTP que o frontend usa para falar com a API
describe('cliente da API', () => {
  it('envia o token de sessão no cabeçalho Authorization', async () => {
    sessaoActiva()
    const { chamadas } = servidorFalso({ 'GET /materiais': () => [] })

    await api.get('/materiais')

    expect(chamadas[0].cabecalhos.Authorization).toBe('Bearer token-de-teste')
  })

  it('envia o corpo em JSON nos POST', async () => {
    const { chamadas } = servidorFalso({ 'POST /clientes': (corpo) => [201, { id: 7, ...corpo }] })

    const criado = await api.post('/clientes', { nome: 'Ana' })

    expect(chamadas[0].corpo).toEqual({ nome: 'Ana' })
    expect(criado.id).toBe(7)
  })

  it('transforma erros de validação numa mensagem legível', async () => {
    servidorFalso({
      'POST /materiais': () => [400, { mensagem: 'Dados inválidos', campos: { nome: 'O nome é obrigatório' } }],
    })

    const erro = await api.post('/materiais', {}).catch((e) => e)

    expect(erro).toBeInstanceOf(ErroApi)
    expect(erro.status).toBe(400)
    expect(textoErro(erro)).toBe('Dados inválidos: O nome é obrigatório')
  })

  it('termina a sessão quando a API responde 401', async () => {
    sessaoActiva()
    servidorFalso({ 'GET /encomendas': () => [401, { mensagem: 'Sessão inválida ou expirada' }] })
    const ouvinte = vi.fn()
    window.addEventListener('acme:sessao-expirada', ouvinte)

    await expect(api.get('/encomendas')).rejects.toThrow('Sessão inválida')

    expect(lerSessao()).toBeNull()
    expect(ouvinte).toHaveBeenCalled()
    window.removeEventListener('acme:sessao-expirada', ouvinte)
  })

  it('explica quando o servidor não responde', async () => {
    vi.stubGlobal('fetch', vi.fn(() => Promise.reject(new TypeError('Failed to fetch'))))
    await expect(api.get('/estado')).rejects.toThrow('Não foi possível contactar o servidor')
  })

  it('aceita respostas sem corpo (204)', async () => {
    servidorFalso({ 'DELETE /materiais/3': () => undefined })
    await expect(api.apagar('/materiais/3')).resolves.toBeNull()
  })
})
