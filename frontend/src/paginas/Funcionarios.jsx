import CrudSimples from '../componentes/CrudSimples.jsx'

const eArtesao = (f) => f.tipo === 'ARTESAO'

export default function Funcionarios() {
  return (
    <CrudSimples
      titulo="Funcionários"
      descricao="Pessoal contratado. Os artesãos são alocados aos turnos e registam produção e ponto."
      recurso="funcionarios"
      nomeSingular="Funcionário"
      vazio="Nenhum funcionário registado."
      campos={[
        {
          nome: 'tipo', rotulo: 'Tipo', tipo: 'select', obrigatorio: true, inicial: 'ARTESAO', soNaCriacao: true,
          opcoes: [{ valor: 'ARTESAO', texto: 'Artesão' }, { valor: 'FUNCIONARIO', texto: 'Outro funcionário' }],
        },
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true },
        { nome: 'especialidade', rotulo: 'Especialidade', mostrar: eArtesao, ajuda: 'ex.: carpintaria, acabamentos' },
        { nome: 'cargo', rotulo: 'Cargo', obrigatorio: true, mostrar: (f) => !eArtesao(f) },
        { nome: 'telefone', rotulo: 'Telefone' },
        { nome: 'endereco', rotulo: 'Endereço' },
      ]}
      colunas={[
        { rotulo: 'Nome', valor: (f) => <strong>{f.nome}</strong> },
        { rotulo: 'Cargo', valor: (f) => <span className={eArtesao(f) ? 'etiqueta verde' : 'etiqueta'}>{f.cargo}</span> },
        { rotulo: 'Especialidade', valor: (f) => f.especialidade || '—' },
        { rotulo: 'Telefone', valor: (f) => f.telefone || '—' },
        { rotulo: 'Endereço', valor: (f) => f.endereco || '—' },
      ]}
    />
  )
}
