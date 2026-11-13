package acme.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integração: login + protecção da API (controlador -> serviço -> base de dados H2). */
@SpringBootTest
class SegurancaIntegracaoTest {

    @Autowired
    private WebApplicationContext contexto;

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).build();
    }

    @Test
    void semTokenAApiRespondeNaoAutorizado() throws Exception {
        mvc.perform(get("/api/materiais"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    void tokenFalsoERejeitado() throws Exception {
        mvc.perform(get("/api/materiais").header("Authorization", "Bearer abc.def"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordErradaERejeitada() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"admin\",\"password\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Utilizador ou password incorrectos"));
    }

    @Test
    void loginValidoDaAcessoAApi() throws Exception {
        String sessao = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"ADMIN\",\"password\":\"acme2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utilizador.nome").value("Administrador"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(sessao, "$.token");

        mvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("admin"));
        mvc.perform(get("/api/materiais").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void estadoDaApiEPublico() throws Exception {
        mvc.perform(get("/api/estado")).andExpect(status().isOk());
    }
}
