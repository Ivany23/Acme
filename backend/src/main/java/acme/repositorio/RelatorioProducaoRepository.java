package acme.repositorio;

import acme.modelo.RelatorioProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RelatorioProducaoRepository extends JpaRepository<RelatorioProducao, Long> {

    List<RelatorioProducao> findAllByOrderByDataGeracaoDesc();

    Optional<RelatorioProducao> findByEncomendaId(Long encomendaId);

    boolean existsByEncomendaId(Long encomendaId);
}
