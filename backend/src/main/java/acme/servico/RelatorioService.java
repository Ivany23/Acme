package acme.servico;

import acme.modelo.EstadoEncomenda;
import acme.repositorio.*;
import acme.web.dto.Respostas.AnaliseDTO;
import acme.web.dto.Respostas.DashboardDTO;
import acme.web.dto.Respostas.RelatorioDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Relatórios de produção e análise dos dados históricos (RF009). */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final RelatorioProducaoRepository relatorios;
    private final ClienteRepository clientes;
    private final PecaRepository pecas;
    private final MaterialRepository materiais;
    private final FuncionarioRepository funcionarios;
    private final ArtesaoRepository artesaos;
    private final TurnoRepository turnos;
    private final EncomendaRepository encomendas;

    public RelatorioService(RelatorioProducaoRepository relatorios, ClienteRepository clientes, PecaRepository pecas,
                            MaterialRepository materiais, FuncionarioRepository funcionarios,
                            ArtesaoRepository artesaos, TurnoRepository turnos, EncomendaRepository encomendas) {
        this.relatorios = relatorios;
        this.clientes = clientes;
        this.pecas = pecas;
        this.materiais = materiais;
        this.funcionarios = funcionarios;
        this.artesaos = artesaos;
        this.turnos = turnos;
        this.encomendas = encomendas;
    }

    public List<RelatorioDTO> listar(CriteriosAnalise criterios) {
        return analisador(criterios).getRelatorios().stream().map(RelatorioDTO::de).toList();
    }

    public RelatorioDTO daEncomenda(Long encomendaId) {
        return relatorios.findByEncomendaId(encomendaId).map(RelatorioDTO::de)
                .orElseThrow(() -> new NaoEncontradoException("Relatório da encomenda", encomendaId));
    }

    /** RF009: minera os dados históricos segundo os critérios de análise definidos. */
    public AnaliseDTO analise(CriteriosAnalise criterios) {
        return AnaliseDTO.de(analisador(criterios));
    }

    private AnalisadorDados analisador(CriteriosAnalise criterios) {
        String artesao = null;
        if (criterios.artesao() != null && !criterios.artesao().isBlank()) {
            artesao = criterios.artesao().trim();
        }
        CriteriosAnalise c = new CriteriosAnalise(criterios.inicio(), criterios.fim(), criterios.pecaId(), artesao);
        return new AnalisadorDados(relatorios.findAllByOrderByDataGeracaoDesc()).filtrar(c);
    }

    public DashboardDTO dashboard() {
        Map<EstadoEncomenda, Long> porEstado = new EnumMap<>(EstadoEncomenda.class);
        for (EstadoEncomenda e : EstadoEncomenda.values()) {
            porEstado.put(e, encomendas.countByEstado(e));
        }
        return new DashboardDTO(clientes.count(), pecas.count(), materiais.count(), funcionarios.count(),
                artesaos.count(), turnos.count(), porEstado, analise(CriteriosAnalise.TODOS));
    }
}
