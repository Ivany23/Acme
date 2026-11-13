import { vi } from 'vitest'

/**
 * Substitui o fetch por uma API falsa em memória.
 * rotas: { 'GET /materiais': (corpo, url) => [status, dados] | dados }
 */
export function servidorFalso(rotas) {
  const chamadas = []
  const fetchFalso = vi.fn(async (url, opcoes = {}) => {
    const metodo = opcoes.method || 'GET'
    const caminho = String(url).replace(/^.*\/api/, '').split('?')[0]
    const corpo = opcoes.body ? JSON.parse(opcoes.body) : undefined
    chamadas.push({ metodo, caminho, url: String(url), corpo, cabecalhos: opcoes.headers || {} })

    const rota = rotas[`${metodo} ${caminho}`]
    if (!rota) {
      return new Response(JSON.stringify({ mensagem: `Rota não simulada: ${metodo} ${caminho}` }), { status: 404 })
    }
    let r = await rota(corpo, String(url))
    let status = 200
    if (Array.isArray(r) && typeof r[0] === 'number' && r.length === 2) [status, r] = r
    return new Response(r === undefined ? '' : JSON.stringify(r), {
      status,
      headers: { 'Content-Type': 'application/json' },
    })
  })
  vi.stubGlobal('fetch', fetchFalso)
  return { chamadas, fetchFalso }
}

export function sessaoActiva() {
  localStorage.setItem('acme.sessao', JSON.stringify({
    token: 'token-de-teste',
    utilizador: { id: 1, login: 'admin', nome: 'Administrador' },
  }))
}
