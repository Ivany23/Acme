package acme.repositorio;

import acme.modelo.RegistoProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistoProducaoRepository extends JpaRepository<RegistoProducao, Long> {

    List<RegistoProducao> findAllByOrderByDataDescHoraInicioDesc();

    List<RegistoProducao> findByEncomendaIdOrderByDataAscHoraInicioAsc(Long encomendaId);

    boolean existsByArtesaoId(Long artesaoId);

    boolean existsByTurnoId(Long turnoId);

    boolean existsByEtapaId(Long etapaId);

    boolean existsByMateriaisUsadosMaterialId(Long materialId);
}
