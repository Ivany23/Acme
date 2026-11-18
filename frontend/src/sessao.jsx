import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, guardarSessao, lerSessao } from './api.js'
import { pode as podeFazer } from './permissoes.js'

const SessaoContexto = createContext(null)

export function SessaoProvider({ children }) {
  // Sessões guardadas por versões antigas (sem perfil) são descartadas: obrigam a entrar de novo
  const [sessao, setSessao] = useState(() => {
    const guardada = lerSessao()
    if (guardada && !guardada.utilizador?.papel) {
      guardarSessao(null)
      return null
    }
    return guardada
  })

  const iniciar = useCallback((nova) => {
    guardarSessao(nova)
    setSessao(nova)
    return nova
  }, [])

  const entrar = useCallback(
    async (login, password) => iniciar(await api.post('/auth/login', { login, password })),
    [iniciar],
  )

  /** Caso de uso "Registar Conta de Cliente": cria a conta e entra logo. */
  const registarCliente = useCallback(
    async (dados) => iniciar(await api.post('/portal/registar', dados)),
    [iniciar],
  )

  const sair = useCallback(() => {
    guardarSessao(null)
    setSessao(null)
  }, [])

  useEffect(() => {
    const expirou = () => setSessao(null)
    window.addEventListener('acme:sessao-expirada', expirou)
    return () => window.removeEventListener('acme:sessao-expirada', expirou)
  }, [])

  const valor = useMemo(() => ({ sessao, entrar, registarCliente, sair }), [sessao, entrar, registarCliente, sair])
  return <SessaoContexto.Provider value={valor}>{children}</SessaoContexto.Provider>
}

export function useSessao() {
  return useContext(SessaoContexto)
}

/** Devolve uma função pode(accao) para o utilizador da sessão actual. */
export function usePode() {
  const { sessao } = useSessao()
  const papel = sessao?.utilizador?.papel
  return useCallback((accao) => podeFazer(papel, accao), [papel])
}
