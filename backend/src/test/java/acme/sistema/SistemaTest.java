package acme.sistema;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Teste de SISTEMA: arranca a aplicação completa num servidor HTTP real (porta aleatória)
 * e usa-a como o frontend faria, com pedidos HTTP verdadeiros do início ao fim.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SistemaTest {

    @Autowired
    private Environment ambiente;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private String base;
    private String token;

    record Resposta(int status, String corpo) {
        <T> T ler(String caminho) {
            return JsonPath.read(corpo, caminho);
        }

        long id() {
            return ((Number) ler("$.id")).longValue();
        }
    }

    @BeforeEach
    void preparar() {
        base = "http://localhost:" + ambiente.getProperty("local.server.port") + "/api";
        token = null;
    }

    private Resposta pedido(String metodo, String caminho, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + caminho)).timeout(Duration.ofSeconds(20));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            b.header("Content-Type", "application/json");
        }
        b.method(metodo, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        return new Resposta(r.statusCode(), r.body());
    }

    private Resposta get(String caminho) throws Exception {
        return pedido("GET", caminho, null);
    }

    private Resposta post(String caminho, String json) throws Exception {
        return pedido("POST", caminho, json);
    }

    private void entrar() throws Exception {
        Resposta login = post("/auth/login", "{\"login\":\"admin\",\"password\":\"acme2026\"}");
        assertEquals(200, login.status());
        token = login.ler("$.token");
    }

    @Test
    void cenarioCompletoDeUmaEncomenda() throws Exception {
        // 1. sem login nada é acessível
        assertEquals(401, get("/encomendas").status());
        entrar();

        // 2. catálogo: material + peça com etapas
        long madeira = post("/materiais", "{\"nome\":\"Pinho\",\"unidadeMedida\":\"kg\",\"custoUnitario\":40}").id();
        Resposta peca = post("/pecas", """
                {"nome":"Banco","dimensoes":"40x40x45 cm","precoUnitario":800,
                 "materiais":[{"materialId":%d,"quantidade":3}],
                 "etapas":[{"descricao":"Corte","ordem":1},{"descricao":"Montagem","ordem":2}]}
                """.formatted(madeira));
        assertEquals(201, peca.status());
        long montagem = ((Number) peca.ler("$.etapas[1].id")).longValue();

        // 3. pessoas e turnos
        long cliente = post("/clientes", "{\"nome\":\"Carlos\",\"email\":\"carlos@mail.com\"}").id();
        long artesao = post("/funcionarios", "{\"tipo\":\"ARTESAO\",\"nome\":\"Pedro\"}").id();
        long turno = post("/turnos", "{\"denominacao\":\"Manhã\",\"horaEntrada\":\"08:00\",\"horaSaida\":\"16:00\"}").id();
        Resposta alocacao = post("/alocacoes/automatica", "{\"data\":\"2032-03-01\"}");
        assertEquals(201, alocacao.status());

        // 4. encomenda: 800x5 + 120x5 + 0 = 4600 (+20%) = 5520
        Resposta enc = post("/encomendas", "{\"clienteId\":%d,\"pecaId\":%d,\"quantidade\":5}"
                .formatted(cliente, peca.id()));
        assertEquals(201, enc.status());
        assertEquals(5520.0, ((Number) enc.ler("$.custoEstimado")).doubleValue(), 0.001);
        long encomenda = enc.id();

        // 5. pagamento, produção e ponto
        assertEquals("EM_PRODUCAO", post("/encomendas/" + encomenda + "/pagamento", null).ler("$.estado"));
        Resposta producao = post("/producao", """
                {"artesaoId":%d,"encomendaId":%d,"etapaId":%d,"turnoId":%d,"data":"2032-03-01",
                 "horaInicio":"09:00","horaFim":"12:15","materiaisUsados":[{"materialId":%d,"quantidade":16}],
                 "custosIndirectos":100}
                """.formatted(artesao, encomenda, montagem, turno, madeira));
        assertEquals(201, producao.status());
        Resposta ponto = post("/ponto", "{\"artesaoId\":%d,\"data\":\"2032-03-01\",\"horaEntrada\":\"08:00\",\"horaSaida\":\"17:30\"}"
                .formatted(artesao));
        assertEquals(201, ponto.status());
        assertEquals(90, (int) ponto.ler("$.minutosExtra"));

        // 6. conclusão -> relatório automático (Observer)
        assertEquals(200, pedido("PATCH", "/encomendas/" + encomenda + "/estado", "{\"estado\":\"CONCLUIDA\"}").status());
        Resposta relatorio = get("/relatorios/encomenda/" + encomenda);
        assertEquals(200, relatorio.status());
        assertEquals(195, (int) relatorio.ler("$.minutosPorEtapa.Montagem"));
        assertEquals(740.0, ((Number) relatorio.ler("$.custoReal")).doubleValue(), 0.001); // 16x40 + 100

        List<String> notificacoes = JsonPath.read(get("/clientes/" + cliente + "/notificacoes").corpo(), "$");
        assertEquals(2, notificacoes.size());

        // 7. painel reflecte a encomenda concluída
        Resposta painel = get("/dashboard");
        assertEquals(200, painel.status());
        assertTrue(((Number) painel.ler("$.encomendasPorEstado.CONCLUIDA")).intValue() >= 1);
    }

    @Test
    void clienteAcompanhaASuaEncomendaPeloPortal() throws Exception {
        // a empresa tem uma peça no catálogo
        entrar();
        long peca = post("/pecas", "{\"nome\":\"Estante\",\"dimensoes\":\"180x80\",\"precoUnitario\":2500}").id();
        String tokenAdmin = token;

        // o cliente regista a sua conta (sem sessão)
        token = null;
        Resposta registo = post("/portal/registar",
                "{\"nome\":\"Joana\",\"email\":\"joana@mail.com\",\"password\":\"segredo1\"}");
        assertEquals(201, registo.status());
        String tokenCliente = registo.ler("$.token");

        // solicita orçamento (2500 x 2 = 5000 +20% = 6000) e encomenda
        token = tokenCliente;
        Resposta orc = get("/portal/orcamento?pecaId=" + peca + "&quantidade=2");
        assertEquals(6000.0, ((Number) orc.ler("$.custoEstimado")).doubleValue(), 0.001);
        long encomenda = post("/portal/encomendas", "{\"pecaId\":" + peca + ",\"quantidade\":2}").id();
        assertEquals(403, get("/encomendas").status());

        // a empresa confirma o pagamento e conclui a encomenda
        token = tokenAdmin;
        assertEquals(200, post("/encomendas/" + encomenda + "/pagamento", null).status());
        assertEquals(200, pedido("PATCH", "/encomendas/" + encomenda + "/estado", "{\"estado\":\"CONCLUIDA\"}").status());

        // o cliente vê o estado e as notificações (padrão Observer)
        token = tokenCliente;
        assertEquals("CONCLUIDA", get("/portal/encomendas").ler("$[0].estado"));
        List<String> notificacoes = JsonPath.read(get("/portal/notificacoes").corpo(), "$");
        assertEquals(2, notificacoes.size());
        assertTrue(notificacoes.get(1).endsWith("CONCLUIDA"));
    }

    @Test
    void cadaPerfilSoFazOQueLheCompete() throws Exception {
        entrar();
        assertEquals(201, post("/utilizadores",
                "{\"login\":\"rita\",\"nome\":\"Rita\",\"papel\":\"CONTADOR\",\"password\":\"segredo1\"}").status());

        Resposta login = post("/auth/login", "{\"login\":\"rita\",\"password\":\"segredo1\"}");
        token = login.ler("$.token");
        assertEquals("CONTADOR", login.ler("$.utilizador.papel"));

        assertEquals(201, post("/materiais", "{\"nome\":\"Lixa\",\"unidadeMedida\":\"folha\",\"custoUnitario\":15}").status());
        assertEquals(200, get("/relatorios/analise").status());
        assertEquals(403, post("/funcionarios", "{\"tipo\":\"ARTESAO\",\"nome\":\"X\"}").status());
        assertEquals(403, get("/utilizadores").status());
    }

    @Test
    void sessaoInvalidaEErrosDeNegocioSaoRespondidosCorrectamente() throws Exception {
        token = "token.falso";
        assertEquals(401, get("/materiais").status());

        entrar();
        assertEquals(404, get("/pecas/123456").status());
        assertEquals(400, post("/clientes", "{\"nome\":\"\"}").status());
        assertEquals(400, post("/turnos", "{\"denominacao\":\"X\",\"horaEntrada\":\"10:00\",\"horaSaida\":\"09:00\"}").status());
    }
}
