package acme.web;

import acme.config.SegurancaConfig;
import acme.modelo.Papel;
import acme.seguranca.Acesso;
import acme.servico.UtilizadorService;
import acme.web.dto.Pedidos.UtilizadorPedido;
import acme.web.dto.Respostas.UtilizadorDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Gestão de contas de acesso: apenas o administrador. */
@RestController
@RequestMapping("/api/utilizadores")
@Acesso(Papel.ADMINISTRADOR)
public class UtilizadorController {

    private final UtilizadorService servico;

    public UtilizadorController(UtilizadorService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<UtilizadorDTO> listar() {
        return servico.listar();
    }

    /** Lista de papéis para o formulário. */
    @GetMapping("/papeis")
    public List<Map<String, String>> papeis() {
        return Arrays.stream(Papel.values())
                .map(p -> Map.of("valor", p.name(), "texto", p.getDescricao()))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilizadorDTO criar(@Valid @RequestBody UtilizadorPedido pedido) {
        return servico.criar(pedido);
    }

    @PutMapping("/{id}")
    public UtilizadorDTO actualizar(@PathVariable Long id, @Valid @RequestBody UtilizadorPedido pedido,
                                    @RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO quem) {
        return servico.actualizar(id, pedido, quem.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable Long id,
                       @RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO quem) {
        servico.apagar(id, quem.id());
    }
}
