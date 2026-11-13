package acme.modelo;

import jakarta.persistence.*;

import java.time.LocalDate;

/** Indica em que turno trabalha um artesão num dado dia (RF005). Nunca mais de um turno por dia. */
@Entity
@Table(name = "alocacao_turno",
        uniqueConstraints = @UniqueConstraint(name = "uk_alocacao_artesao_data", columnNames = {"artesao_id", "data"}))
public class AlocacaoTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artesao_id")
    private Artesao artesao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id")
    private Turno turno;

    @Column(nullable = false)
    private LocalDate data;

    protected AlocacaoTurno() {
    }

    public AlocacaoTurno(Artesao artesao, Turno turno, LocalDate data) {
        this.artesao = Validar.naoNulo(artesao, "artesao");
        this.turno = Validar.naoNulo(turno, "turno");
        this.data = Validar.naoNulo(data, "data");
    }

    public Long getId() { return id; }
    public Artesao getArtesao() { return artesao; }
    public Turno getTurno() { return turno; }
    public LocalDate getData() { return data; }
}
