package acme.web;

import acme.servico.RelatorioService;
import acme.web.dto.Respostas.AnaliseDTO;
import acme.web.dto.Respostas.DashboardDTO;
import acme.web.dto.Respostas.RelatorioDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RelatorioController {

    private final RelatorioService servico;

    public RelatorioController(RelatorioService servico) {
        this.servico = servico;
    }

    @GetMapping("/relatorios")
    public List<RelatorioDTO> listar() {
        return servico.listar();
    }

    @GetMapping("/relatorios/encomenda/{encomendaId}")
    public RelatorioDTO daEncomenda(@PathVariable Long encomendaId) {
        return servico.daEncomenda(encomendaId);
    }

    /** RF009: análise dos dados históricos. */
    @GetMapping("/relatorios/analise")
    public AnaliseDTO analise() {
        return servico.analise();
    }

    @GetMapping("/dashboard")
    public DashboardDTO dashboard() {
        return servico.dashboard();
    }

    /** Verificação simples para o frontend saber se a API está activa. */
    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of("estado", "OK", "aplicacao", "Sistema Acme");
    }
}
