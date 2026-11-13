package acme.modelo;

import acme.Dados;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class ModeloTest {

    // RF001
    @Test
    void materialCalculaCustoDaQuantidade() {
        assertEquals(150, Dados.madeira().calcularCusto(3), 0.001);
    }

    @Test
    void materialNaoAceitaCustoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new Material("Cola", "litro", -1));
    }

    @Test
    void pecaCalculaCustoDosMateriaisPorUnidade() {
        Peca cadeira = Dados.cadeira(Dados.madeira());
        assertEquals(250, cadeira.custoMateriaisPorUnidade(), 0.001);
    }

    @Test
    void pecaSubstituiQuantidadeDoMesmoMaterial() {
        Material madeira = Dados.madeira();
        Peca cadeira = Dados.cadeira(madeira);
        cadeira.adicionarMaterial(madeira, 2);
        assertEquals(1, cadeira.getMateriais().size());
        assertEquals(100, cadeira.custoMateriaisPorUnidade(), 0.001);
    }

    @Test
    void pecaMantemEtapasPorOrdem() {
        Peca cadeira = Dados.cadeira(Dados.madeira());
        assertEquals("Corte", cadeira.getEtapas().get(0).getDescricao());
        assertEquals("Montagem", cadeira.getEtapas().get(1).getDescricao());
    }

    @Test
    void pecaNaoAceitaPrecoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> new Peca("Mesa", "1x1 m", -5));
    }

    // RF002
    @Test
    void encomendaComecaPendente() {
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 2);
        assertEquals(EstadoEncomenda.PENDENTE, e.getEstado());
        assertFalse(e.isPaga());
    }

    @Test
    void encomendaNaoAceitaQuantidadeZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 0));
    }

    @Test
    void encomendaNaoPodeSerPagaDuasVezes() {
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 1);
        e.confirmarPagamento();
        assertThrows(IllegalStateException.class, e::confirmarPagamento);
    }

    @Test
    void encomendaRespeitaTransicoesDeEstado() {
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 1);
        assertThrows(IllegalStateException.class, () -> e.actualizarEstado(EstadoEncomenda.CONCLUIDA));
        e.actualizarEstado(EstadoEncomenda.EM_PRODUCAO);
        e.actualizarEstado(EstadoEncomenda.CONCLUIDA);
        assertThrows(IllegalStateException.class, () -> e.actualizarEstado(EstadoEncomenda.CANCELADA));
    }

    // RF003
    @Test
    void funcionarioExigeNome() {
        assertThrows(IllegalArgumentException.class, () -> new Funcionario(" ", "Contador", "84", "Maputo"));
    }

    @Test
    void artesaoEUmFuncionarioComCargoArtesao() {
        Funcionario f = Dados.artesao("João");
        assertEquals("Artesão", f.getCargo());
    }

    // RF004
    @Test
    void turnoCalculaDuracao() {
        assertEquals(480, Dados.matutino().calcularDuracaoMinutos());
    }

    @Test
    void turnoNaoAceitaSaidaAntesDaEntrada() {
        assertThrows(IllegalArgumentException.class,
                () -> new Turno("Errado", LocalTime.of(15, 0), LocalTime.of(7, 0)));
    }

    // RF006
    @Test
    void registoProducaoCalculaTempoECustoDosMateriais() {
        Material madeira = Dados.madeira();
        Peca cadeira = Dados.cadeira(madeira);
        Encomenda e = new Encomenda(Dados.cliente(), cadeira, 1);
        RegistoProducao r = new RegistoProducao(Dados.artesao("João"), e, cadeira.getEtapas().get(0),
                Dados.matutino(), LocalDate.now(), LocalTime.of(8, 0), LocalTime.of(9, 45), 30, false);
        r.adicionarMaterialUsado(madeira, 4);
        assertEquals(105, r.calcularTempoGastoMinutos());
        assertEquals(200, r.custoMateriais(), 0.001);
    }

    @Test
    void registoProducaoNaoAceitaFimAntesDoInicio() {
        Peca cadeira = Dados.cadeira(Dados.madeira());
        Encomenda e = new Encomenda(Dados.cliente(), cadeira, 1);
        assertThrows(IllegalArgumentException.class, () -> new RegistoProducao(Dados.artesao("João"), e,
                cadeira.getEtapas().get(0), Dados.matutino(), LocalDate.now(), LocalTime.of(10, 0),
                LocalTime.of(9, 0), 0, false));
    }

    // RF007
    @Test
    void registoPontoCalculaHorasExtraComMinutos() {
        RegistoPonto p = new RegistoPonto(Dados.artesao("João"), Dados.matutino(), LocalDate.now(),
                LocalTime.of(7, 0), LocalTime.of(16, 30));
        assertEquals(570, p.calcularMinutosTrabalhados());
        assertEquals(90, p.calcularMinutosExtra());
    }

    @Test
    void registoPontoSemHorasExtra() {
        RegistoPonto p = new RegistoPonto(Dados.artesao("João"), Dados.matutino(), LocalDate.now(),
                LocalTime.of(7, 0), LocalTime.of(14, 0));
        assertEquals(0, p.calcularMinutosExtra());
    }
}
