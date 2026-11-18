package acme.web;

import acme.modelo.Papel;
import acme.seguranca.Acesso;
import acme.servico.CriteriosAnalise;
import acme.servico.RelatorioService;
import acme.web.dto.Respostas.AnaliseDTO;
import acme.web.dto.Respostas.DashboardDTO;
import acme.web.dto.Respostas.RelatorioDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RelatorioController {

    private final RelatorioService servico;

    public RelatorioController(RelatorioService servico) {
        this.servico = servico;
    }

    /** Relatórios de produção, filtrados pelos critérios de análise (todos opcionais). */
    @GetMapping("/relatorios")
    @Acesso({Papel.DIRECTOR_GERAL, Papel.CONTADOR, Papel.SUPERVISOR, Papel.GERENTE_VENDAS})
    public List<RelatorioDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long pecaId,
            @RequestParam(required = false) String artesao) {
        return servico.listar(new CriteriosAnalise(inicio, fim, pecaId, artesao));
    }

    @GetMapping("/relatorios/encomenda/{encomendaId}")
    @Acesso({Papel.DIRECTOR_GERAL, Papel.CONTADOR, Papel.SUPERVISOR, Papel.GERENTE_VENDAS})
    public RelatorioDTO daEncomenda(@PathVariable Long encomendaId) {
        return servico.daEncomenda(encomendaId);
    }

    /** RF009 / AnalisadorDados: mineração dos dados históricos segundo os critérios definidos. */
    @GetMapping("/relatorios/analise")
    @Acesso({Papel.DIRECTOR_GERAL, Papel.CONTADOR, Papel.SUPERVISOR, Papel.GERENTE_VENDAS})
    public AnaliseDTO analise(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long pecaId,
            @RequestParam(required = false) String artesao) {
        return servico.analise(new CriteriosAnalise(inicio, fim, pecaId, artesao));
    }

    @GetMapping("/dashboard")
    @Acesso(funcionarios = true)
    public DashboardDTO dashboard() {
        return servico.dashboard();
    }

    /** Público: verificação simples para o frontend saber se a API está activa. */
    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("estado", "OK", "aplicacao", "Sistema Acme");
    }
}
