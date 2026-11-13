package acme.repositorio;

import acme.modelo.Encomenda;
import acme.modelo.EstadoEncomenda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EncomendaRepository extends JpaRepository<Encomenda, Long> {

    List<Encomenda> findAllByOrderByIdDesc();

    List<Encomenda> findByEstadoOrderByIdDesc(EstadoEncomenda estado);

    List<Encomenda> findByClienteIdOrderByIdDesc(Long clienteId);

    long countByEstado(EstadoEncomenda estado);

    boolean existsByClienteId(Long clienteId);

    boolean existsByPecaId(Long pecaId);
}
