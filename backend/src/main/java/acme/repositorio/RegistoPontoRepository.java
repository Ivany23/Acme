package acme.repositorio;

import acme.modelo.RegistoPonto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RegistoPontoRepository extends JpaRepository<RegistoPonto, Long> {

    List<RegistoPonto> findAllByOrderByDataDescHoraEntradaDesc();

    List<RegistoPonto> findByDataOrderByHoraEntradaAsc(LocalDate data);

    List<RegistoPonto> findByArtesaoIdOrderByDataDesc(Long artesaoId);

    List<RegistoPonto> findByArtesaoIdAndData(Long artesaoId, LocalDate data);

    boolean existsByArtesaoId(Long artesaoId);

    boolean existsByTurnoId(Long turnoId);
}
