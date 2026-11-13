package acme.servico;

import acme.modelo.EtapaProducao;
import acme.modelo.Material;
import acme.modelo.Peca;
import acme.repositorio.EncomendaRepository;
import acme.repositorio.MaterialRepository;
import acme.repositorio.PecaRepository;
import acme.repositorio.RegistoProducaoRepository;
import acme.web.dto.Pedidos.EtapaPedido;
import acme.web.dto.Pedidos.MaterialPedido;
import acme.web.dto.Pedidos.PecaPedido;
import acme.web.dto.Pedidos.QuantidadeMaterialPedido;
import acme.web.dto.Respostas.MaterialDTO;
import acme.web.dto.Respostas.PecaDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Materiais e peças (RF001). */
@Service
@Transactional
public class CatalogoService {

    private final MaterialRepository materiais;
    private final PecaRepository pecas;
    private final EncomendaRepository encomendas;
    private final RegistoProducaoRepository registos;

    public CatalogoService(MaterialRepository materiais, PecaRepository pecas, EncomendaRepository encomendas,
                           RegistoProducaoRepository registos) {
        this.materiais = materiais;
        this.pecas = pecas;
        this.encomendas = encomendas;
        this.registos = registos;
    }

    // ---------- Materiais ----------

    @Transactional(readOnly = true)
    public List<MaterialDTO> listarMateriais() {
        return materiais.findAllByOrderByNomeAsc().stream().map(MaterialDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public MaterialDTO obterMaterial(Long id) {
        return MaterialDTO.de(material(id));
    }

    public MaterialDTO criarMaterial(MaterialPedido p) {
        return MaterialDTO.de(materiais.save(new Material(p.nome(), p.unidadeMedida(), p.custoUnitario())));
    }

    public MaterialDTO actualizarMaterial(Long id, MaterialPedido p) {
        Material m = material(id);
        m.actualizar(p.nome(), p.unidadeMedida(), p.custoUnitario());
        return MaterialDTO.de(m);
    }

    public void apagarMaterial(Long id) {
        Material m = material(id);
        if (registos.existsByMateriaisUsadosMaterialId(id)) {
            throw new IllegalStateException("O material já foi usado em registos de produção e não pode ser apagado");
        }
        materiais.delete(m);
        materiais.flush();
    }

    public Material material(Long id) {
        return materiais.findById(id).orElseThrow(() -> new NaoEncontradoException("Material", id));
    }

    // ---------- Peças ----------

    @Transactional(readOnly = true)
    public List<PecaDTO> listarPecas() {
        return pecas.findAllByOrderByNomeAsc().stream().map(PecaDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public PecaDTO obterPeca(Long id) {
        return PecaDTO.de(peca(id));
    }

    public PecaDTO criarPeca(PecaPedido p) {
        Peca peca = new Peca(p.nome(), p.dimensoes(), p.precoUnitario());
        definirMateriais(peca, p.materiaisOuVazio());
        for (EtapaPedido e : p.etapasOuVazio()) {
            peca.adicionarEtapa(e.descricao(), e.ordem());
        }
        return PecaDTO.de(pecas.save(peca));
    }

    public PecaDTO actualizarPeca(Long id, PecaPedido p) {
        Peca peca = peca(id);
        peca.actualizar(p.nome(), p.dimensoes(), p.precoUnitario());
        definirMateriais(peca, p.materiaisOuVazio());

        // etapas: as que têm id são actualizadas, as sem id são novas, as que faltam são removidas
        Set<Long> manter = new HashSet<>();
        for (EtapaPedido e : p.etapasOuVazio()) {
            if (e.id() != null) {
                if (peca.obterEtapa(e.id()) == null) {
                    throw new IllegalArgumentException("A etapa " + e.id() + " não pertence a esta peça");
                }
                manter.add(e.id());
            }
        }
        for (EtapaProducao existente : peca.getEtapas()) {
            if (!manter.contains(existente.getId()) && registos.existsByEtapaId(existente.getId())) {
                throw new IllegalStateException("A etapa '" + existente.getDescricao()
                        + "' já tem registos de produção e não pode ser removida");
            }
        }
        peca.manterApenasEtapas(manter);
        for (EtapaPedido e : p.etapasOuVazio()) {
            if (e.id() != null) {
                peca.obterEtapa(e.id()).actualizar(e.descricao(), e.ordem());
            } else {
                peca.adicionarEtapa(e.descricao(), e.ordem());
            }
        }
        peca.ordenarEtapas();
        pecas.flush();
        return PecaDTO.de(peca);
    }

    public void apagarPeca(Long id) {
        Peca peca = peca(id);
        if (encomendas.existsByPecaId(id)) {
            throw new IllegalStateException("A peça tem encomendas associadas e não pode ser apagada");
        }
        pecas.delete(peca);
        pecas.flush();
    }

    public Peca peca(Long id) {
        return pecas.findById(id).orElseThrow(() -> new NaoEncontradoException("Peça", id));
    }

    private void definirMateriais(Peca peca, List<QuantidadeMaterialPedido> lista) {
        peca.limparMateriais();
        pecas.flush();
        for (QuantidadeMaterialPedido q : lista) {
            peca.adicionarMaterial(material(q.materialId()), q.quantidade());
        }
    }
}
