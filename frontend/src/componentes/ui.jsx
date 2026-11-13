import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { textoErro } from '../api.js'
import { ESTADOS } from '../util.js'

/** Carrega dados da API e permite recarregar. */
export function useDados(carregar, dependencias = []) {
  const [estado, setEstado] = useState({ dados: null, erro: null, carregando: true })
  const versao = useRef(0)

  const recarregar = useCallback(async () => {
    const minha = ++versao.current
    setEstado((e) => ({ ...e, carregando: true, erro: null }))
    try {
      const dados = await carregar()
      if (minha === versao.current) setEstado({ dados, erro: null, carregando: false })
    } catch (erro) {
      if (minha === versao.current) setEstado((e) => ({ ...e, erro, carregando: false }))
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencias)

  useEffect(() => {
    recarregar()
  }, [recarregar])

  return { ...estado, recarregar }
}

// ---------- Avisos rápidos (toast) ----------

const AvisosContexto = createContext({ avisar: () => {} })

export function AvisosProvider({ children }) {
  const [avisos, setAvisos] = useState([])

  const avisar = useCallback((texto, tipo = 'ok') => {
    const id = Math.random().toString(36).slice(2)
    setAvisos((a) => [...a, { id, texto, tipo }])
    setTimeout(() => setAvisos((a) => a.filter((x) => x.id !== id)), tipo === 'erro' ? 6000 : 3000)
  }, [])

  return (
    <AvisosContexto.Provider value={{ avisar }}>
      {children}
      <div className="avisos" role="status">
        {avisos.map((a) => (
          <div key={a.id} className={`aviso-flutuante ${a.tipo}`}>{a.texto}</div>
        ))}
      </div>
    </AvisosContexto.Provider>
  )
}

export function useAvisos() {
  return useContext(AvisosContexto)
}

/** Executa uma acção, mostra o resultado e evita cliques repetidos. */
export function useAccao() {
  const { avisar } = useAvisos()
  const [ocupado, setOcupado] = useState(false)

  const executar = useCallback(async (accao, mensagemSucesso) => {
    setOcupado(true)
    try {
      const r = await accao()
      if (mensagemSucesso) avisar(mensagemSucesso, 'ok')
      return { ok: true, resultado: r }
    } catch (erro) {
      avisar(textoErro(erro), 'erro')
      return { ok: false, erro }
    } finally {
      setOcupado(false)
    }
  }, [avisar])

  return { executar, ocupado }
}

// ---------- Estrutura ----------

export function Cabecalho({ titulo, descricao, children }) {
  return (
    <header className="cabecalho">
      <div>
        <h1>{titulo}</h1>
        {descricao && <p className="descricao">{descricao}</p>}
      </div>
      {children && <div className="cabecalho-accoes">{children}</div>}
    </header>
  )
}

export function Modal({ titulo, aberto, fechar, children, largo = false }) {
  useEffect(() => {
    if (!aberto) return undefined
    const tecla = (e) => e.key === 'Escape' && fechar()
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [aberto, fechar])

  if (!aberto) return null
  return (
    <div className="modal-fundo" onMouseDown={(e) => e.target === e.currentTarget && fechar()}>
      <div className={`modal ${largo ? 'largo' : ''}`} role="dialog" aria-modal="true" aria-label={titulo}>
        <div className="modal-topo">
          <h2>{titulo}</h2>
          <button type="button" className="botao-fechar" onClick={fechar} aria-label="Fechar">×</button>
        </div>
        {children}
      </div>
    </div>
  )
}

export function Campo({ rotulo, children, ajuda, largura }) {
  return (
    <label className="campo" style={largura ? { gridColumn: `span ${largura}` } : undefined}>
      <span className="campo-rotulo">{rotulo}</span>
      {children}
      {ajuda && <small className="campo-ajuda">{ajuda}</small>}
    </label>
  )
}

export function Mensagem({ tipo = 'info', children }) {
  return <div className={`mensagem ${tipo}`}>{children}</div>
}

export function Estado({ valor }) {
  return <span className={`estado estado-${String(valor).toLowerCase()}`}>{ESTADOS[valor] || valor}</span>
}

/** Mostra carregamento / erro / vazio antes do conteúdo. */
export function Conteudo({ carregando, erro, vazio, textoVazio = 'Ainda não há registos.', recarregar, children }) {
  if (carregando) return <div className="carregando">A carregar…</div>
  if (erro) {
    return (
      <Mensagem tipo="erro">
        {textoErro(erro)}{' '}
        {recarregar && <button type="button" className="botao ligacao" onClick={recarregar}>Tentar de novo</button>}
      </Mensagem>
    )
  }
  if (vazio) return <div className="vazio">{textoVazio}</div>
  return children
}
