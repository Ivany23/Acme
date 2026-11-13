import { useState } from 'react'
import CrudSimples from '../componentes/CrudSimples.jsx'
import { Conteudo, Modal, useDados } from '../componentes/ui.jsx'
import { api } from '../api.js'

function Notificacoes({ cliente, fechar }) {
  const lista = useDados(() => api.get(`/clientes/${cliente.id}/notificacoes`), [cliente.id])
  return (
    <Modal titulo={`Notificações de ${cliente.nome}`} aberto fechar={fechar}>
      <Conteudo {...lista} vazio={lista.dados?.length === 0} textoVazio="Este cliente ainda não recebeu notificações.">
        <ol className="lista-notificacoes">
          {lista.dados?.map((n, i) => <li key={i}>{n}</li>)}
        </ol>
      </Conteudo>
    </Modal>
  )
}

export default function Clientes() {
  const [verNotificacoes, setVerNotificacoes] = useState(null)
  return (
    <>
      <CrudSimples
        titulo="Clientes"
        descricao="Quem encomenda peças. Cada cliente é avisado quando o estado das suas encomendas muda."
        recurso="clientes"
        nomeSingular="Cliente"
        vazio="Nenhum cliente registado."
        campos={[
          { nome: 'nome', rotulo: 'Nome', obrigatorio: true, largura: 2 },
          { nome: 'email', rotulo: 'Email', tipo: 'email' },
          { nome: 'telefone', rotulo: 'Telefone' },
        ]}
        colunas={[
          { rotulo: 'Nome', valor: (c) => <strong>{c.nome}</strong> },
          { rotulo: 'Email', valor: (c) => c.email || '—' },
          { rotulo: 'Telefone', valor: (c) => c.telefone || '—' },
        ]}
        extraLinha={(c) => (
          <button type="button" className="botao pequeno" onClick={() => setVerNotificacoes(c)}>Notificações</button>
        )}
      />
      {verNotificacoes && <Notificacoes cliente={verNotificacoes} fechar={() => setVerNotificacoes(null)} />}
    </>
  )
}
