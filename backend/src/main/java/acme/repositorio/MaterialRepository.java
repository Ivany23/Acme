package acme.repositorio;

import acme.modelo.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    List<Material> findAllByOrderByNomeAsc();
}
