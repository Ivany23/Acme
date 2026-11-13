package acme.config;

import acme.modelo.Utilizador;
import acme.repositorio.UtilizadorRepository;
import acme.servico.AuthService;
import acme.servico.TokenService;
import acme.web.dto.Respostas.UtilizadorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

/** Login simples: todos os pedidos /api/** precisam de "Authorization: Bearer <token>". */
@Configuration
public class SegurancaConfig implements WebMvcConfigurer {

    public static final String ATRIBUTO_UTILIZADOR = "utilizador";
    private static final Logger log = LoggerFactory.getLogger(SegurancaConfig.class);

    private final AuthService auth;

    public SegurancaConfig(@Lazy AuthService auth) {
        this.auth = auth;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Se AUTH_SEGREDO não estiver definido, deriva um segredo estável a partir da password da base de dados. */
    @Bean
    public TokenService tokenService(@Value("${acme.auth.segredo:}") String segredo,
                                     @Value("${spring.datasource.password:}") String passwordBd,
                                     @Value("${acme.auth.validade-horas:12}") long horas) {
        String chave = segredo.isBlank() ? sha256("acme-sessao:" + passwordBd) : segredo;
        return new TokenService(chave, Duration.ofHours(horas), Clock.systemUTC());
    }

    /**
     * Cria o utilizador administrador quando ainda não existe nenhum.
     * Corre antes de o servidor web aceitar pedidos (SmartInitializingSingleton).
     */
    @Bean
    public SmartInitializingSingleton criarAdministrador(UtilizadorRepository utilizadores, PasswordEncoder encoder,
                                                         @Value("${acme.admin.login:admin}") String login,
                                                         @Value("${acme.admin.password:acme2026}") String password) {
        return () -> {
            if (utilizadores.count() == 0) {
                utilizadores.save(new Utilizador(login, "Administrador", encoder.encode(password)));
                log.info("Utilizador '{}' criado", login);
            }
        };
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest pedido, HttpServletResponse resposta, Object handler) {
                if ("OPTIONS".equalsIgnoreCase(pedido.getMethod())) {
                    return true;
                }
                String cabecalho = pedido.getHeader("Authorization");
                String token = cabecalho != null && cabecalho.startsWith("Bearer ") ? cabecalho.substring(7).trim() : null;
                UtilizadorDTO utilizador = auth.autenticar(token);
                pedido.setAttribute(ATRIBUTO_UTILIZADOR, utilizador);
                return true;
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/login", "/api/estado");
    }

    private static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
