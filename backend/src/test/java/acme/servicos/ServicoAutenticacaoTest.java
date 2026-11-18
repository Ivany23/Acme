package acme.servicos;

import acme.modelo.Cliente;
import acme.modelo.Papel;
import acme.modelo.Utilizador;
import acme.repositorio.ClienteRepository;
import acme.repositorio.UtilizadorRepository;
import acme.servico.NaoAutenticadoException;
import acme.servico.ServicoAutenticacao;
import acme.servico.TokenService;
import acme.web.dto.Pedidos.RegistoClientePedido;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Serviços: ServicoAutenticacao (cartão CRC do TG1) com os repositórios simulados. */
@ExtendWith(MockitoExtension.class)
class ServicoAutenticacaoTest {

    @Mock
    private UtilizadorRepository utilizadores;
    @Mock
    private ClienteRepository clientes;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private ServicoAutenticacao servico;

    @BeforeEach
    void preparar() {
        TokenService tokens = new TokenService("segredo-de-testes-suficiente", Duration.ofHours(1), Clock.systemUTC());
        servico = new ServicoAutenticacao(utilizadores, clientes, encoder, tokens);
    }

    private Utilizador contador() {
        return new Utilizador("contador", "Rita", encoder.encode("acme2026"), Papel.CONTADOR);
    }

    @Test
    void loginCorrectoDevolveTokenEPapel() {
        when(utilizadores.findByLogin("contador")).thenReturn(Optional.of(contador()));

        SessaoDTO s = servico.login(" Contador ", "acme2026");

        assertEquals(Papel.CONTADOR, s.utilizador().papel());
        assertEquals("Contador", s.utilizador().papelDescricao());
        assertEquals("Rita", servico.autenticar(s.token()).nome());
    }

    @Test
    void passwordErradaFalha() {
        when(utilizadores.findByLogin("contador")).thenReturn(Optional.of(contador()));
        assertThrows(NaoAutenticadoException.class, () -> servico.login("contador", "errada"));
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

    @Test
    void registarClienteCriaClienteEContaComPapelCliente() {
        when(utilizadores.existsByLogin("ana@mail.com")).thenReturn(false);
        when(clientes.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(utilizadores.save(any(Utilizador.class))).thenAnswer(inv -> inv.getArgument(0));

        SessaoDTO s = servico.registarCliente(new RegistoClientePedido("Ana", "Ana@Mail.com", "84", "segredo1"));

        assertEquals(Papel.CLIENTE, s.utilizador().papel());
        assertEquals("ana@mail.com", s.utilizador().login());
        assertNotNull(s.token());
        verify(clientes).save(any(Cliente.class));
    }

    @Test
    void naoRegistaDuasContasComOMesmoEmail() {
        when(utilizadores.existsByLogin("ana@mail.com")).thenReturn(true);

        assertThrows(IllegalStateException.class,
                () -> servico.registarCliente(new RegistoClientePedido("Ana", "ana@mail.com", null, "segredo1")));
        verify(clientes, never()).save(any());
    }

    @Test
    void alterarPasswordExigeAPasswordActual() {
        Utilizador u = contador();
        when(utilizadores.findById(5L)).thenReturn(Optional.of(u));

        assertThrows(IllegalArgumentException.class, () -> servico.alterarPassword(5L, "errada", "nova123"));
        servico.alterarPassword(5L, "acme2026", "nova123");

        assertTrue(encoder.matches("nova123", u.getPasswordHash()));
    }
}
