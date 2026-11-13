package acme.servicos;

import acme.Dados;
import acme.modelo.Cliente;
import acme.modelo.Encomenda;
import acme.modelo.EstadoEncomenda;
import acme.modelo.Peca;
import acme.observer.GeradorRelatorio;
import acme.repositorio.EncomendaRepository;
import acme.repositorio.RegistoProducaoRepository;
import acme.servico.*;
import acme.web.dto.Pedidos.EncomendaPedido;
import acme.web.dto.Respostas.EncomendaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Serviços: regras do EncomendaService com as dependências simuladas (Mockito). */
@ExtendWith(MockitoExtension.class)
class EncomendaServiceTest {

    @Mock
    private EncomendaRepository encomendas;
    @Mock
    private RegistoProducaoRepository registos;
    @Mock
    private PessoasService pessoas;
    @Mock
    private CatalogoService catalogo;
    @Mock
    private GeradorRelatorio gerador;

    private EncomendaService servico;
    private Cliente ana;
    private Peca cadeira;

    @BeforeEach
    void preparar() {
        servico = new EncomendaService(encomendas, registos, pessoas, catalogo, new CalculadoraCustos(20), gerador);
        ana = Dados.cliente();
        cadeira = Dados.cadeira(Dados.madeira());
    }

    @Test
    void criarCalculaOrcamentoEGuarda() {
        when(pessoas.cliente(1L)).thenReturn(ana);
        when(catalogo.peca(2L)).thenReturn(cadeira);
        when(encomendas.save(any(Encomenda.class))).thenAnswer(inv -> inv.getArgument(0));

        EncomendaDTO dto = servico.criar(new EncomendaPedido(1L, 2L, 2, 100.0));

        assertEquals(3120, dto.custoEstimado(), 0.001);
        assertEquals(EstadoEncomenda.PENDENTE, dto.estado());
        verify(encomendas).save(any(Encomenda.class));
    }

    @Test
    void pagamentoAvisaClienteEGeradorDeRelatorios() {
        Encomenda e = new Encomenda(ana, cadeira, 1);
        when(encomendas.findById(5L)).thenReturn(Optional.of(e));

        EncomendaDTO dto = servico.confirmarPagamento(5L);

        assertEquals(EstadoEncomenda.EM_PRODUCAO, dto.estado());
        assertTrue(dto.paga());
        assertEquals(1, ana.getNotificacoes().size());
        verify(gerador).estadoAlterado(e);
    }

    @Test
    void naoEntraEmProducaoSemPagamento() {
        when(encomendas.findById(5L)).thenReturn(Optional.of(new Encomenda(ana, cadeira, 1)));

        assertThrows(IllegalStateException.class, () -> servico.actualizarEstado(5L, EstadoEncomenda.EM_PRODUCAO));
        verifyNoInteractions(gerador);
    }

    @Test
    void encomendaInexistenteDaNaoEncontrado() {
        when(encomendas.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> servico.obter(99L));
    }

    @Test
    void soApagaEncomendasPendentes() {
        Encomenda e = new Encomenda(ana, cadeira, 1);
        e.confirmarPagamento();
        when(encomendas.findById(5L)).thenReturn(Optional.of(e));

        assertThrows(IllegalStateException.class, () -> servico.apagar(5L));
        verify(encomendas, never()).delete(any());
    }

    @Test
    void apagaEncomendaPendenteSemRegistos() {
        Encomenda e = new Encomenda(ana, cadeira, 1);
        when(encomendas.findById(5L)).thenReturn(Optional.of(e));
        when(registos.findByEncomendaIdOrderByDataAscHoraInicioAsc(5L)).thenReturn(List.of());

        servico.apagar(5L);

        verify(encomendas).delete(e);
    }
}
