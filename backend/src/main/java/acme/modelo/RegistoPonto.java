package acme.modelo;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/** Hora de entrada e de saída de um artesão, em horas e minutos (RF007). */
@Entity
@Table(name = "registo_ponto")
public class RegistoPonto {

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

    @Column(nullable = false)
    private LocalTime horaEntrada;

    @Column(nullable = false)
    private LocalTime horaSaida;

    protected RegistoPonto() {
    }

    public RegistoPonto(Artesao artesao, Turno turno, LocalDate data, LocalTime horaEntrada, LocalTime horaSaida) {
        Validar.naoNulo(horaEntrada, "horaEntrada");
        Validar.naoNulo(horaSaida, "horaSaida");
        if (!horaSaida.isAfter(horaEntrada)) {
            throw new IllegalArgumentException("A hora de saída deve ser depois da hora de entrada");
        }
        this.artesao = Validar.naoNulo(artesao, "artesao");
        this.turno = Validar.naoNulo(turno, "turno");
        this.data = Validar.naoNulo(data, "data");
        this.horaEntrada = horaEntrada;
        this.horaSaida = horaSaida;
    }

    public long calcularMinutosTrabalhados() {
        return Duration.between(horaEntrada, horaSaida).toMinutes();
    }

    /** Minutos trabalhados para além da duração do turno. */
    public long calcularMinutosExtra() {
        return Math.max(0, calcularMinutosTrabalhados() - turno.calcularDuracaoMinutos());
    }

    public Long getId() { return id; }
    public Artesao getArtesao() { return artesao; }
    public Turno getTurno() { return turno; }
    public LocalDate getData() { return data; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public LocalTime getHoraSaida() { return horaSaida; }
}
