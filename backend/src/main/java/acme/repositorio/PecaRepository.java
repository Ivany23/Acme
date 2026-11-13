package acme.repositorio;

import acme.modelo.Peca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PecaRepository extends JpaRepository<Peca, Long> {
    List<Peca> findAllByOrderByNomeAsc();
}
