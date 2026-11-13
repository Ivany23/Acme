package acme.repositorio;

import acme.modelo.Artesao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArtesaoRepository extends JpaRepository<Artesao, Long> {
    List<Artesao> findAllByOrderByIdAsc();
}
