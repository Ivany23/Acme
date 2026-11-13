package acme.unidade;

import acme.servico.TokenService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Unidade: geração e validação dos tokens de sessão. */
class TokenServiceTest {

    private static final String SEGREDO = "um-segredo-com-mais-de-16-caracteres";
    private final Clock agora = Clock.fixed(Instant.parse("2026-11-13T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void tokenValidoDevolveOLogin() {
        TokenService t = new TokenService(SEGREDO, Duration.ofHours(1), agora);
        assertEquals(Optional.of("admin"), t.validar(t.gerar("admin")));
    }

    @Test
    void tokenExpiradoERejeitado() {
        String token = new TokenService(SEGREDO, Duration.ofHours(1), agora).gerar("admin");
        Clock depois = Clock.offset(agora, Duration.ofHours(2));
        assertTrue(new TokenService(SEGREDO, Duration.ofHours(1), depois).validar(token).isEmpty());
    }

    @Test
    void tokenAlteradoERejeitado() {
        TokenService t = new TokenService(SEGREDO, Duration.ofHours(1), agora);
        String token = t.gerar("admin");
        String alterado = "X" + token.substring(1);
        assertTrue(t.validar(alterado).isEmpty());
    }

    @Test
    void tokenDeOutroSegredoERejeitado() {
        String token = new TokenService("outro-segredo-muito-diferente!!", Duration.ofHours(1), agora).gerar("admin");
        assertTrue(new TokenService(SEGREDO, Duration.ofHours(1), agora).validar(token).isEmpty());
    }

    @Test
    void lixoOuNuloSaoRejeitados() {
        TokenService t = new TokenService(SEGREDO, Duration.ofHours(1), agora);
        assertTrue(t.validar(null).isEmpty());
        assertTrue(t.validar("nao-e-um-token").isEmpty());
        assertTrue(t.validar("%%%.###").isEmpty());
    }

    @Test
    void segredoCurtoNaoEAceite() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService("curto", Duration.ofHours(1), agora));
    }
}
