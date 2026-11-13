import { useState } from 'react'
import { api } from '../api.js'
import { Cabecalho, Campo, Conteudo, Modal, useAccao, useDados } from '../componentes/ui.jsx'
import { data as formatarData, hoje, hora, minutos } from '../util.js'

function NovoPonto({ dia, fechar, criado }) {
  const base = useDados(async () => {
    const [artesaos, turnos] = await Promise.all([api.get('/funcionarios/artesaos'), api.get('/turnos')])
    return { artesaos, turnos }
  })
  const { executar, ocupado } = useAccao()
  const [form, setForm] = useState({ artesaoId: '', turnoId: '', data: dia, horaEntrada: '', horaSaida: '' })
  const alterar = (campo, valor) => setForm((f) => ({ ...f, [campo]: valor }))
  const artesaoId = form.artesaoId || base.dados?.artesaos[0]?.id || ''

  async function submeter(e) {
    e.preventDefault()
    const r = await executar(() => api.post('/ponto', {
      artesaoId: Number(artesaoId),
      turnoId: form.turnoId ? Number(form.turnoId) : null,
      data: form.data,
      horaEntrada: form.horaEntrada,
      horaSaida: form.horaSaida,
    }), 'Ponto registado')
    if (r.ok) criado()
  }

  return (
    <Modal titulo="Registar ponto" aberto fechar={fechar}>
      <Conteudo {...base} vazio={base.dados?.artesaos.length === 0} textoVazio="Registe artesãos em Funcionários.">
        <form onSubmit={submeter}>
          <div className="grelha-form">
            <Campo rotulo="Artesão" largura={2}>
              <select value={artesaoId} onChange={(e) => alterar('artesaoId', e.target.value)} required>
                {base.dados?.artesaos.map((a) => <option key={a.id} value={a.id}>{a.nome}</option>)}
              </select>
            </Campo>
            <Campo rotulo="Turno" largura={2} ajuda="Se não escolher, é usado o turno alocado nesse dia.">
              <select value={form.turnoId} onChange={(e) => alterar('turnoId', e.target.value)}>
                <option value="">Turno alocado</option>
                {base.dados?.turnos.map((t) => <option key={t.id} value={t.id}>{t.denominacao}</option>)}
              </select>
            </Campo>
            <Campo rotulo="Data">
              <input type="date" value={form.data} onChange={(e) => alterar('data', e.target.value)} required />
            </Campo>
            <Campo rotulo="Entrada">
              <input type="time" value={form.horaEntrada} onChange={(e) => alterar('horaEntrada', e.target.value)} required />
            </Campo>
            <Campo rotulo="Saída">
              <input type="time" value={form.horaSaida} onChange={(e) => alterar('horaSaida', e.target.value)} required />
            </Campo>
          </div>
          <div className="modal-rodape">
            <button type="button" className="botao" onClick={fechar}>Cancelar</button>
            <button type="submit" className="botao principal" disabled={ocupado}>{ocupado ? 'A registar…' : 'Registar'}</button>
          </div>
        </form>
      </Conteudo>
    </Modal>
  )
}

export default function Ponto() {
  const [dia, setDia] = useState(hoje())
  const lista = useDados(() => api.get(`/ponto?data=${dia}`), [dia])
  const { executar, ocupado } = useAccao()
  const [novo, setNovo] = useState(false)

  async function apagar(p) {
    if (!window.confirm(`Apagar o ponto de ${p.artesaoNome}?`)) return
    const r = await executar(() => api.apagar(`/ponto/${p.id}`), 'Registo de ponto apagado')
    if (r.ok) lista.recarregar()
  }

  const totalExtra = lista.dados?.reduce((s, p) => s + p.minutosExtra, 0) ?? 0

  return (
    <>
      <Cabecalho titulo="Registo de ponto" descricao="Entradas e saídas dos artesãos. As horas extra são calculadas pela duração do turno.">
        <input type="date" value={dia} onChange={(e) => setDia(e.target.value)} aria-label="Dia" />
        <button type="button" className="botao principal" onClick={() => setNovo(true)}>Registar ponto</button>
      </Cabecalho>

      <section className="cartao">
        <div className="cartao-topo">
          <h2>{formatarData(dia)}</h2>
          {totalExtra > 0 && <span className="etiqueta laranja">{minutos(totalExtra)} de horas extra</span>}
        </div>
        <Conteudo {...lista} vazio={lista.dados?.length === 0} textoVazio="Nenhum registo de ponto neste dia.">
          <div className="tabela-envolvente">
            <table className="tabela">
              <thead>
                <tr><th>Artesão</th><th>Turno</th><th>Entrada</th><th>Saída</th><th>Trabalhado</th><th>Horas extra</th><th className="accoes" /></tr>
              </thead>
              <tbody>
                {lista.dados?.map((p) => (
                  <tr key={p.id}>
                    <td><strong>{p.artesaoNome}</strong></td>
                    <td>{p.turnoDenominacao}</td>
                    <td>{hora(p.horaEntrada)}</td>
                    <td>{hora(p.horaSaida)}</td>
                    <td>{minutos(p.minutosTrabalhados)}</td>
                    <td>{p.minutosExtra > 0 ? <span className="etiqueta laranja">{minutos(p.minutosExtra)}</span> : '—'}</td>
                    <td className="accoes">
                      <button type="button" className="botao pequeno perigo" disabled={ocupado} onClick={() => apagar(p)}>Apagar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Conteudo>
      </section>

      {novo && <NovoPonto dia={dia} fechar={() => setNovo(false)} criado={() => { setNovo(false); lista.recarregar() }} />}
    </>
  )
}
