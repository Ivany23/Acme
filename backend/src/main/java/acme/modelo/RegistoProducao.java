package acme.modelo;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Resumo da produção feita por um artesão num turno (RF006). */
@Entity
@Table(name = "registo_producao")
public class RegistoProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artesao_id")
    private Artesao artesao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encomenda_id")
    private Encomenda encomenda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "etapa_id")
    private EtapaProducao etapa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private LocalTime horaInicio;

    @Column(nullable = false)
    private LocalTime horaFim;

    @OneToMany(mappedBy = "registo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MaterialUsado> materiaisUsados = new ArrayList<>();

    private double custosIndirectos;

    private boolean retrabalho;

    protected RegistoProducao() {
    }

    public RegistoProducao(Artesao artesao, Encomenda encomenda, EtapaProducao etapa, Turno turno, LocalDate data,
                           LocalTime horaInicio, LocalTime horaFim, double custosIndirectos, boolean retrabalho) {
        Validar.naoNulo(horaInicio, "horaInicio");
        Validar.naoNulo(horaFim, "horaFim");
        if (!horaFim.isAfter(horaInicio)) {
            throw new IllegalArgumentException("A hora de fim deve ser depois da hora de início");
        }
        this.artesao = Validar.naoNulo(artesao, "artesao");
        this.encomenda = Validar.naoNulo(encomenda, "encomenda");
        this.etapa = Validar.naoNulo(etapa, "etapa");
        this.turno = Validar.naoNulo(turno, "turno");
        this.data = Validar.naoNulo(data, "data");
        this.horaInicio = horaInicio;
        this.horaFim = horaFim;
        this.custosIndirectos = Validar.naoNegativo(custosIndirectos, "custosIndirectos");
        this.retrabalho = retrabalho;
    }

    public void adicionarMaterialUsado(Material material, double quantidade) {
        materiaisUsados.add(new MaterialUsado(this, material, quantidade));
    }

    public long calcularTempoGastoMinutos() {
        return Duration.between(horaInicio, horaFim).toMinutes();
    }

    public double custoMateriais() {
        double total = 0;
        for (MaterialUsado m : materiaisUsados) {
            total += m.custo();
        }
        return total;
    }

    public Long getId() { return id; }
    public Artesao getArtesao() { return artesao; }
    public Encomenda getEncomenda() { return encomenda; }
    public EtapaProducao getEtapa() { return etapa; }
    public Turno getTurno() { return turno; }
    public LocalDate getData() { return data; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public List<MaterialUsado> getMateriaisUsados() { return materiaisUsados; }
    public double getCustosIndirectos() { return custosIndirectos; }
    public boolean isRetrabalho() { return retrabalho; }
}
