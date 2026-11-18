import CrudSimples from '../componentes/CrudSimples.jsx'
import { usePode } from '../sessao.jsx'
import { dinheiro } from '../util.js'

export default function Materiais() {
  const pode = usePode()
  return (
    <CrudSimples
      titulo="Materiais"
      descricao="Matéria-prima usada no fabrico e o seu custo por unidade de medida."
      podeAlterar={pode('materiais.alterar')}
      recurso="materiais"
      nomeSingular="Material"
      vazio="Nenhum material registado."
      campos={[
        { nome: 'nome', rotulo: 'Nome', obrigatorio: true, largura: 2 },
        { nome: 'unidadeMedida', rotulo: 'Unidade de medida', obrigatorio: true, ajuda: 'ex.: kg, litro, unidade' },
        { nome: 'custoUnitario', rotulo: 'Custo unitário (MT)', tipo: 'number', minimo: 0, obrigatorio: true },
      ]}
      colunas={[
        { rotulo: 'Nome', valor: (m) => <strong>{m.nome}</strong> },
        { rotulo: 'Unidade', valor: (m) => m.unidadeMedida },
        { rotulo: 'Custo unitário', valor: (m) => dinheiro(m.custoUnitario), classe: 'numero' },
      ]}
    />
  )
}
