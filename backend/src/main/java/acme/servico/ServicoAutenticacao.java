package acme.servico;

import acme.modelo.Cliente;
import acme.modelo.Utilizador;
import acme.repositorio.ClienteRepository;
import acme.repositorio.UtilizadorRepository;
import acme.web.dto.Pedidos.RegistoClientePedido;
import acme.web.dto.Respostas.SessaoDTO;
import acme.web.dto.Respostas.UtilizadorDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cartão CRC "ServicoAutenticacao" (TG1): autentica clientes e funcionários,
 * regista a conta do cliente e permite alterar a password.
 */
@Service
@Transactional(readOnly = true)
public class ServicoAutenticacao {

    private static final String CREDENCIAIS_INVALIDAS = "Utilizador ou password incorrectos";

    private final UtilizadorRepository utilizadores;
    private final ClienteRepository clientes;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public ServicoAutenticacao(UtilizadorRepository utilizadores, ClienteRepository clientes,
                               PasswordEncoder encoder, TokenService tokens) {
        this.utilizadores = utilizadores;
        this.clientes = clientes;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    public SessaoDTO login(String login, String password) {
        if (login == null || password == null) {
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        Utilizador u = utilizadores.findByLogin(normalizar(login))
                .orElseThrow(() -> new NaoAutenticadoException(CREDENCIAIS_INVALIDAS));
        if (!encoder.matches(password, u.getPasswordHash())) {
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        return new SessaoDTO(tokens.gerar(u.getLogin()), UtilizadorDTO.de(u));
    }

    /** Valida o token e devolve o utilizador correspondente. */
    public UtilizadorDTO autenticar(String token) {
        String login = tokens.validar(token)
                .orElseThrow(() -> new NaoAutenticadoException("Sessão inválida ou expirada; entre novamente"));
        return utilizadores.findByLogin(login).map(UtilizadorDTO::de)
                .orElseThrow(() -> new NaoAutenticadoException("Utilizador já não existe"));
    }

    /** Caso de uso "Registar Conta de Cliente": cria o cliente e a sua conta, e já devolve a sessão. */
    @Transactional
    public SessaoDTO registarCliente(RegistoClientePedido p) {
        String login = normalizar(p.email());
        if (utilizadores.existsByLogin(login)) {
            throw new IllegalStateException("Já existe uma conta com este email");
        }
        Cliente cliente = clientes.save(new Cliente(p.nome(), login, p.telefone()));
        Utilizador u = utilizadores.save(Utilizador.deCliente(cliente, login, encoder.encode(p.password())));
        return new SessaoDTO(tokens.gerar(u.getLogin()), UtilizadorDTO.de(u));
    }

    @Transactional
    public void alterarPassword(Long utilizadorId, String actual, String nova) {
        Utilizador u = utilizadores.findById(utilizadorId)
                .orElseThrow(() -> new NaoEncontradoException("Utilizador", utilizadorId));
        if (!encoder.matches(actual, u.getPasswordHash())) {
            throw new IllegalArgumentException("A password actual não está correcta");
        }
        u.alterarPasswordHash(encoder.encode(nova));
    }

    static String normalizar(String login) {
        return login.trim().toLowerCase();
    }
}
