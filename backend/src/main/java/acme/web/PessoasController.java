package acme.web;

import acme.servico.PessoasService;
import acme.web.dto.Pedidos.ClientePedido;
import acme.web.dto.Pedidos.FuncionarioPedido;
import acme.web.dto.Respostas.ClienteDTO;
import acme.web.dto.Respostas.FuncionarioDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PessoasController {

    private final PessoasService servico;

    public PessoasController(PessoasService servico) {
        this.servico = servico;
    }

    @GetMapping("/clientes")
    public List<ClienteDTO> listarClientes() {
        return servico.listarClientes();
    }

    @GetMapping("/clientes/{id}")
    public ClienteDTO obterCliente(@PathVariable Long id) {
        return servico.obterCliente(id);
    }

    /** Notificações recebidas pelo cliente (padrão Observer). */
    @GetMapping("/clientes/{id}/notificacoes")
    public List<String> notificacoes(@PathVariable Long id) {
        return servico.notificacoes(id);
    }

    @PostMapping("/clientes")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteDTO criarCliente(@Valid @RequestBody ClientePedido pedido) {
        return servico.criarCliente(pedido);
    }

    @PutMapping("/clientes/{id}")
    public ClienteDTO actualizarCliente(@PathVariable Long id, @Valid @RequestBody ClientePedido pedido) {
        return servico.actualizarCliente(id, pedido);
    }

    @DeleteMapping("/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarCliente(@PathVariable Long id) {
        servico.apagarCliente(id);
    }

    @GetMapping("/funcionarios")
    public List<FuncionarioDTO> listarFuncionarios() {
        return servico.listarFuncionarios();
    }

    @GetMapping("/funcionarios/artesaos")
    public List<FuncionarioDTO> listarArtesaos() {
        return servico.listarArtesaos();
    }

    @GetMapping("/funcionarios/{id}")
    public FuncionarioDTO obterFuncionario(@PathVariable Long id) {
        return servico.obterFuncionario(id);
    }

    @PostMapping("/funcionarios")
    @ResponseStatus(HttpStatus.CREATED)
    public FuncionarioDTO criarFuncionario(@Valid @RequestBody FuncionarioPedido pedido) {
        return servico.criarFuncionario(pedido);
    }

    @PutMapping("/funcionarios/{id}")
    public FuncionarioDTO actualizarFuncionario(@PathVariable Long id, @Valid @RequestBody FuncionarioPedido pedido) {
        return servico.actualizarFuncionario(id, pedido);
    }

    @DeleteMapping("/funcionarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagarFuncionario(@PathVariable Long id) {
        servico.apagarFuncionario(id);
    }
}
