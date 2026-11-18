package acme.integracao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integração: controlo de acesso por perfil (actores do TG1) aplicado à API real com base de dados H2. */
@SpringBootTest
class PerfisIntegracaoTest {

    @Autowired
    private WebApplicationContext contexto;

    private ApiTeste api;

    @BeforeEach
    void preparar() {
        api = new ApiTeste(contexto);
    }

    private static final String MATERIAL = "{\"nome\":\"Cola\",\"unidadeMedida\":\"litro\",\"custoUnitario\":120}";

    @Test
    void contadorAlteraCustosMasArtesaoNao() throws Exception {
        long artesao = api.criarArtesao("João");
        String contador = api.contaCom("CONTADOR", null, null);
        String tokenArtesao = api.contaCom("ARTESAO", artesao, null);

        api.pedido(contador, post("/api/materiais"), MATERIAL).andExpect(status().isCreated());
        api.pedido(tokenArtesao, post("/api/materiais"), MATERIAL)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem", containsString("Artesão")));
    }

    @Test
    void artesaoConsultaOQuePrecisaMasNaoVeFinancasNemClientes() throws Exception {
        String tokenArtesao = api.contaCom("ARTESAO", api.criarArtesao("Maria"), null);

        api.pedido(tokenArtesao, get("/api/materiais"), null).andExpect(status().isOk());
        api.pedido(tokenArtesao, get("/api/encomendas"), null).andExpect(status().isOk());
        api.pedido(tokenArtesao, get("/api/relatorios"), null).andExpect(status().isForbidden());
        api.pedido(tokenArtesao, get("/api/clientes"), null).andExpect(status().isForbidden());
        api.pedido(tokenArtesao, get("/api/utilizadores"), null).andExpect(status().isForbidden());
    }

    @Test
    void artesaoSoRegistaOSeuProprioPonto() throws Exception {
        long joao = api.criarArtesao("João");
        long pedro = api.criarArtesao("Pedro");
        long turno = api.criarTurno();
        String tokenJoao = api.contaCom("ARTESAO", joao, null);
        String ponto = "{\"artesaoId\":%d,\"turnoId\":%d,\"data\":\"2033-01-05\",\"horaEntrada\":\"07:00\",\"horaSaida\":\"15:30\"}";

        api.pedido(tokenJoao, post("/api/ponto"), ponto.formatted(joao, turno)).andExpect(status().isCreated());
        api.pedido(tokenJoao, post("/api/ponto"), ponto.formatted(pedro, turno)).andExpect(status().isForbidden());
    }

    @Test
    void gerenteDeVendasConsultaHistoricoMasNaoGereUtilizadores() throws Exception {
        String vendas = api.contaCom("GERENTE_VENDAS", null, null);

        api.pedido(vendas, get("/api/relatorios/analise"), null).andExpect(status().isOk());
        api.pedido(vendas, get("/api/utilizadores"), null).andExpect(status().isForbidden());
        api.pedido(vendas, post("/api/funcionarios"), "{\"tipo\":\"ARTESAO\",\"nome\":\"X\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void clienteNaoAcedeAGestaoEAdminNaoAcedeAAreaDoCliente() throws Exception {
        long cliente = api.id(api.corpo(api.admin(), post("/api/clientes"), "{\"nome\":\"Carlos\"}"));
        String tokenCliente = api.contaCom("CLIENTE", null, cliente);

        api.pedido(tokenCliente, get("/api/materiais"), null).andExpect(status().isForbidden());
        api.pedido(tokenCliente, get("/api/portal/encomendas"), null).andExpect(status().isOk());
        api.pedido(api.admin(), get("/api/portal/encomendas"), null).andExpect(status().isForbidden());
    }

    @Test
    void regrasDaGestaoDeUtilizadores() throws Exception {
        String admin = api.admin();
        String login = ApiTeste.unico("dup");
        String conta = "{\"login\":\"" + login + "\",\"nome\":\"X\",\"papel\":\"CONTADOR\",\"password\":\"segredo1\"}";

        // artesão sem funcionário associado
        api.pedido(admin, post("/api/utilizadores"),
                        "{\"login\":\"" + ApiTeste.unico("a") + "\",\"nome\":\"X\",\"papel\":\"ARTESAO\",\"password\":\"segredo1\"}")
                .andExpect(status().isBadRequest());
        // login duplicado
        api.pedido(admin, post("/api/utilizadores"), conta).andExpect(status().isCreated());
        api.pedido(admin, post("/api/utilizadores"), conta).andExpect(status().isConflict());
        // o administrador não se apaga a si próprio
        long proprio = ((Number) com.jayway.jsonpath.JsonPath.read(
                api.corpo(admin, get("/api/auth/eu"), null), "$.id")).longValue();
        api.pedido(admin, delete("/api/utilizadores/" + proprio), null).andExpect(status().isConflict());
    }

    @Test
    void funcionarioComContaNaoPodeSerApagado() throws Exception {
        long artesao = api.criarArtesao("Rui");
        api.contaCom("ARTESAO", artesao, null);

        api.pedido(api.admin(), delete("/api/funcionarios/" + artesao), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("conta de acesso")));
    }

    @Test
    void qualquerUtilizadorAlteraASuaPassword() throws Exception {
        String login = ApiTeste.unico("sup");
        String admin = api.admin();
        api.pedido(admin, post("/api/utilizadores"),
                "{\"login\":\"" + login + "\",\"nome\":\"S\",\"papel\":\"SUPERVISOR\",\"password\":\"segredo1\"}")
                .andExpect(status().isCreated());
        String token = api.login(login, "segredo1");

        api.pedido(token, put("/api/auth/password"), "{\"actual\":\"errada\",\"nova\":\"outra123\"}")
                .andExpect(status().isBadRequest());
        api.pedido(token, put("/api/auth/password"), "{\"actual\":\"segredo1\",\"nova\":\"outra123\"}")
                .andExpect(status().isNoContent());
        api.login(login, "outra123");
    }
}
