package acme.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integração: casos de uso do actor Cliente (TG1) — Registar Conta, Autenticar Cliente,
 * Solicitar Orçamento e Acompanhar Estado da Encomenda (padrão Observer).
 */
@SpringBootTest
class PortalClienteIntegracaoTest {

    @Autowired
    private WebApplicationContext contexto;

    private ApiTeste api;

    @BeforeEach
    void preparar() {
        api = new ApiTeste(contexto);
    }

    private String registar(String email) throws Exception {
        String r = api.pedido(null, post("/api/portal/registar"), """
                        {"nome":"Cliente Teste","email":"%s","telefone":"840000000","password":"segredo1"}
                        """.formatted(email))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.utilizador.papel").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.token");
    }

    private long criarPeca() throws Exception {
        return api.id(api.corpo(api.admin(), post("/api/pecas"),
                "{\"nome\":\"" + ApiTeste.unico("Banco") + "\",\"dimensoes\":\"40x40\",\"precoUnitario\":1000}"));
    }

    @Test
    void clienteRegistaSeEncomendaEAcompanhaOEstado() throws Exception {
        String email = ApiTeste.unico("ana") + "@mail.com";
        String cliente = registar(email);
        long peca = criarPeca();

        // o mesmo email não pode ser registado duas vezes
        api.pedido(null, post("/api/portal/registar"),
                        "{\"nome\":\"X\",\"email\":\"" + email + "\",\"password\":\"segredo1\"}")
                .andExpect(status().isConflict());

        // entra com o email
        api.login(email, "segredo1");

        // catálogo e orçamento: 1000 x 3 = 3000 (+20%) = 3600
        api.pedido(cliente, get("/api/portal/pecas"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].precoUnitario".formatted(peca), hasItem(1000.0)));
        api.pedido(cliente, get("/api/portal/orcamento").param("pecaId", "" + peca).param("quantidade", "3"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custoEstimado").value(3600.0));

        // faz a encomenda
        long encomenda = api.id(api.pedido(cliente, post("/api/portal/encomendas"),
                        "{\"pecaId\":" + peca + ",\"quantidade\":3}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString());

        // a empresa confirma o pagamento -> Observer avisa o cliente
        api.pedido(api.admin(), post("/api/encomendas/" + encomenda + "/pagamento"), null).andExpect(status().isOk());

        api.pedido(cliente, get("/api/portal/encomendas"), null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].estado").value("EM_PRODUCAO"));
        api.pedido(cliente, get("/api/portal/notificacoes"), null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0]", endsWith("EM_PRODUCAO")));

        // já em produção: o cliente não pode cancelar
        api.pedido(cliente, post("/api/portal/encomendas/" + encomenda + "/cancelar"), null)
                .andExpect(status().isConflict());
    }

    @Test
    void clienteSoVeECancelaAsSuasEncomendas() throws Exception {
        String ana = registar(ApiTeste.unico("ana") + "@mail.com");
        String beto = registar(ApiTeste.unico("beto") + "@mail.com");
        long peca = criarPeca();
        long daAna = api.id(api.corpo(ana, post("/api/portal/encomendas"), "{\"pecaId\":" + peca + ",\"quantidade\":1}"));

        api.pedido(beto, get("/api/portal/encomendas"), null).andExpect(jsonPath("$", hasSize(0)));
        api.pedido(beto, post("/api/portal/encomendas/" + daAna + "/cancelar"), null).andExpect(status().isNotFound());

        api.pedido(ana, post("/api/portal/encomendas/" + daAna + "/cancelar"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));
        api.pedido(ana, get("/api/portal/notificacoes"), null)
                .andExpect(jsonPath("$[0]", endsWith("CANCELADA")));
    }

    @Test
    void registoValidaOsDados() throws Exception {
        api.pedido(null, post("/api/portal/registar"), "{\"nome\":\"\",\"email\":\"nao-e-email\",\"password\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.password").exists());
    }

    @Test
    void portalExigeSessao() throws Exception {
        api.pedido(null, get("/api/portal/encomendas"), null).andExpect(status().isUnauthorized());
    }

    @Test
    void analiseHistoricaAceitaCriterios() throws Exception {
        String admin = api.admin();
        api.pedido(admin, get("/api/relatorios/analise").param("inicio", "2020-01-01").param("fim", "2040-12-31")
                        .param("artesao", "Ninguém"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRelatorios").value(0));
        api.pedido(admin, get("/api/relatorios/analise").param("inicio", "2030-01-02").param("fim", "2030-01-01"), null)
                .andExpect(status().isBadRequest());
    }
}
