package acme.config;

import acme.modelo.Papel;
import acme.modelo.Utilizador;
import acme.repositorio.UtilizadorRepository;
import acme.seguranca.Acesso;
import acme.seguranca.RegrasAcesso;
import acme.servico.AcessoNegadoException;
import acme.servico.ServicoAutenticacao;
import acme.servico.TokenService;
import acme.web.dto.Respostas.UtilizadorDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;

/**
 * Segurança da API:
 * 1. Autenticação: os pedidos /api/** precisam de "Authorization: Bearer <token>".
 * 2. Autorização: cada endpoint declara com @Acesso os papéis que o podem usar.
 */
@Configuration
public class SegurancaConfig implements WebMvcConfigurer {

    public static final String ATRIBUTO_UTILIZADOR = "utilizador";
    private static final Logger log = LoggerFactory.getLogger(SegurancaConfig.class);

    private final ServicoAutenticacao autenticacao;

    public SegurancaConfig(@Lazy ServicoAutenticacao autenticacao) {
        this.autenticacao = autenticacao;
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
     * Antes de o servidor aceitar pedidos: cria o administrador se não houver nenhum utilizador
     * e atribui o papel de administrador a contas antigas criadas antes de existirem papéis.
     */
    @Bean
    public SmartInitializingSingleton criarAdministrador(UtilizadorRepository utilizadores, PasswordEncoder encoder,
                                                         PlatformTransactionManager transaccoes,
                                                         @Value("${acme.admin.login:admin}") String login,
                                                         @Value("${acme.admin.password:acme2026}") String password) {
        return () -> new TransactionTemplate(transaccoes).executeWithoutResult(estado -> {
            for (Utilizador antigo : utilizadores.findByPapelIsNull()) {
                antigo.actualizar(antigo.getNome(), Papel.ADMINISTRADOR, null, null);
            }
            if (utilizadores.count() == 0) {
                utilizadores.save(new Utilizador(login, "Administrador", encoder.encode(password), Papel.ADMINISTRADOR));
                log.info("Utilizador '{}' criado", login);
            }
        });
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest pedido, HttpServletResponse resposta, Object handler) {
                if ("OPTIONS".equalsIgnoreCase(pedido.getMethod()) || !(handler instanceof HandlerMethod metodo)) {
                    return true;
                }
                String cabecalho = pedido.getHeader("Authorization");
                String token = cabecalho != null && cabecalho.startsWith("Bearer ") ? cabecalho.substring(7).trim() : null;
                UtilizadorDTO utilizador = autenticacao.autenticar(token);

                Acesso acesso = metodo.getMethodAnnotation(Acesso.class);
                if (acesso == null) {
                    acesso = metodo.getBeanType().getAnnotation(Acesso.class);
                }
                if (!RegrasAcesso.permitido(utilizador.papel(), acesso)) {
                    throw new AcessoNegadoException("O seu perfil (" + utilizador.papelDescricao()
                            + ") não tem permissão para esta operação");
                }
                pedido.setAttribute(ATRIBUTO_UTILIZADOR, utilizador);
                return true;
            }
        }).addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/estado", "/api/portal/registar");
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
