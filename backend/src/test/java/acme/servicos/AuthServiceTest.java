package acme.servicos;

import acme.modelo.Utilizador;
import acme.repositorio.UtilizadorRepository;
import acme.servico.AuthService;
import acme.servico.NaoAutenticadoException;
import acme.servico.TokenService;
import acme.web.dto.Respostas.SessaoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** Serviços: login com o repositório de utilizadores simulado. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtilizadorRepository utilizadores;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private AuthService servico;

    @BeforeEach
    void preparar() {
        TokenService tokens = new TokenService("segredo-de-testes-suficiente", Duration.ofHours(1), Clock.systemUTC());
        servico = new AuthService(utilizadores, encoder, tokens);
    }

    @Test
    void loginCorrectoDevolveTokenUtilizavel() {
        Utilizador admin = new Utilizador("admin", "Administrador", encoder.encode("acme2026"));
        when(utilizadores.findByLogin("admin")).thenReturn(Optional.of(admin));

        SessaoDTO s = servico.login(" Admin ", "acme2026");

        assertNotNull(s.token());
        assertEquals("Administrador", servico.autenticar(s.token()).nome());
    }

    @Test
    void passwordErradaFalha() {
        when(utilizadores.findByLogin("admin"))
                .thenReturn(Optional.of(new Utilizador("admin", "Administrador", encoder.encode("acme2026"))));
        assertThrows(NaoAutenticadoException.class, () -> servico.login("admin", "errada"));
    }

    @Test
    void utilizadorInexistenteFalha() {
        when(utilizadores.findByLogin("ninguem")).thenReturn(Optional.empty());
        assertThrows(NaoAutenticadoException.class, () -> servico.login("ninguem", "x"));
    }

    @Test
    void tokenInvalidoFalha() {
        assertThrows(NaoAutenticadoException.class, () -> servico.autenticar("invalido"));
        assertThrows(NaoAutenticadoException.class, () -> servico.autenticar(null));
    }
}
