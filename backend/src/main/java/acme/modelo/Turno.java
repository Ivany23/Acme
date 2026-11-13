package acme.modelo;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.LocalTime;

/** Turno de trabalho, ex.: matutino ou vespertino (RF004). */
@Entity
@Table(name = "turno")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String denominacao;

    @Column(nullable = false)
    private LocalTime horaEntrada;

    @Column(nullable = false)
    private LocalTime horaSaida;

    protected Turno() {
    }

    public Turno(String denominacao, LocalTime horaEntrada, LocalTime horaSaida) {
        actualizar(denominacao, horaEntrada, horaSaida);
    }

    public void actualizar(String denominacao, LocalTime horaEntrada, LocalTime horaSaida) {
        Validar.naoNulo(horaEntrada, "horaEntrada");
        Validar.naoNulo(horaSaida, "horaSaida");
        if (!horaSaida.isAfter(horaEntrada)) {
            throw new IllegalArgumentException("A hora de saída deve ser depois da hora de entrada");
        }
        this.denominacao = Validar.obrigatorio(denominacao, "denominacao");
        this.horaEntrada = horaEntrada;
        this.horaSaida = horaSaida;
    }

    public long calcularDuracaoMinutos() {
        return Duration.between(horaEntrada, horaSaida).toMinutes();
    }

    public Long getId() { return id; }
    public String getDenominacao() { return denominacao; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public LocalTime getHoraSaida() { return horaSaida; }
}
