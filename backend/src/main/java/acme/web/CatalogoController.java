package acme.web;

import acme.servico.CatalogoService;
import acme.web.dto.Pedidos.MaterialPedido;
import acme.web.dto.Pedidos.PecaPedido;
import acme.web.dto.Respostas.MaterialDTO;
import acme.web.dto.Respostas.PecaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogoController {

    private final CatalogoService servico;

    public CatalogoController(CatalogoService servico) {
        this.servico = servico;
    }

    @GetMapping("/materiais")
    public List<MaterialDTO> listarMateriais() {
        return servico.listarMateriais();
    }

    @GetMapping("/materiais/{id}")
    public MaterialDTO obterMaterial(@PathVariable Long id) {
        return servico.obterMaterial(id);
    }

    @PostMapping("/materiais")
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialDTO criarMaterial(@Valid @RequestBody MaterialPedido pedido) {
        return servico.criarMaterial(pedido);
    }

    @PutMapping("/materiais/{id}")
    public MaterialDTO actualizarMaterial(@PathVariable Long id, @Valid @RequestBody MaterialPedido pedido) {
        return servico.actualizarMaterial(id, pedido);
    }

    @DeleteMapping("/materiais/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarMaterial(@PathVariable Long id) {
        servico.apagarMaterial(id);
    }

    @GetMapping("/pecas")
    public List<PecaDTO> listarPecas() {
        return servico.listarPecas();
    }

    @GetMapping("/pecas/{id}")
    public PecaDTO obterPeca(@PathVariable Long id) {
        return servico.obterPeca(id);
    }

    @PostMapping("/pecas")
    @ResponseStatus(HttpStatus.CREATED)
    public PecaDTO criarPeca(@Valid @RequestBody PecaPedido pedido) {
        return servico.criarPeca(pedido);
    }

    @PutMapping("/pecas/{id}")
    public PecaDTO actualizarPeca(@PathVariable Long id, @Valid @RequestBody PecaPedido pedido) {
        return servico.actualizarPeca(id, pedido);
    }

    @DeleteMapping("/pecas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarPeca(@PathVariable Long id) {
        servico.apagarPeca(id);
    }
}
