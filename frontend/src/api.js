// Cliente HTTP da API Acme. Junta o token de sessão e transforma os erros em mensagens legíveis.

export const API_URL = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '')
const CHAVE_SESSAO = 'acme.sessao'

// O Render (plano gratuito) pode demorar ~1 minuto a acordar
const TEMPO_LIMITE_MS = 90000

export class ErroApi extends Error {
  constructor(mensagem, status = 0, campos = null) {
    super(mensagem)
    this.status = status
    this.campos = campos
  }
}

export function lerSessao() {
  try {
    return JSON.parse(localStorage.getItem(CHAVE_SESSAO)) || null
  } catch {
    return null
  }
}

export function guardarSessao(sessao) {
  if (sessao) localStorage.setItem(CHAVE_SESSAO, JSON.stringify(sessao))
  else localStorage.removeItem(CHAVE_SESSAO)
}

export async function pedido(caminho, { metodo = 'GET', corpo, sinal } = {}) {
  const cabecalhos = { Accept: 'application/json' }
  const sessao = lerSessao()
  if (sessao?.token) cabecalhos.Authorization = `Bearer ${sessao.token}`
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'

  const controlo = new AbortController()
  const temporizador = setTimeout(() => controlo.abort(), TEMPO_LIMITE_MS)
  if (sinal) sinal.addEventListener('abort', () => controlo.abort())

  let resposta
  try {
    resposta = await fetch(API_URL + caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
      signal: controlo.signal,
    })
  } catch {
    throw new ErroApi('Não foi possível contactar o servidor. Verifique a ligação e tente de novo.')
  } finally {
    clearTimeout(temporizador)
  }

  const texto = await resposta.text()
  let dados = null
  if (texto) {
    try {
      dados = JSON.parse(texto)
    } catch {
      dados = null
    }
  }

  if (!resposta.ok) {
    if (resposta.status === 401 && caminho !== '/auth/login') {
      guardarSessao(null)
      window.dispatchEvent(new Event('acme:sessao-expirada'))
    }
    const mensagem = dados?.mensagem || `Erro ${resposta.status} ao comunicar com o servidor`
    throw new ErroApi(mensagem, resposta.status, dados?.campos || null)
  }
  return dados
}

export const api = {
  get: (caminho, opcoes) => pedido(caminho, opcoes),
  post: (caminho, corpo) => pedido(caminho, { metodo: 'POST', corpo: corpo ?? {} }),
  put: (caminho, corpo) => pedido(caminho, { metodo: 'PUT', corpo }),
  patch: (caminho, corpo) => pedido(caminho, { metodo: 'PATCH', corpo }),
  apagar: (caminho) => pedido(caminho, { metodo: 'DELETE' }),
}

/** Converte a mensagem de um erro (incluindo os campos inválidos) em texto. */
export function textoErro(erro) {
  if (!erro) return ''
  if (erro.campos) {
    return `${erro.message}: ${Object.values(erro.campos).join('; ')}`
  }
  return erro.message || String(erro)
}
