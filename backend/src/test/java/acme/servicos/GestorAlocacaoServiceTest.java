package acme.servicos;

import acme.Dados;
import acme.modelo.AlocacaoTurno;
import acme.modelo.Artesao;
import acme.modelo.Turno;
import acme.repositorio.*;
import acme.servico.GestorAlocacao;
import acme.web.dto.Respostas.AlocacaoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Serviços: RF005, alocação automática dos artesãos aos turnos. */
@ExtendWith(MockitoExtension.class)
class GestorAlocacaoServiceTest {

    @Mock
    private TurnoRepository turnos;
    @Mock
    private ArtesaoRepository artesaos;
    @Mock
    private AlocacaoTurnoRepository alocacoes;
    @Mock
    private RegistoProducaoRepository registos;
    @Mock
    private RegistoPontoRepository pontos;

    private GestorAlocacao servico;
    private final LocalDate dia = LocalDate.of(2026, 11, 2);
    private final Turno manha = Dados.matutino();
    private final Turno tarde = Dados.vespertino();

    @BeforeEach
    void preparar() {
        servico = new GestorAlocacao(turnos, artesaos, alocacoes, registos, pontos);
    }

    @Test
    void distribuiOsArtesaosPelosTurnosDeFormaRotativa() {
        when(turnos.findAllByOrderByHoraEntradaAsc()).thenReturn(List.of(manha, tarde));
        when(artesaos.findAllByOrderByIdAsc()).thenReturn(List.of(
                Dados.artesao("João"), Dados.artesao("Maria"), Dados.artesao("Pedro")));
        when(alocacoes.findByArtesaoIdAndData(any(), any())).thenReturn(Optional.empty());
        when(alocacoes.save(any(AlocacaoTurno.class))).thenAnswer(inv -> inv.getArgument(0));

        List<AlocacaoDTO> r = servico.alocarArtesaos(dia);

        assertEquals(3, r.size());
        assertEquals(List.of("Matutino", "Vespertino", "Matutino"),
                r.stream().map(AlocacaoDTO::turnoDenominacao).toList());
    }

    @Test
    void artesaoJaAlocadoNoDiaNaoRecebeOutroTurno() {
        Artesao joao = Dados.artesao("João");
        when(turnos.findAllByOrderByHoraEntradaAsc()).thenReturn(List.of(manha));
        when(artesaos.findAllByOrderByIdAsc()).thenReturn(List.of(joao));
        when(alocacoes.findByArtesaoIdAndData(any(), any()))
                .thenReturn(Optional.of(new AlocacaoTurno(joao, manha, dia)));

        assertTrue(servico.alocarArtesaos(dia).isEmpty());
        verify(alocacoes, never()).save(any());
    }

    @Test
    void semTurnosLancaErro() {
        when(turnos.findAllByOrderByHoraEntradaAsc()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> servico.alocarArtesaos(dia));
    }

    @Test
    void semArtesaosLancaErro() {
        when(turnos.findAllByOrderByHoraEntradaAsc()).thenReturn(List.of(manha));
        when(artesaos.findAllByOrderByIdAsc()).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> servico.alocarArtesaos(dia));
    }

    @Test
    void naoApagaTurnoEmUso() {
        when(turnos.findById(1L)).thenReturn(Optional.of(manha));
        when(alocacoes.existsByTurnoId(1L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> servico.apagarTurno(1L));
        verify(turnos, never()).delete(any());
    }
}
