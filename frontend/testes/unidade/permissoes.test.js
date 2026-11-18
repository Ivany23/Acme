import { describe, expect, it } from 'vitest'
import { eCliente, pode } from '../../src/permissoes.js'

// Testes de UNIDADE: regras de permissão usadas para mostrar/esconder menus e botões
describe('permissões por perfil', () => {
  it('o administrador pode tudo da gestão', () => {
    for (const accao of ['materiais.alterar', 'encomendas.pagar', 'utilizadores', 'relatorios.ver']) {
      expect(pode('ADMINISTRADOR', accao)).toBe(true)
    }
  })

  it('o artesão regista produção e ponto, mas não altera custos nem vê relatórios', () => {
    expect(pode('ARTESAO', 'producao.registar')).toBe(true)
    expect(pode('ARTESAO', 'ponto.registar')).toBe(true)
    expect(pode('ARTESAO', 'materiais.alterar')).toBe(false)
    expect(pode('ARTESAO', 'relatorios.ver')).toBe(false)
  })

  it('o contador gere custos e pagamentos, mas não utilizadores', () => {
    expect(pode('CONTADOR', 'materiais.alterar')).toBe(true)
    expect(pode('CONTADOR', 'encomendas.pagar')).toBe(true)
    expect(pode('CONTADOR', 'utilizadores')).toBe(false)
  })

  it('o gerente de vendas trata de clientes e encomendas e consulta o histórico', () => {
    expect(pode('GERENTE_VENDAS', 'clientes.alterar')).toBe(true)
    expect(pode('GERENTE_VENDAS', 'encomendas.criar')).toBe(true)
    expect(pode('GERENTE_VENDAS', 'relatorios.ver')).toBe(true)
    expect(pode('GERENTE_VENDAS', 'turnos.alterar')).toBe(false)
  })

  it('o cliente não tem acesso a nada da gestão', () => {
    expect(pode('CLIENTE', 'painel')).toBe(false)
    expect(pode('CLIENTE', 'encomendas.ver')).toBe(false)
  })

  it('sem perfil ou acção desconhecida, nega', () => {
    expect(pode(undefined, 'painel')).toBe(false)
    expect(pode('ADMINISTRADOR', 'accao.inexistente')).toBe(false)
  })

  it('identifica sessões de cliente', () => {
    expect(eCliente({ utilizador: { papel: 'CLIENTE' } })).toBe(true)
    expect(eCliente({ utilizador: { papel: 'CONTADOR' } })).toBe(false)
    expect(eCliente(null)).toBe(false)
  })
})
