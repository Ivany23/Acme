import { useState } from 'react'
import { api } from '../api.js'
import { Campo, Modal, useAccao } from './ui.jsx'

export default function AlterarPassword({ fechar }) {
  const { executar, ocupado } = useAccao()
  const [form, setForm] = useState({ actual: '', nova: '', confirmar: '' })
  const [erro, setErro] = useState('')
  const alterar = (c, v) => setForm((f) => ({ ...f, [c]: v }))

  async function submeter(e) {
    e.preventDefault()
    if (form.nova !== form.confirmar) {
      setErro('A confirmação não coincide com a nova password')
      return
    }
    setErro('')
    const r = await executar(() => api.put('/auth/password', { actual: form.actual, nova: form.nova }), 'Password alterada')
    if (r.ok) fechar()
  }

  return (
    <Modal titulo="Alterar password" aberto fechar={fechar}>
      <form onSubmit={submeter}>
        <div className="grelha-form">
          <Campo rotulo="Password actual" largura={4}>
            <input type="password" value={form.actual} onChange={(e) => alterar('actual', e.target.value)}
              autoComplete="current-password" required />
          </Campo>
          <Campo rotulo="Nova password" ajuda="Pelo menos 6 caracteres">
            <input type="password" minLength={6} value={form.nova} onChange={(e) => alterar('nova', e.target.value)}
              autoComplete="new-password" required />
          </Campo>
          <Campo rotulo="Confirmar nova password">
            <input type="password" minLength={6} value={form.confirmar} onChange={(e) => alterar('confirmar', e.target.value)}
              autoComplete="new-password" required />
          </Campo>
        </div>
        {erro && <div className="mensagem erro">{erro}</div>}
        <div className="modal-rodape">
          <button type="button" className="botao" onClick={fechar}>Cancelar</button>
          <button type="submit" className="botao principal" disabled={ocupado}>{ocupado ? 'A guardar…' : 'Alterar'}</button>
        </div>
      </form>
    </Modal>
  )
}
