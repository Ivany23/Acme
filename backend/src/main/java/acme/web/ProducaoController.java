package acme.web;

import acme.servico.ProducaoService;
import acme.web.dto.Pedidos.RegistoPontoPedido;
import acme.web.dto.Pedidos.RegistoProducaoPedido;
import acme.web.dto.Respostas.RegistoPontoDTO;
import acme.web.dto.Respostas.RegistoProducaoDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ProducaoController {

    private final ProducaoService servico;

    public ProducaoController(ProducaoService servico) {
        this.servico = servico;
    }

    @GetMapping("/producao")
    public List<RegistoProducaoDTO> listarRegistos(@RequestParam(required = false) Long encomendaId) {
        return servico.listarRegistos(encomendaId);
    }

    @PostMapping("/producao")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistoProducaoDTO registar(@Valid @RequestBody RegistoProducaoPedido pedido) {
        return servico.registarProducao(pedido);
    }

    @DeleteMapping("/producao/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarRegisto(@PathVariable Long id) {
        servico.apagarRegisto(id);
    }

    @GetMapping("/ponto")
    public List<RegistoPontoDTO> listarPontos(
            @RequestParam(required = false) Long artesaoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return servico.listarPontos(artesaoId, data);
    }

    @PostMapping("/ponto")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistoPontoDTO registarPonto(@Valid @RequestBody RegistoPontoPedido pedido) {
        return servico.registarPonto(pedido);
    }

    @DeleteMapping("/ponto/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarPonto(@PathVariable Long id) {
        servico.apagarPonto(id);
    }
}
