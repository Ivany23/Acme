// O que cada perfil pode fazer no ecrã. A API aplica as mesmas regras; aqui serve só
// para mostrar ao utilizador apenas os menus e botões que ele pode usar.

const A = 'ADMINISTRADOR'
const D = 'DIRECTOR_GERAL'
const C = 'CONTADOR'
const S = 'SUPERVISOR'
const V = 'GERENTE_VENDAS'
const R = 'ARTESAO'
const FUNCIONARIOS = [A, D, C, S, V, R]

export const PAPEIS = {
  ADMINISTRADOR: 'Administrador',
  DIRECTOR_GERAL: 'Director-Geral',
  CONTADOR: 'Contador',
  SUPERVISOR: 'Supervisor de Turno',
  GERENTE_VENDAS: 'Gerente de Vendas',
  ARTESAO: 'Artesão',
  CLIENTE: 'Cliente',
}

const PERMISSOES = {
  painel: FUNCIONARIOS,
  'encomendas.ver': FUNCIONARIOS,
  'encomendas.criar': [A, D, V],
  'encomendas.pagar': [A, D, C, V],
  'encomendas.estado': [A, D, S, V],
  'encomendas.apagar': [A, D, V],
  'producao.ver': FUNCIONARIOS,
  'producao.registar': [A, S, R],
  'ponto.ver': FUNCIONARIOS,
  'ponto.registar': [A, S, R],
  'turnos.ver': FUNCIONARIOS,
  'turnos.alterar': [A, D, S],
  'relatorios.ver': [A, D, C, S, V],
  'pecas.ver': FUNCIONARIOS,
  'pecas.alterar': [A, D, C],
  'materiais.ver': FUNCIONARIOS,
  'materiais.alterar': [A, D, C],
  'clientes.ver': [A, D, C, S, V],
  'clientes.alterar': [A, D, V],
  'funcionarios.ver': [A, D, C, S],
  'funcionarios.alterar': [A, D],
  utilizadores: [A],
}

export function pode(papel, accao) {
  return Boolean(papel && PERMISSOES[accao]?.includes(papel))
}

export function eCliente(sessao) {
  return sessao?.utilizador?.papel === 'CLIENTE'
}
