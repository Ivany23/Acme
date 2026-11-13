package acme.servico;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Cria e valida tokens de sessão assinados com HMAC-SHA256.
 * Formato: base64url("login|expiracaoEmSegundos") + "." + base64url(assinatura).
 */
public class TokenService {

    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DESCODIFICADOR = Base64.getUrlDecoder();

    private final byte[] segredo;
    private final Duration validade;
    private final Clock relogio;

    public TokenService(String segredo, Duration validade, Clock relogio) {
        if (segredo == null || segredo.length() < 16) {
            throw new IllegalArgumentException("O segredo dos tokens deve ter pelo menos 16 caracteres");
        }
        this.segredo = segredo.getBytes(StandardCharsets.UTF_8);
        this.validade = validade;
        this.relogio = relogio;
    }

    public String gerar(String login) {
        long expira = relogio.instant().plus(validade).getEpochSecond();
        String dados = CODIFICADOR.encodeToString((login + "|" + expira).getBytes(StandardCharsets.UTF_8));
        return dados + "." + CODIFICADOR.encodeToString(assinar(dados));
    }

    /** Devolve o login se o token for válido e não estiver expirado. */
    public Optional<String> validar(String token) {
        try {
            if (token == null) {
                return Optional.empty();
            }
            String[] partes = token.split("\\.");
            if (partes.length != 2) {
                return Optional.empty();
            }
            byte[] assinatura = DESCODIFICADOR.decode(partes[1]);
            if (!MessageDigest.isEqual(assinatura, assinar(partes[0]))) {
                return Optional.empty();
            }
            String dados = new String(DESCODIFICADOR.decode(partes[0]), StandardCharsets.UTF_8);
            int separador = dados.lastIndexOf('|');
            if (separador <= 0) {
                return Optional.empty();
            }
            long expira = Long.parseLong(dados.substring(separador + 1));
            if (relogio.instant().getEpochSecond() >= expira) {
                return Optional.empty();
            }
            return Optional.of(dados.substring(0, separador));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private byte[] assinar(String dados) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo, "HmacSHA256"));
            return mac.doFinal(dados.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível assinar o token", e);
        }
    }
}
