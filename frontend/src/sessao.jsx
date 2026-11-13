import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { api, guardarSessao, lerSessao } from './api.js'

const SessaoContexto = createContext(null)

export function SessaoProvider({ children }) {
  const [sessao, setSessao] = useState(() => lerSessao())

  const entrar = useCallback(async (login, password) => {
    const nova = await api.post('/auth/login', { login, password })
    guardarSessao(nova)
    setSessao(nova)
    return nova
  }, [])

  const sair = useCallback(() => {
    guardarSessao(null)
    setSessao(null)
  }, [])

  useEffect(() => {
    const expirou = () => setSessao(null)
    window.addEventListener('acme:sessao-expirada', expirou)
    return () => window.removeEventListener('acme:sessao-expirada', expirou)
  }, [])

  const valor = useMemo(() => ({ sessao, entrar, sair }), [sessao, entrar, sair])
  return <SessaoContexto.Provider value={valor}>{children}</SessaoContexto.Provider>
}

export function useSessao() {
  return useContext(SessaoContexto)
}
