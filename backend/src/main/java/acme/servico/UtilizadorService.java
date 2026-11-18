package acme.servico;

import acme.modelo.Cliente;
import acme.modelo.Funcionario;
import acme.modelo.Papel;
import acme.modelo.Utilizador;
import acme.repositorio.ClienteRepository;
import acme.repositorio.FuncionarioRepository;
import acme.repositorio.UtilizadorRepository;
import acme.web.dto.Pedidos.UtilizadorPedido;
import acme.web.dto.Respostas.UtilizadorDTO;
import org.hibernate.Hibernate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/** Gestão das contas de acesso (apenas o administrador). */
@Service
@Transactional
public class UtilizadorService {

    private final UtilizadorRepository utilizadores;
    private final FuncionarioRepository funcionarios;
    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;

    public UtilizadorService(UtilizadorRepository utilizadores, FuncionarioRepository funcionarios,
                             ClienteRepository clientes, PasswordEncoder encoder) {
        this.utilizadores = utilizadores;
        this.funcionarios = funcionarios;
        this.clientes = clientes;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UtilizadorDTO> listar() {
        return utilizadores.findAllByOrderByNomeAsc().stream().map(UtilizadorDTO::de).toList();
    }

    public UtilizadorDTO criar(UtilizadorPedido p) {
        String login = ServicoAutenticacao.normalizar(p.login());
        if (utilizadores.existsByLogin(login)) {
            throw new IllegalStateException("Já existe um utilizador com o login '" + login + "'");
        }
        if (p.password() == null || p.password().length() < 6) {
            throw new IllegalArgumentException("A password deve ter pelo menos 6 caracteres");
        }
        Funcionario f = funcionario(p);
        Cliente c = cliente(p);
        verificarPessoaLivre(f, c, null);
        Utilizador u = new Utilizador(login, p.nome(), encoder.encode(p.password()), p.papel(), f, c);
        return UtilizadorDTO.de(utilizadores.save(u));
    }

    public UtilizadorDTO actualizar(Long id, UtilizadorPedido p, Long quemAltera) {
        Utilizador u = utilizador(id);
        if (!u.getLogin().equals(ServicoAutenticacao.normalizar(p.login()))) {
            throw new IllegalArgumentException("O login não pode ser alterado");
        }
        if (Objects.equals(id, quemAltera) && p.papel() != Papel.ADMINISTRADOR) {
            throw new IllegalStateException("Não pode retirar o papel de administrador à sua própria conta");
        }
        Funcionario f = funcionario(p);
        Cliente c = cliente(p);
        verificarPessoaLivre(f, c, id);
        u.actualizar(p.nome(), p.papel(), f, c);
        if (p.password() != null && !p.password().isBlank()) {
            if (p.password().length() < 6) {
                throw new IllegalArgumentException("A password deve ter pelo menos 6 caracteres");
            }
            u.alterarPasswordHash(encoder.encode(p.password()));
        }
        return UtilizadorDTO.de(u);
    }

    public void apagar(Long id, Long quemApaga) {
        Utilizador u = utilizador(id);
        if (Objects.equals(id, quemApaga)) {
            throw new IllegalStateException("Não pode apagar a sua própria conta");
        }
        utilizadores.delete(u);
    }

    private Utilizador utilizador(Long id) {
        return utilizadores.findById(id).orElseThrow(() -> new NaoEncontradoException("Utilizador", id));
    }

    private Funcionario funcionario(UtilizadorPedido p) {
        if (p.papel() == Papel.CLIENTE || p.funcionarioId() == null) {
            return null;
        }
        Funcionario f = funcionarios.findById(p.funcionarioId())
                .orElseThrow(() -> new NaoEncontradoException("Funcionário", p.funcionarioId()));
        return (Funcionario) Hibernate.unproxy(f);
    }

    private Cliente cliente(UtilizadorPedido p) {
        if (p.papel() != Papel.CLIENTE) {
            return null;
        }
        if (p.clienteId() == null) {
            throw new IllegalArgumentException("Escolha o cliente a quem pertence a conta");
        }
        return clientes.findById(p.clienteId()).orElseThrow(() -> new NaoEncontradoException("Cliente", p.clienteId()));
    }

    /** Cada funcionário ou cliente tem no máximo uma conta. */
    private void verificarPessoaLivre(Funcionario f, Cliente c, Long idActual) {
        Long id = idActual == null ? -1L : idActual;
        if (f != null && utilizadores.existsByFuncionarioIdAndIdNot(f.getId(), id)) {
            throw new IllegalStateException(f.getNome() + " já tem uma conta de acesso");
        }
        if (c != null && utilizadores.existsByClienteIdAndIdNot(c.getId(), id)) {
            throw new IllegalStateException(c.getNome() + " já tem uma conta de acesso");
        }
    }
}
