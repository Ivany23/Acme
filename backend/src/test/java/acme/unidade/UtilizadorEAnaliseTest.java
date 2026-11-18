package acme.unidade;

import acme.Dados;
import acme.modelo.*;
import acme.servico.AnalisadorDados;
import acme.servico.CriteriosAnalise;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Unidade: coerência das contas de acesso e critérios de análise do AnalisadorDados. */
class UtilizadorEAnaliseTest {

    // ---------- Utilizador ----------

    @Test
    void contaDeArtesaoTemDeEstarLigadaAUmArtesao() {
        Utilizador u = new Utilizador("joao", "João", "hash", Papel.ADMINISTRADOR);
        Funcionario contabilista = new Funcionario("Rita", "Contabilista", null, null);

        assertThrows(IllegalArgumentException.class, () -> u.actualizar("João", Papel.ARTESAO, null, null));
        assertThrows(IllegalArgumentException.class, () -> u.actualizar("Rita", Papel.ARTESAO, contabilista, null));

        u.actualizar("João", Papel.ARTESAO, Dados.artesao("João"), null);
        assertEquals(Papel.ARTESAO, u.getPapel());
    }

    @Test
    void contaDeClienteTemDeEstarLigadaAUmCliente() {
        Utilizador u = new Utilizador("ana", "Ana", "hash", Papel.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class, () -> u.actualizar("Ana", Papel.CLIENTE, null, null));

        Utilizador c = Utilizador.deCliente(Dados.cliente(), "ana@mail.com", "hash");
        assertEquals(Papel.CLIENTE, c.getPapel());
        assertEquals("Ana", c.getCliente().getNome());
    }

    @Test
    void funcionarioNaoPodeTerContaDeCliente() {
        Utilizador u = new Utilizador("rita", "Rita", "hash", Papel.ADMINISTRADOR);
        assertThrows(IllegalArgumentException.class,
                () -> u.actualizar("Rita", Papel.CONTADOR, null, Dados.cliente()));
    }

    @Test
    void loginEGuardadoEmMinusculas() {
        assertEquals("director", new Utilizador("Director", "D", "hash", Papel.DIRECTOR_GERAL).getLogin());
    }

    // ---------- Critérios de análise ----------

    private RelatorioProducao relatorio(String artesao, double custoReal) {
        Material madeira = Dados.madeira();
        Peca cadeira = Dados.cadeira(madeira);
        Encomenda e = new Encomenda(Dados.cliente(), cadeira, 1);
        RegistoProducao r = new RegistoProducao(Dados.artesao(artesao), e, cadeira.getEtapas().get(0),
                Dados.matutino(), LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 0), 0, false);
        return new RelatorioProducao(e, List.of(r), List.of(), custoReal);
    }

    @Test
    void filtraPorArtesao() {
        AnalisadorDados a = new AnalisadorDados(List.of(relatorio("João", 400), relatorio("Maria", 600)));

        AnalisadorDados soJoao = a.filtrar(new CriteriosAnalise(null, null, null, "João"));

        assertEquals(1, soJoao.totalRelatorios());
        assertEquals(400, soJoao.custoMedioPorEncomenda(), 0.001);
        assertEquals(60L, soJoao.minutosPorArtesao().get("João"));
    }

    @Test
    void filtraPorPeriodo() {
        AnalisadorDados a = new AnalisadorDados(List.of(relatorio("João", 400)));
        LocalDate hoje = LocalDate.now();

        assertEquals(1, a.filtrar(new CriteriosAnalise(hoje, hoje, null, null)).totalRelatorios());
        assertEquals(0, a.filtrar(new CriteriosAnalise(hoje.plusDays(1), null, null, null)).totalRelatorios());
        assertEquals(0, a.filtrar(new CriteriosAnalise(null, hoje.minusDays(1), null, null)).totalRelatorios());
    }

    @Test
    void custoMedioPorPeca() {
        AnalisadorDados a = new AnalisadorDados(List.of(relatorio("João", 400), relatorio("Maria", 600)));
        assertEquals(500, a.custoMedioPorPeca().get("Cadeira"), 0.001);
    }

    @Test
    void periodoInvertidoERejeitado() {
        LocalDate hoje = LocalDate.now();
        assertThrows(IllegalArgumentException.class, () -> new CriteriosAnalise(hoje, hoje.minusDays(1), null, null));
    }
}
