package acme.servicos;

import acme.Dados;
import acme.modelo.Artesao;
import acme.modelo.Encomenda;
import acme.repositorio.RegistoPontoRepository;
import acme.repositorio.RegistoProducaoRepository;
import acme.servico.*;
import acme.web.dto.Pedidos.RegistoPontoPedido;
import acme.web.dto.Pedidos.RegistoProducaoPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Serviços: RF006 (produção) e RF007 (ponto). */
@ExtendWith(MockitoExtension.class)
class ProducaoServiceTest {

    @Mock
    private RegistoProducaoRepository registos;
    @Mock
    private RegistoPontoRepository pontos;
    @Mock
    private EncomendaService encomendas;
    @Mock
    private PessoasService pessoas;
    @Mock
    private GestorAlocacao gestor;
    @Mock
    private CatalogoService catalogo;

    private ProducaoService servico;
    private final LocalDate dia = LocalDate.of(2026, 11, 2);

    @BeforeEach
    void preparar() {
        servico = new ProducaoService(registos, pontos, encomendas, pessoas, gestor, catalogo);
    }

    private RegistoProducaoPedido pedidoProducao() {
        return new RegistoProducaoPedido(1L, 2L, 3L, 4L, dia, LocalTime.of(8, 0), LocalTime.of(10, 0),
                null, 0.0, false);
    }

    @Test
    void naoRegistaProducaoEmEncomendaPendente() {
        when(encomendas.encomenda(2L)).thenReturn(new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 1));

        assertThrows(IllegalStateException.class, () -> servico.registarProducao(pedidoProducao()));
        verify(registos, never()).save(any());
    }

    @Test
    void etapaTemDeSerDaPecaDaEncomenda() {
        Encomenda e = new Encomenda(Dados.cliente(), Dados.cadeira(Dados.madeira()), 1);
        e.confirmarPagamento();
        when(encomendas.encomenda(2L)).thenReturn(e);

        assertThrows(IllegalArgumentException.class, () -> servico.registarProducao(pedidoProducao()));
        verify(registos, never()).save(any());
    }

    @Test
    void pontoSemTurnoUsaOTurnoAlocado() {
        Artesao joao = Dados.artesao("João");
        when(pessoas.artesao(1L)).thenReturn(joao);
        when(gestor.turnoDoArtesao(any(), any())).thenReturn(Optional.of(Dados.matutino()));
        when(pontos.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = servico.registarPonto(new RegistoPontoPedido(1L, null, dia, LocalTime.of(7, 0), LocalTime.of(16, 0)));

        assertEquals("Matutino", dto.turnoDenominacao());
        assertEquals(60, dto.minutosExtra());
    }

    @Test
    void pontoSemTurnoAlocadoEErro() {
        when(pessoas.artesao(1L)).thenReturn(Dados.artesao("João"));
        when(gestor.turnoDoArtesao(any(), any())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> servico.registarPonto(
                new RegistoPontoPedido(1L, null, dia, LocalTime.of(7, 0), LocalTime.of(15, 0))));
    }
}
