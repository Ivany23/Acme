package acme.web;

import acme.config.SegurancaConfig;
import acme.modelo.Papel;
import acme.seguranca.Acesso;
import acme.servico.ServicoAutenticacao;
import acme.web.dto.Pedidos.AlterarPasswordPedido;
import acme.web.dto.Pedidos.LoginPedido;
import acme.web.dto.Respostas.SessaoDTO;
import acme.web.dto.Respostas.UtilizadorDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final ServicoAutenticacao servico;

    public AutenticacaoController(ServicoAutenticacao servico) {
        this.servico = servico;
    }

    /** Público: login de funcionários e de clientes. */
    @PostMapping("/login")
    public SessaoDTO login(@Valid @RequestBody LoginPedido pedido) {
        return servico.login(pedido.login(), pedido.password());
    }

    /** Devolve o utilizador da sessão actual. */
    @GetMapping("/eu")
    @Acesso(value = Papel.CLIENTE, funcionarios = true)
    public UtilizadorDTO eu(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO utilizador) {
        return utilizador;
    }

    @PutMapping("/password")
    @Acesso(value = Papel.CLIENTE, funcionarios = true)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void alterarPassword(@RequestAttribute(SegurancaConfig.ATRIBUTO_UTILIZADOR) UtilizadorDTO utilizador,
                                @Valid @RequestBody AlterarPasswordPedido pedido) {
        servico.alterarPassword(utilizador.id(), pedido.actual(), pedido.nova());
    }
}
