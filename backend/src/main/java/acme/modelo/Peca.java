package acme.modelo;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Peça fabricada pela Acme (RF001). */
@Entity
@Table(name = "peca")
public class Peca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String dimensoes;

    private double precoUnitario;

    @OneToMany(mappedBy = "peca", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaterialPeca> materiais = new ArrayList<>();

    @OneToMany(mappedBy = "peca", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC")
    private List<EtapaProducao> etapas = new ArrayList<>();

    protected Peca() {
    }

    public Peca(String nome, String dimensoes, double precoUnitario) {
        actualizar(nome, dimensoes, precoUnitario);
    }

    public void actualizar(String nome, String dimensoes, double precoUnitario) {
        if (precoUnitario < 0) {
            throw new IllegalArgumentException("O preço unitário não pode ser negativo");
        }
        this.nome = Validar.obrigatorio(nome, "nome");
        this.dimensoes = dimensoes;
        this.precoUnitario = precoUnitario;
    }

    /** Define (ou substitui) a quantidade de um material necessária por unidade. */
    public void adicionarMaterial(Material material, double quantidadePorUnidade) {
        materiais.removeIf(m -> Objects.equals(m.getMaterial(), material));
        materiais.add(new MaterialPeca(this, material, quantidadePorUnidade));
    }

    public void limparMateriais() {
        materiais.clear();
    }

    public EtapaProducao adicionarEtapa(String descricao, int ordem) {
        EtapaProducao etapa = new EtapaProducao(this, descricao, ordem);
        etapas.add(etapa);
        ordenarEtapas();
        return etapa;
    }

    /** Remove as etapas cujo id não esteja na lista de ids a manter. */
    public void manterApenasEtapas(Collection<Long> idsAManter) {
        etapas.removeIf(e -> e.getId() == null || !idsAManter.contains(e.getId()));
    }

    public EtapaProducao obterEtapa(Long etapaId) {
        for (EtapaProducao e : etapas) {
            if (Objects.equals(e.getId(), etapaId)) {
                return e;
            }
        }
        return null;
    }

    public void ordenarEtapas() {
        etapas.sort(Comparator.comparingInt(EtapaProducao::getOrdem));
    }

    /** Custo dos materiais necessários para fabricar UMA unidade. */
    public double custoMateriaisPorUnidade() {
        double total = 0;
        for (MaterialPeca m : materiais) {
            total += m.custo();
        }
        return total;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDimensoes() { return dimensoes; }
    public double getPrecoUnitario() { return precoUnitario; }
    public List<MaterialPeca> getMateriais() { return materiais; }
    public List<EtapaProducao> getEtapas() { return etapas; }
}
