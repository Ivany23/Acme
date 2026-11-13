package acme.repositorio;

import acme.modelo.AlocacaoTurno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlocacaoTurnoRepository extends JpaRepository<AlocacaoTurno, Long> {

    List<AlocacaoTurno> findByDataOrderByTurnoHoraEntradaAscArtesaoNomeAsc(LocalDate data);

    Optional<AlocacaoTurno> findByArtesaoIdAndData(Long artesaoId, LocalDate data);

    boolean existsByArtesaoId(Long artesaoId);

    boolean existsByTurnoId(Long turnoId);
}
