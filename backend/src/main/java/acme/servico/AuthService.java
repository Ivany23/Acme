package acme.servico;

import acme.modelo.Utilizador;
import acme.repositorio.UtilizadorRepository;
import acme.web.dto.Respostas.SessaoDTO;
import acme.web.dto.Respostas.UtilizadorDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Login dos utilizadores. */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String CREDENCIAIS_INVALIDAS = "Utilizador ou password incorrectos";

    private final UtilizadorRepository utilizadores;
    private final PasswordEncoder encoder;
    private final TokenService tokens;

    public AuthService(UtilizadorRepository utilizadores, PasswordEncoder encoder, TokenService tokens) {
        this.utilizadores = utilizadores;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    public SessaoDTO login(String login, String password) {
        if (login == null || password == null) {
            throw new NaoAutenticadoException(CREDENCIAIS_INVALIDAS);
        }
        Utilizador u = utilizadores.findByLogin(login.trim().toLowerCase())
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
}
