package acme.servicos;

import acme.modelo.EstadoEncomenda;
import acme.repositorio.PecaRepository;
import acme.servico.EncomendaService;
import acme.servico.NaoEncontradoException;
import acme.servico.PessoasService;
import acme.servico.PortalClienteService;
import acme.web.dto.Pedidos.EncomendaPedido;
import acme.web.dto.Respostas.EncomendaDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Serviços: casos de uso do actor Cliente, com a camada de encomendas simulada. */
@ExtendWith(MockitoExtension.class)
class PortalClienteServiceTest {

    @Mock
    private PecaRepository pecas;
    @Mock
    private EncomendaService encomendas;
    @Mock
    private PessoasService pessoas;

    private PortalClienteService servico;

    @BeforeEach
    void preparar() {
        servico = new PortalClienteService(pecas, encomendas, pessoas);
    }

    private static EncomendaDTO encomenda(long clienteId, EstadoEncomenda estado) {
        return new EncomendaDTO(9L, clienteId, "Ana", 1L, "Cadeira", 2, estado, false, 0, 2400,
                LocalDateTime.now());
    }

    @Test
    void encomendaFicaSempreEmNomeDoProprioCliente() {
        when(encomendas.criar(any())).thenReturn(encomenda(3L, EstadoEncomenda.PENDENTE));

        servico.encomendar(3L, 1L, 2);

        verify(encomendas).criar(new EncomendaPedido(3L, 1L, 2, 0.0));
    }

    @Test
    void clienteCancelaASuaEncomendaPendente() {
        when(encomendas.obter(9L)).thenReturn(encomenda(3L, EstadoEncomenda.PENDENTE));
        when(encomendas.actualizarEstado(9L, EstadoEncomenda.CANCELADA)).thenReturn(encomenda(3L, EstadoEncomenda.CANCELADA));

        assertEquals(EstadoEncomenda.CANCELADA, servico.cancelar(3L, 9L).estado());
    }

    @Test
    void clienteNaoVeNemCancelaEncomendasDeOutros() {
        when(encomendas.obter(9L)).thenReturn(encomenda(4L, EstadoEncomenda.PENDENTE));

        assertThrows(NaoEncontradoException.class, () -> servico.cancelar(3L, 9L));
        verify(encomendas, never()).actualizarEstado(any(), any());
    }

    @Test
    void naoCancelaEncomendaJaEmProducao() {
        when(encomendas.obter(9L)).thenReturn(encomenda(3L, EstadoEncomenda.EM_PRODUCAO));

        assertThrows(IllegalStateException.class, () -> servico.cancelar(3L, 9L));
    }

    @Test
    void listaApenasAsEncomendasDoCliente() {
        servico.minhasEncomendas(3L);
        verify(encomendas).listar(null, 3L);
    }
}
