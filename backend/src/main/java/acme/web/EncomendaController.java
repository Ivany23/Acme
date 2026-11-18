package acme.web;

import acme.modelo.EstadoEncomenda;
import acme.servico.EncomendaService;
import acme.web.dto.Pedidos.EncomendaPedido;
import acme.web.dto.Pedidos.EstadoPedido;
import acme.web.dto.Respostas.EncomendaDTO;
import acme.web.dto.Respostas.OrcamentoDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import acme.modelo.Papel;
import acme.seguranca.Acesso;
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
    @Acesso(funcionarios = true)
    public List<EncomendaDTO> listar(@RequestParam(required = false) EstadoEncomenda estado,
                                     @RequestParam(required = false) Long clienteId) {
        return servico.listar(estado, clienteId);
    }

    @GetMapping("/{id}")
    @Acesso(funcionarios = true)
    public EncomendaDTO obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    /** RF008: simula o orçamento sem criar a encomenda. */
    @GetMapping("/orcamento")
    @Acesso({Papel.DIRECTOR_GERAL, Papel.CONTADOR, Papel.GERENTE_VENDAS})
    public OrcamentoDTO orcamento(@RequestParam Long pecaId, @RequestParam int quantidade,
                                  @RequestParam(defaultValue = "0") double custosIndirectos) {
        return servico.orcamento(pecaId, quantidade, custosIndirectos);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Acesso({Papel.DIRECTOR_GERAL, Papel.GERENTE_VENDAS})
    public EncomendaDTO criar(@Valid @RequestBody EncomendaPedido pedido) {
        return servico.criar(pedido);
    }

    @PostMapping("/{id}/pagamento")
    @Acesso({Papel.DIRECTOR_GERAL, Papel.CONTADOR, Papel.GERENTE_VENDAS})
    public EncomendaDTO pagar(@PathVariable Long id) {
        return servico.confirmarPagamento(id);
    }

    @PatchMapping("/{id}/estado")
    @Acesso({Papel.DIRECTOR_GERAL, Papel.SUPERVISOR, Papel.GERENTE_VENDAS})
    public EncomendaDTO mudarEstado(@PathVariable Long id, @Valid @RequestBody EstadoPedido pedido) {
        return servico.actualizarEstado(id, pedido.estado());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Acesso({Papel.DIRECTOR_GERAL, Papel.GERENTE_VENDAS})
    public void apagar(@PathVariable Long id) {
        servico.apagar(id);
    }
}
