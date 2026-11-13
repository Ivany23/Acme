package acme.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Testa a API REST de ponta a ponta, com base de dados H2 em memória. */
@SpringBootTest
class ApiIntegracaoTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    private String enviar(MockHttpServletRequestBuilder pedido, String json, ResultMatcher esperado) throws Exception {
        if (json != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(pedido).andExpect(esperado).andReturn().getResponse().getContentAsString();
    }

    private long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void fluxoCompletoDaEncomendaAteAoRelatorio() throws Exception {
        long madeira = id(enviar(post("/api/materiais"),
                "{\"nome\":\"Madeira\",\"unidadeMedida\":\"kg\",\"custoUnitario\":50}", status().isCreated()));

        String peca = enviar(post("/api/pecas"), """
                {"nome":"Cadeira","dimensoes":"45x45x90 cm","precoUnitario":1000,
                 "materiais":[{"materialId":%d,"quantidade":5}],
                 "etapas":[{"descricao":"Montagem","ordem":2},{"descricao":"Corte","ordem":1}]}
                """.formatted(madeira), status().isCreated());
        long pecaId = id(peca);
        long corte = ((Number) JsonPath.read(peca, "$.etapas[0].id")).longValue();
        mvc.perform(get("/api/pecas/" + pecaId))
                .andExpect(jsonPath("$.etapas[0].descricao").value("Corte"))
                .andExpect(jsonPath("$.custoMateriaisPorUnidade").value(250.0));

        long cliente = id(enviar(post("/api/clientes"),
                "{\"nome\":\"Ana\",\"email\":\"ana@mail.com\",\"telefone\":\"845556677\"}", status().isCreated()));
        long artesao = id(enviar(post("/api/funcionarios"),
                "{\"tipo\":\"ARTESAO\",\"nome\":\"João\",\"especialidade\":\"Carpintaria\"}", status().isCreated()));
        long turno = id(enviar(post("/api/turnos"),
                "{\"denominacao\":\"Matutino\",\"horaEntrada\":\"07:00\",\"horaSaida\":\"15:00\"}", status().isCreated()));

        // RF005: alocação automática
        enviar(post("/api/alocacoes/automatica"), "{\"data\":\"2030-01-10\"}", status().isCreated());
        mvc.perform(get("/api/alocacoes").param("data", "2030-01-10"))
                .andExpect(jsonPath("$[?(@.artesaoId == %d)].turnoId".formatted(artesao), hasItem((int) turno)));

        // RF008: orçamento -> 1000x2 + 250x2 + 100 = 2600 (+20%) = 3120
        mvc.perform(get("/api/encomendas/orcamento").param("pecaId", "" + pecaId).param("quantidade", "2")
                        .param("custosIndirectos", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.custoEstimado").value(3120.0));

        long encomenda = id(enviar(post("/api/encomendas"), """
                {"clienteId":%d,"pecaId":%d,"quantidade":2,"custosIndirectos":100}
                """.formatted(cliente, pecaId), status().isCreated()));
        mvc.perform(get("/api/encomendas/" + encomenda))
                .andExpect(jsonPath("$.estado").value("PENDENTE"))
                .andExpect(jsonPath("$.custoEstimado").value(3120.0));

        // não se regista produção antes do pagamento
        String producao = """
                {"artesaoId":%d,"encomendaId":%d,"etapaId":%d,"turnoId":%d,"data":"2030-01-10",
                 "horaInicio":"08:00","horaFim":"10:00","materiaisUsados":[{"materialId":%d,"quantidade":5}],
                 "custosIndirectos":50,"retrabalho":false}
                """.formatted(artesao, encomenda, corte, turno, madeira);
        enviar(post("/api/producao"), producao, status().isConflict());

        // pagamento -> EM_PRODUCAO e cliente avisado (Observer)
        enviar(post("/api/encomendas/" + encomenda + "/pagamento"), null, status().isOk());
        enviar(post("/api/encomendas/" + encomenda + "/pagamento"), null, status().isConflict());

        // RF006 e RF007
        mvc.perform(post("/api/producao").contentType(MediaType.APPLICATION_JSON).content(producao))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tempoGastoMinutos").value(120))
                .andExpect(jsonPath("$.custoMateriais").value(250.0));
        mvc.perform(post("/api/ponto").contentType(MediaType.APPLICATION_JSON).content("""
                        {"artesaoId":%d,"data":"2030-01-10","horaEntrada":"07:00","horaSaida":"16:00"}
                        """.formatted(artesao)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.turnoId").value((int) turno))
                .andExpect(jsonPath("$.minutosExtra").value(60));

        // conclusão -> relatório gerado automaticamente (RF009)
        enviar(get("/api/relatorios/encomenda/" + encomenda), null, status().isNotFound());
        enviar(patch("/api/encomendas/" + encomenda + "/estado"), "{\"estado\":\"CONCLUIDA\"}", status().isOk());
        mvc.perform(get("/api/relatorios/encomenda/" + encomenda))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutosPorEtapa.Corte").value(120))
                .andExpect(jsonPath("$.minutosPorArtesao.['João']").value(120))
                .andExpect(jsonPath("$.materiaisConsumidos.Madeira").value(5.0))
                .andExpect(jsonPath("$.minutosExtraPorArtesao.['João']").value(60))
                .andExpect(jsonPath("$.custoReal").value(300.0))
                .andExpect(jsonPath("$.custoEstimado").value(3120.0));

        mvc.perform(get("/api/clientes/" + cliente + "/notificacoes"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", endsWith("EM_PRODUCAO")))
                .andExpect(jsonPath("$[1]", endsWith("CONCLUIDA")));

        // estado final não pode mudar; dados em uso não podem ser apagados
        enviar(patch("/api/encomendas/" + encomenda + "/estado"), "{\"estado\":\"CANCELADA\"}", status().isConflict());
        enviar(delete("/api/clientes/" + cliente), null, status().isConflict());
        enviar(delete("/api/materiais/" + madeira), null, status().isConflict());
        enviar(delete("/api/turnos/" + turno), null, status().isConflict());

        mvc.perform(get("/api/relatorios/analise"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRelatorios", greaterThanOrEqualTo(1)));
        mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encomendasPorEstado.CONCLUIDA", greaterThanOrEqualTo(1)));
    }

    @Test
    void alocacaoAutomaticaNuncaDaDoisTurnosNoMesmoDia() throws Exception {
        enviar(post("/api/turnos"), "{\"denominacao\":\"A\",\"horaEntrada\":\"06:00\",\"horaSaida\":\"14:00\"}",
                status().isCreated());
        enviar(post("/api/funcionarios"), "{\"tipo\":\"ARTESAO\",\"nome\":\"Maria\"}", status().isCreated());

        String primeira = enviar(post("/api/alocacoes/automatica"), "{\"data\":\"2031-05-05\"}", status().isCreated());
        List<?> criadas = JsonPath.read(primeira, "$");
        org.junit.jupiter.api.Assertions.assertFalse(criadas.isEmpty());

        mvc.perform(post("/api/alocacoes/automatica").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"data\":\"2031-05-05\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void actualizaPecaMantendoEtapasExistentes() throws Exception {
        String peca = enviar(post("/api/pecas"), """
                {"nome":"Mesa","precoUnitario":3000,"etapas":[{"descricao":"Corte","ordem":1}]}
                """, status().isCreated());
        long pecaId = id(peca);
        long etapa = ((Number) JsonPath.read(peca, "$.etapas[0].id")).longValue();

        mvc.perform(put("/api/pecas/" + pecaId).contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Mesa grande","precoUnitario":3500,
                         "etapas":[{"id":%d,"descricao":"Corte rigoroso","ordem":1},{"descricao":"Pintura","ordem":2}]}
                        """.formatted(etapa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Mesa grande"))
                .andExpect(jsonPath("$.etapas", hasSize(2)))
                .andExpect(jsonPath("$.etapas[0].id").value((int) etapa))
                .andExpect(jsonPath("$.etapas[0].descricao").value("Corte rigoroso"));

        enviar(delete("/api/pecas/" + pecaId), null, status().isNoContent());
        enviar(get("/api/pecas/" + pecaId), null, status().isNotFound());
    }

    @Test
    void errosDevolvemRespostasClaras() throws Exception {
        mvc.perform(post("/api/materiais").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"\",\"custoUnitario\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.unidadeMedida").exists())
                .andExpect(jsonPath("$.campos.custoUnitario").exists());

        mvc.perform(get("/api/clientes/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem", containsString("999999")));

        mvc.perform(post("/api/turnos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"denominacao\":\"X\",\"horaEntrada\":\"15:00\",\"horaSaida\":\"07:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem", containsString("saída")));

        mvc.perform(post("/api/clientes").contentType(MediaType.APPLICATION_JSON).content("{isto nao e json"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/encomendas").param("estado", "INEXISTENTE"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/estado")).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("OK"));
    }

    @Test
    void corsPermiteOFrontend() throws Exception {
        mvc.perform(options("/api/materiais")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
