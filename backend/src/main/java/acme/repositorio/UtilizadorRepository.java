package acme.repositorio;

import acme.modelo.Papel;
import acme.modelo.Utilizador;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {

    Optional<Utilizador> findByLogin(String login);

    boolean existsByLogin(String login);

    List<Utilizador> findAllByOrderByNomeAsc();

    List<Utilizador> findByPapelIsNull();

    long countByPapel(Papel papel);

    boolean existsByFuncionarioId(Long funcionarioId);

    boolean existsByClienteId(Long clienteId);

    boolean existsByFuncionarioIdAndIdNot(Long funcionarioId, Long id);

    boolean existsByClienteIdAndIdNot(Long clienteId, Long id);
}
