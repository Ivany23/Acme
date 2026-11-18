package acme.integracao;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ajudante dos testes de integração: pedidos com o token de um utilizador. */
final class ApiTeste {

    private final MockMvc mvc;

    ApiTeste(WebApplicationContext contexto) {
        this.mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    static String unico(String prefixo) {
        return prefixo + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    ResultActions pedido(String token, MockHttpServletRequestBuilder p, String json) throws Exception {
        if (token != null) {
            p.header("Authorization", "Bearer " + token);
        }
        if (json != null) {
            p.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(p);
    }

    String corpo(String token, MockHttpServletRequestBuilder p, String json) throws Exception {
        return pedido(token, p, json).andReturn().getResponse().getContentAsString();
    }

    String login(String login, String password) throws Exception {
        String r = pedido(null, post("/api/auth/login"),
                "{\"login\":\"" + login + "\",\"password\":\"" + password + "\"}")
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(r, "$.token");
    }

    String admin() throws Exception {
        return login("admin", "acme2026");
    }

    long id(String json) {
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Cria (como admin) uma conta com o papel indicado e devolve o token dessa conta. */
    String contaCom(String papel, Long funcionarioId, Long clienteId) throws Exception {
        String login = unico(papel.toLowerCase());
        pedido(admin(), post("/api/utilizadores"), """
                {"login":"%s","nome":"Teste %s","papel":"%s","funcionarioId":%s,"clienteId":%s,"password":"segredo1"}
                """.formatted(login, papel, papel, funcionarioId, clienteId))
                .andExpect(status().isCreated());
        return login(login, "segredo1");
    }

    long criarArtesao(String nome) throws Exception {
        return id(corpo(admin(), post("/api/funcionarios"), "{\"tipo\":\"ARTESAO\",\"nome\":\"" + nome + "\"}"));
    }

    long criarTurno() throws Exception {
        return id(corpo(admin(), post("/api/turnos"),
                "{\"denominacao\":\"" + unico("T") + "\",\"horaEntrada\":\"07:00\",\"horaSaida\":\"15:00\"}"));
    }
}
