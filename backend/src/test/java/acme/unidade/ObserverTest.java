package acme.unidade;

import acme.modelo.*;

import acme.Dados;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Padrão Observer: a Encomenda (sujeito) avisa os observadores quando muda de estado. */
class ObserverTest {

    @Test
    void clienteEAvisadoQuandoOEstadoMuda() {
        Cliente ana = Dados.cliente();
        Encomenda e = new Encomenda(ana, Dados.cadeira(Dados.madeira()), 1);
        e.adicionarObservador(ana);

        e.actualizarEstado(EstadoEncomenda.EM_PRODUCAO);

        assertEquals(1, ana.getNotificacoes().size());
        assertTrue(ana.getNotificacoes().get(0).endsWith("está agora EM_PRODUCAO"));
    }

    @Test
    void todosOsObservadoresSaoAvisados() {
        List<String> avisos = new ArrayList<>();
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 1);
        e.adicionarObservador(enc -> avisos.add("A"));
        e.adicionarObservador(enc -> avisos.add("B"));

        e.actualizarEstado(EstadoEncomenda.CANCELADA);

        assertEquals(List.of("A", "B"), avisos);
    }

    @Test
    void omesmoObservadorNaoERegistadoDuasVezes() {
        Cliente ana = Dados.cliente();
        Encomenda e = new Encomenda(ana, Dados.cadeira(Dados.madeira()), 1);
        e.adicionarObservador(ana);
        e.adicionarObservador(ana);

        e.actualizarEstado(EstadoEncomenda.EM_PRODUCAO);

        assertEquals(1, ana.getNotificacoes().size());
    }

    @Test
    void pagamentoColocaEncomendaEmProducaoEAvisaCliente() {
        Cliente ana = Dados.cliente();
        Encomenda e = new Encomenda(ana, Dados.cadeira(Dados.madeira()), 1);
        e.adicionarObservador(ana);

        e.confirmarPagamento();

        assertTrue(e.isPaga());
        assertEquals(EstadoEncomenda.EM_PRODUCAO, e.getEstado());
        assertEquals(1, ana.getNotificacoes().size());
    }

    // RF009
    @Test
    void relatorioAgregaTempoMateriaisEHorasExtra() {
        Material madeira = Dados.madeira();
        Peca cadeira = Dados.cadeira(madeira);
        Artesao joao = Dados.artesao("João");
        Turno manha = Dados.matutino();
        Encomenda e = new Encomenda(Dados.cliente(), cadeira, 1);

        RegistoProducao r = new RegistoProducao(joao, e, cadeira.getEtapas().get(0), manha, LocalDate.now(),
                LocalTime.of(8, 0), LocalTime.of(10, 0), 50, true);
        r.adicionarMaterialUsado(madeira, 5);
        RegistoPonto p = new RegistoPonto(joao, manha, LocalDate.now(), LocalTime.of(7, 0), LocalTime.of(16, 0));

        RelatorioProducao rel = new RelatorioProducao(e, List.of(r), List.of(p), 300);

        assertEquals(120L, rel.getMinutosPorEtapa().get("Corte"));
        assertEquals(120L, rel.getMinutosPorArtesao().get("João"));
        assertEquals(5.0, rel.getMateriaisConsumidos().get("Madeira"), 0.001);
        assertEquals(60L, rel.getMinutosExtraPorArtesao().get("João"));
        assertEquals(1, rel.getRetrabalhos());
        assertEquals(300, rel.getCustoReal(), 0.001);
    }
}
