package acme.web;

import acme.servico.GestorAlocacao;
import acme.web.dto.Pedidos.AlocacaoAutomaticaPedido;
import acme.web.dto.Pedidos.AlocacaoManualPedido;
import acme.web.dto.Pedidos.TurnoPedido;
import acme.web.dto.Respostas.AlocacaoDTO;
import acme.web.dto.Respostas.TurnoDTO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TurnoController {

    private final GestorAlocacao servico;

    public TurnoController(GestorAlocacao servico) {
        this.servico = servico;
    }

    @GetMapping("/turnos")
    public List<TurnoDTO> listar() {
        return servico.listarTurnos();
    }

    @GetMapping("/turnos/{id}")
    public TurnoDTO obter(@PathVariable Long id) {
        return servico.obterTurno(id);
    }

    @PostMapping("/turnos")
    @ResponseStatus(HttpStatus.CREATED)
    public TurnoDTO criar(@Valid @RequestBody TurnoPedido pedido) {
        return servico.criarTurno(pedido);
    }

    @PutMapping("/turnos/{id}")
    public TurnoDTO actualizar(@PathVariable Long id, @Valid @RequestBody TurnoPedido pedido) {
        return servico.actualizarTurno(id, pedido);
    }

    @DeleteMapping("/turnos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id) {
        servico.apagarTurno(id);
    }

    /** Alocações de um dia (por omissão, hoje). */
    @GetMapping("/alocacoes")
    public List<AlocacaoDTO> alocacoes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return servico.alocacoesDoDia(data == null ? LocalDate.now() : data);
    }

    /** RF005: alocação automática de todos os artesãos para o dia indicado. */
    @PostMapping("/alocacoes/automatica")
    @ResponseStatus(HttpStatus.CREATED)
    public List<AlocacaoDTO> alocarAutomaticamente(@Valid @RequestBody AlocacaoAutomaticaPedido pedido) {
        return servico.alocarArtesaos(pedido.data());
    }

    @PostMapping("/alocacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public AlocacaoDTO alocarManualmente(@Valid @RequestBody AlocacaoManualPedido pedido) {
        return servico.alocarManualmente(pedido.artesaoId(), pedido.turnoId(), pedido.data());
    }

    @DeleteMapping("/alocacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarAlocacao(@PathVariable Long id) {
        servico.apagarAlocacao(id);
    }
}
