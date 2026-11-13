package acme.servico;

import acme.modelo.AlocacaoTurno;
import acme.modelo.Artesao;
import acme.modelo.Turno;
import acme.repositorio.*;
import acme.web.dto.Pedidos.TurnoPedido;
import acme.web.dto.Respostas.AlocacaoDTO;
import acme.web.dto.Respostas.TurnoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Turnos (RF004) e alocação automática dos artesãos aos turnos (RF005). */
@Service
@Transactional
public class GestorAlocacao {

    private final TurnoRepository turnos;
    private final ArtesaoRepository artesaos;
    private final AlocacaoTurnoRepository alocacoes;
    private final RegistoProducaoRepository registos;
    private final RegistoPontoRepository pontos;

    public GestorAlocacao(TurnoRepository turnos, ArtesaoRepository artesaos, AlocacaoTurnoRepository alocacoes,
                          RegistoProducaoRepository registos, RegistoPontoRepository pontos) {
        this.turnos = turnos;
        this.artesaos = artesaos;
        this.alocacoes = alocacoes;
        this.registos = registos;
        this.pontos = pontos;
    }

    // ---------- Turnos ----------

    @Transactional(readOnly = true)
    public List<TurnoDTO> listarTurnos() {
        return turnos.findAllByOrderByHoraEntradaAsc().stream().map(TurnoDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public TurnoDTO obterTurno(Long id) {
        return TurnoDTO.de(turno(id));
    }

    public TurnoDTO criarTurno(TurnoPedido p) {
        return TurnoDTO.de(turnos.save(new Turno(p.denominacao(), p.horaEntrada(), p.horaSaida())));
    }

    public TurnoDTO actualizarTurno(Long id, TurnoPedido p) {
        Turno t = turno(id);
        t.actualizar(p.denominacao(), p.horaEntrada(), p.horaSaida());
        return TurnoDTO.de(t);
    }

    public void apagarTurno(Long id) {
        Turno t = turno(id);
        if (alocacoes.existsByTurnoId(id) || registos.existsByTurnoId(id) || pontos.existsByTurnoId(id)) {
            throw new IllegalStateException("O turno já está a ser usado e não pode ser apagado");
        }
        turnos.delete(t);
        turnos.flush();
    }

    public Turno turno(Long id) {
        return turnos.findById(id).orElseThrow(() -> new NaoEncontradoException("Turno", id));
    }

    // ---------- Alocações ----------

    /** Distribui os artesãos pelos turnos de forma rotativa: 1.º -> turno 1, 2.º -> turno 2, ... */
    public List<AlocacaoDTO> alocarArtesaos(LocalDate data) {
        List<Turno> listaTurnos = turnos.findAllByOrderByHoraEntradaAsc();
        if (listaTurnos.isEmpty()) {
            throw new IllegalStateException("Não existem turnos registados");
        }
        List<Artesao> listaArtesaos = artesaos.findAllByOrderByIdAsc();
        if (listaArtesaos.isEmpty()) {
            throw new IllegalStateException("Não existem artesãos registados");
        }
        List<AlocacaoTurno> doDia = new ArrayList<>();
        for (int i = 0; i < listaArtesaos.size(); i++) {
            Artesao a = listaArtesaos.get(i);
            if (alocacoes.findByArtesaoIdAndData(a.getId(), data).isPresent()) {
                continue; // nunca mais de um turno por dia
            }
            doDia.add(alocacoes.save(new AlocacaoTurno(a, listaTurnos.get(i % listaTurnos.size()), data)));
        }
        return doDia.stream().map(AlocacaoDTO::de).toList();
    }

    /** Aloca (ou troca) manualmente o turno de um artesão num dia. */
    public AlocacaoDTO alocarManualmente(Long artesaoId, Long turnoId, LocalDate data) {
        Artesao artesao = artesaos.findById(artesaoId).orElseThrow(() -> new NaoEncontradoException("Artesão", artesaoId));
        Turno turno = turno(turnoId);
        Optional<AlocacaoTurno> existente = alocacoes.findByArtesaoIdAndData(artesaoId, data);
        if (existente.isPresent()) {
            alocacoes.delete(existente.get());
            alocacoes.flush();
        }
        return AlocacaoDTO.de(alocacoes.save(new AlocacaoTurno(artesao, turno, data)));
    }

    @Transactional(readOnly = true)
    public List<AlocacaoDTO> alocacoesDoDia(LocalDate data) {
        return alocacoes.findByDataOrderByTurnoHoraEntradaAscArtesaoNomeAsc(data).stream()
                .map(AlocacaoDTO::de).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Turno> turnoDoArtesao(Long artesaoId, LocalDate data) {
        return alocacoes.findByArtesaoIdAndData(artesaoId, data).map(AlocacaoTurno::getTurno);
    }

    public void apagarAlocacao(Long id) {
        AlocacaoTurno a = alocacoes.findById(id).orElseThrow(() -> new NaoEncontradoException("Alocação", id));
        alocacoes.delete(a);
    }
}
