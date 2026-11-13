package acme.modelo;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Relatório gerado no fim da produção de uma encomenda (RF009). */
@Entity
@Table(name = "relatorio_producao")
public class RelatorioProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encomenda_id", unique = true)
    private Encomenda encomenda;

    @ElementCollection
    @CollectionTable(name = "relatorio_minutos_etapa", joinColumns = @JoinColumn(name = "relatorio_id"))
    @MapKeyColumn(name = "etapa")
    @Column(name = "minutos")
    private Map<String, Long> minutosPorEtapa = new LinkedHashMap<>();

    @ElementCollection
    @CollectionTable(name = "relatorio_minutos_artesao", joinColumns = @JoinColumn(name = "relatorio_id"))
    @MapKeyColumn(name = "artesao")
    @Column(name = "minutos")
    private Map<String, Long> minutosPorArtesao = new LinkedHashMap<>();

    @ElementCollection
    @CollectionTable(name = "relatorio_materiais", joinColumns = @JoinColumn(name = "relatorio_id"))
    @MapKeyColumn(name = "material")
    @Column(name = "quantidade")
    private Map<String, Double> materiaisConsumidos = new LinkedHashMap<>();

    @ElementCollection
    @CollectionTable(name = "relatorio_minutos_extra", joinColumns = @JoinColumn(name = "relatorio_id"))
    @MapKeyColumn(name = "artesao")
    @Column(name = "minutos")
    private Map<String, Long> minutosExtraPorArtesao = new LinkedHashMap<>();

    private long retrabalhos;

    private double custoEstimado;

    private double custoReal;

    @Column(nullable = false)
    private LocalDateTime dataGeracao = LocalDateTime.now();

    protected RelatorioProducao() {
    }

    public RelatorioProducao(Encomenda encomenda, List<RegistoProducao> registos,
                             List<RegistoPonto> pontos, double custoReal) {
        this.encomenda = Validar.naoNulo(encomenda, "encomenda");
        this.custoEstimado = encomenda.getCustoEstimado();
        this.custoReal = custoReal;
        for (RegistoProducao r : registos) {
            minutosPorEtapa.merge(r.getEtapa().getDescricao(), r.calcularTempoGastoMinutos(), Long::sum);
            minutosPorArtesao.merge(r.getArtesao().getNome(), r.calcularTempoGastoMinutos(), Long::sum);
            for (MaterialUsado m : r.getMateriaisUsados()) {
                materiaisConsumidos.merge(m.getMaterial().getNome(), m.getQuantidade(), Double::sum);
            }
            if (r.isRetrabalho()) {
                retrabalhos++;
            }
        }
        for (RegistoPonto p : pontos) {
            minutosExtraPorArtesao.merge(p.getArtesao().getNome(), p.calcularMinutosExtra(), Long::sum);
        }
    }

    public Long getId() { return id; }
    public Encomenda getEncomenda() { return encomenda; }
    public Map<String, Long> getMinutosPorEtapa() { return minutosPorEtapa; }
    public Map<String, Long> getMinutosPorArtesao() { return minutosPorArtesao; }
    public Map<String, Double> getMateriaisConsumidos() { return materiaisConsumidos; }
    public Map<String, Long> getMinutosExtraPorArtesao() { return minutosExtraPorArtesao; }
    public long getRetrabalhos() { return retrabalhos; }
    public double getCustoEstimado() { return custoEstimado; }
    public double getCustoReal() { return custoReal; }
    public LocalDateTime getDataGeracao() { return dataGeracao; }
}
