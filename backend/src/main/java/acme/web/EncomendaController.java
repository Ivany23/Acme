package acme.web;

import acme.modelo.EstadoEncomenda;
import acme.servico.EncomendaService;
import acme.web.dto.Pedidos.EncomendaPedido;
import acme.web.dto.Pedidos.EstadoPedido;
import acme.web.dto.Respostas.EncomendaDTO;
import acme.web.dto.Respostas.OrcamentoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/encomendas")
public class EncomendaController {

    private final EncomendaService servico;

    public EncomendaController(EncomendaService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<EncomendaDTO> listar(@RequestParam(required = false) EstadoEncomenda estado,
                                     @RequestParam(required = false) Long clienteId) {
        return servico.listar(estado, clienteId);
    }

    @GetMapping("/{id}")
    public EncomendaDTO obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    /** RF008: simula o orçamento sem criar a encomenda. */
    @GetMapping("/orcamento")
    public OrcamentoDTO orcamento(@RequestParam Long pecaId, @RequestParam int quantidade,
                                  @RequestParam(defaultValue = "0") double custosIndirectos) {
        return servico.orcamento(pecaId, quantidade, custosIndirectos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EncomendaDTO criar(@Valid @RequestBody EncomendaPedido pedido) {
        return servico.criar(pedido);
    }

    @PostMapping("/{id}/pagamento")
    public EncomendaDTO pagar(@PathVariable Long id) {
        return servico.confirmarPagamento(id);
    }

    @PatchMapping("/{id}/estado")
    public EncomendaDTO mudarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedido pedido) {
        return servico.actualizarEstado(id, pedido.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id) {
        servico.apagar(id);
    }
}
