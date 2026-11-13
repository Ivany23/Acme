package acme.servico;

import acme.Dados;
import acme.modelo.Encomenda;
import acme.modelo.RelatorioProducao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** RF008 (custos) e RF009 (análise dos dados históricos). */
class CalculadoraCustosTest {

    @Test
    void calculaCustoDosMateriaisDaEncomenda() {
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 2);
        assertEquals(500, new CalculadoraCustos(20).calcularCustoMateriais(e), 0.001);
    }

    // base = 1000 x 2 + materiais 250 x 2 + indirectos 100 = 2600; + 20% = 3120
    @Test
    void custoEstimadoAplicaPercentagemDaOrganizacao() {
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 2);
        assertEquals(3120, new CalculadoraCustos(20).calcularCustoEstimado(e, 100), 0.001);
    }

    @Test
    void orcamentoDetalhaAsParcelas() {
        CalculadoraCustos.Orcamento o = new CalculadoraCustos(20).orcamento(Dados.cadeira(Dados.madeira()), 2, 100);
        assertEquals(2000, o.custoPecas(), 0.001);
        assertEquals(500, o.custoMateriais(), 0.001);
        assertEquals(2600, o.custoBase(), 0.001);
        assertEquals(3120, o.custoEstimado(), 0.001);
    }

    @Test
    void orcamentoRejeitaValoresInvalidos() {
        CalculadoraCustos c = new CalculadoraCustos(20);
        assertThrows(IllegalArgumentException.class, () -> c.orcamento(Dados.cadeira(Dados.madeira()), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> c.orcamento(Dados.cadeira(Dados.madeira()), 1, -1));
    }

    @Test
    void custoRealSemRegistosEZero() {
        assertEquals(0, new CalculadoraCustos(20).calcularCustoReal(List.of()), 0.001);
    }

    @Test
    void analisadorCalculaCustoMedioEDesvio() {
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 1);
        e.definirOrcamento(0, 700);
        AnalisadorDados a = new AnalisadorDados(List.of(
                new RelatorioProducao(e, List.of(), List.of(), 400),
                new RelatorioProducao(e, List.of(), List.of(), 600)));
        assertEquals(500, a.custoMedioPorEncomenda(), 0.001);
        assertEquals(200, a.desvioMedioOrcamento(), 0.001);
        assertEquals(0, a.totalMinutosExtra());
    }

    @Test
    void analisadorSemRelatoriosDevolveZero() {
        assertEquals(0, new AnalisadorDados(List.of()).custoMedioPorEncomenda(), 0.001);
    }
}
